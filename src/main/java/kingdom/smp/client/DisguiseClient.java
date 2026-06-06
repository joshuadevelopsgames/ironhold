package kingdom.smp.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import kingdom.smp.mixin.ShulkerAccessor;
import kingdom.smp.client.entity.MimicRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.util.Mth;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-side disguise rendering. When a disguised player's render state is being extracted,
 * {@link kingdom.smp.mixin.EntityRenderDispatcherDisguiseMixin} asks here for a substitute:
 * we keep one invisible "dummy" entity per disguise type, copy the player's pose/rotation onto
 * it, and return the dummy's render state in the player's place. That state then flows through
 * the normal render pipeline as a single stable entity (no re-entrant submit, no double passes).
 *
 * <p>The player's hitbox/size never changes — this is purely visual.
 */
public final class DisguiseClient {
    private DisguiseClient() {}

    private static final Map<EntityType<?>, Entity> DUMMIES = new HashMap<>();

    /** Per-player dummies ticked once per client tick so vanilla mob particle timing runs. */
    private static final Map<UUID, ParticleDummy> PARTICLE_DUMMIES = new HashMap<>();

    /** Last game tick each disguised player was moving — drives the mimic's settle-to-chest delay. */
    private static final Map<UUID, Long> LAST_MOVING_TICK = new HashMap<>();

    /** A mimic disguise stays "awake" for this long after the player stops before closing up. */
    private static final long MIMIC_SETTLE_TICKS = 140L; // 7 seconds

    /** Upward nudge (blocks) to counter the 1.15x awake-mimic scale sinking it into the floor. */
    private static final double MIMIC_AWAKE_GROUND_LIFT = 0.0625; // 1 pixel

    /** Game tick each disguised player entered water — gates the fish "right itself" delay. */
    private static final Map<UUID, Long> WATER_ENTER_TICK = new HashMap<>();

    /** A fish disguise must be submerged this long before it flips upright to swim. */
    private static final long FISH_RIGHTING_TICKS = 40L; // 2 seconds

    /** Per-disguised-player shulker idle peek timer, matching vanilla's ShulkerPeekGoal cadence. */
    private static final Map<UUID, ShulkerPeek> SHULKER_PEEKS = new HashMap<>();

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            PARTICLE_DUMMIES.clear();
            return;
        }

        Map<UUID, Identifier> disguises = DisguiseCache.snapshot();
        Set<UUID> active = new HashSet<>(disguises.keySet());
        PARTICLE_DUMMIES.entrySet().removeIf(e ->
                !active.contains(e.getKey()) || e.getValue().entity.level() != level);
        SHULKER_PEEKS.keySet().removeIf(id -> !active.contains(id));

        for (Map.Entry<UUID, Identifier> entry : disguises.entrySet()) {
            if (!(level.getPlayerByUUID(entry.getKey()) instanceof Player player)) {
                continue;
            }
            EntityType<?> type = EntityType.byString(entry.getValue().toString()).orElse(null);
            if (type == null || !shouldTickParticleDummy(type, level)) {
                continue;
            }

            Entity dummy = particleDummyFor(entry.getKey(), type, level);
            if (dummy == null) {
                continue;
            }

            mirror(dummy, player);
            dummy.tick();
        }
    }

    /**
     * Returns a substitute render state for a disguised player, or {@code null} if the player
     * isn't disguised (let the normal player extraction proceed).
     */
    public static EntityRenderState extractDisguise(EntityRenderDispatcher dispatcher,
            Player player, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return null;
        }
        Identifier id = DisguiseCache.get(player.getUUID());
        if (id == null) {
            return null;
        }
        EntityType<?> type = EntityType.byString(id.toString()).orElse(null);
        if (type == null) {
            return null;
        }
        Entity dummy = dummyFor(type, level);
        if (dummy == null) {
            return null;
        }
        mirror(dummy, player);
        // The dummy is a mob (not a Player), so this nested extract skips the disguise injection.
        EntityRenderState state = dispatcher.extractEntity(dummy, partialTick);
        // The dummy isn't registered in the level, so its sampled lightmap comes back as 0
        // (pitch black). Light the disguise exactly as the player is lit instead.
        state.lightCoords = dispatcher.getPackedLightCoords(player, partialTick);
        // Drive the mob's walk cycle from the player's actual movement so the legs swing in
        // step with the player (the dummy is never ticked, so its own walk state stays flat).
        if (state instanceof LivingEntityRenderState living) {
            living.walkAnimationPos = player.walkAnimation.position(partialTick);
            living.walkAnimationSpeed = player.walkAnimation.speed(partialTick);
        }
        applyUsePose(state, player, partialTick);
        applyTypeSpecific(state, player, partialTick);
        return state;
    }

    /** Per-disguise quirks that don't fit the generic mirror. */
    private static void applyTypeSpecific(EntityRenderState state, Player player, float partialTick) {
        // Water orientation: fish renderers flop the model on its side while out of water and
        // swim upright while in it. Mirror the player's water state, but require 2 continuous
        // seconds submerged before righting upright (so a fish flops, then settles into a swim).
        if (state instanceof LivingEntityRenderState aquatic) {
            long water = player.level().getGameTime();
            if (player.isInWater()) {
                WATER_ENTER_TICK.putIfAbsent(player.getUUID(), water);
            } else {
                WATER_ENTER_TICK.remove(player.getUUID());
            }
            Long entered = WATER_ENTER_TICK.get(player.getUUID());
            aquatic.isInWater = entered != null && water - entered >= FISH_RIGHTING_TICKS;
        }

        // Shift-snap: a mimic or shulker disguise locks to the block grid while sneaking, so it
        // sits flush like a real placed block instead of drifting around with the player's body.
        // The player's actual hitbox doesn't move — this is purely the visual.
        if (player.isShiftKeyDown() && (state instanceof MimicRenderState || state instanceof ShulkerRenderState)) {
            LivingEntityRenderState placed = (LivingEntityRenderState) state;
            placed.x = Math.floor(player.getX()) + 0.5;
            placed.y = Math.floor(player.getY());
            placed.z = Math.floor(player.getZ()) + 0.5;
        }

        // Flying mobs: their dummies are never ticked, so flap accumulators stay at 0 and the
        // wings sit still. Drive bat/parrot from movement so they fly + flap when the player is
        // moving or airborne and rest/perch when standing still. (Bee, allay, vex, ghast, phantom
        // already animate off ageInTicks, which we mirror — no extra work needed for them.)
        boolean airborneOrMoving = !player.onGround()
                || player.walkAnimation.speed(partialTick) > 0.02F;
        if (state instanceof BatRenderState bat) {
            // Resting = hanging upside down; flying = wings flap. The vanilla Bat entity drives
            // both AnimationStates from its tick() via a private setupAnimationStates() — but our
            // dummy bat is never ticked, so both animations stay stopped and the wings sit still.
            // Mirror Bat.setupAnimationStates() here so the fly/rest keyframe animations actually
            // play. (The render state holds direct references to the dummy bat's AnimationStates,
            // so starting them on the state starts them on the dummy too.)
            boolean resting = !airborneOrMoving;
            bat.isResting = resting;
            if (resting) {
                bat.flyAnimationState.stop();
                bat.restAnimationState.startIfStopped(player.tickCount);
            } else {
                bat.restAnimationState.stop();
                bat.flyAnimationState.startIfStopped(player.tickCount);
            }
        }
        if (state instanceof ParrotRenderState parrot) {
            // Re-synthesize the parrot's flapAngle (entity field never advances on the dummy).
            // Matches the vanilla formula: (sin(flap) + 1) * flapSpeed, with flap advanced by
            // ageInTicks and flapSpeed scaled by how active the player is.
            float flap = state.ageInTicks * 0.3F;
            float flapSpeed = airborneOrMoving ? 1.0F : 0.3F;
            parrot.flapAngle = (Mth.sin(flap) + 1.0F) * flapSpeed;
            parrot.pose = airborneOrMoving ? ParrotModel.Pose.FLYING : ParrotModel.Pose.STANDING;
        }

        // Mimic: come alive (chomping) while the player moves and for 7s after, then settle into
        // a closed chest once the player has stood still for the full delay.
        if (state instanceof MimicRenderState mimic) {
            long now = player.level().getGameTime();
            if (player.walkAnimation.speed(partialTick) > 0.02F) {
                LAST_MOVING_TICK.put(player.getUUID(), now);
            }
            Long lastMoving = LAST_MOVING_TICK.get(player.getUUID());
            boolean awake = lastMoving != null && now - lastMoving < MIMIC_SETTLE_TICKS;
            mimic.awakened = awake;
            // Skip past the 15-tick wake-up so movement reads as the active chomp animation.
            mimic.awakeTicks = awake ? 40 : 0;
            // The awake mimic is scaled up 1.15x, which sinks its base into the floor; lift it
            // back onto the ground. (The dormant chest is unscaled and already sits flush.)
            if (awake) {
                mimic.y += MIMIC_AWAKE_GROUND_LIFT;
            }
        }
    }

    /**
     * Mirror the player's item-use onto a humanoid disguise: which item is being used, how far
     * along, and the matching arm pose (drawing a bow, charging/holding a crossbow, blocking,
     * raising a trident/spyglass/horn). This makes e.g. a skeleton disguise visibly draw a bow.
     */
    private static void applyUsePose(EntityRenderState state, Player player, float partialTick) {
        if (!(state instanceof ArmedEntityRenderState armed)) {
            return;
        }
        armed.rightArmPose = armPoseFor(player, HumanoidArm.RIGHT);
        armed.leftArmPose = armPoseFor(player, HumanoidArm.LEFT);
        // Re-resolve the held-item models using the player as the holding entity, so a bow being
        // drawn shows its "pulling" model (bent bow + nocked arrow), a charging crossbow shows
        // its loading frames, etc. The dummy isn't using the item, so its own resolution is plain.
        var resolver = Minecraft.getInstance().getItemModelResolver();
        armed.rightHandItemStack = player.getMainHandItem();
        armed.leftHandItemStack = player.getOffhandItem();
        resolver.updateForLiving(armed.rightHandItemState, player.getMainHandItem(),
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, player);
        resolver.updateForLiving(armed.leftHandItemState, player.getOffhandItem(),
                ItemDisplayContext.THIRD_PERSON_LEFT_HAND, player);
        if (state instanceof HumanoidRenderState humanoid) {
            humanoid.isUsingItem = player.isUsingItem();
            humanoid.useItemHand = player.getUsedItemHand();
            humanoid.ticksUsingItem = player.getTicksUsingItem(partialTick);
            humanoid.maxCrossbowChargeDuration = CrossbowItem.getChargeDuration(player.getUseItem(), player);
        }
    }

    /** Replicates the vanilla player arm-pose logic for the hand mapped to {@code arm}. */
    private static HumanoidModel.ArmPose armPoseFor(Player player, HumanoidArm arm) {
        InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        }
        if (player.getUsedItemHand() == hand && player.getUseItemRemainingTicks() > 0) {
            ItemUseAnimation anim = stack.getUseAnimation();
            HumanoidModel.ArmPose pose = switch (anim) {
                case BOW -> HumanoidModel.ArmPose.BOW_AND_ARROW;
                case BLOCK -> HumanoidModel.ArmPose.BLOCK;
                case CROSSBOW -> HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                case SPYGLASS -> HumanoidModel.ArmPose.SPYGLASS;
                case TOOT_HORN -> HumanoidModel.ArmPose.TOOT_HORN;
                case BRUSH -> HumanoidModel.ArmPose.BRUSH;
                case TRIDENT -> HumanoidModel.ArmPose.THROW_TRIDENT;
                default -> null;
            };
            if (pose != null) {
                return pose;
            }
        }
        if (!player.swinging && stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        return HumanoidModel.ArmPose.EMPTY;
    }

    private static Entity dummyFor(EntityType<?> type, ClientLevel level) {
        Entity cached = DUMMIES.get(type);
        if (cached != null && cached.level() == level) {
            return cached;
        }
        Entity created = type.create(level, EntitySpawnReason.LOAD);
        if (created != null) {
            DUMMIES.put(type, created);
        }
        return created;
    }

    private static Entity particleDummyFor(UUID playerId, EntityType<?> type, ClientLevel level) {
        ParticleDummy cached = PARTICLE_DUMMIES.get(playerId);
        if (cached != null && cached.type == type && cached.entity.level() == level) {
            return cached.entity;
        }
        Entity created = type.create(level, EntitySpawnReason.LOAD);
        if (created != null) {
            created.setSilent(true);
            PARTICLE_DUMMIES.put(playerId, new ParticleDummy(type, created));
        }
        return created;
    }

    private static boolean shouldTickParticleDummy(EntityType<?> type, ClientLevel level) {
        Entity cached = DUMMIES.get(type);
        if (cached instanceof Shulker) {
            return false;
        }
        Entity created = cached != null && cached.level() == level ? cached : type.create(level, EntitySpawnReason.LOAD);
        if (created == null) {
            return false;
        }
        if (cached == null || cached.level() != level) {
            DUMMIES.put(type, created);
        }
        if (created instanceof Shulker) {
            return false;
        }
        return true;
    }

    /** Copy the player's world placement and animation pose onto the dummy for this frame. */
    private static void mirror(Entity dummy, Player player) {
        if (dummy instanceof Shulker) {
            // Shulker#setPos snaps to block centers and starts vanilla's attach-block
            // interpolation whenever the block changes. A moving player trips that every
            // few frames, so place disguise dummies directly instead.
            dummy.setPosRaw(player.getX(), player.getY(), player.getZ());
            EntityDimensions dimensions = dummy.getDimensions(dummy.getPose());
            dummy.setBoundingBox(dimensions.makeBoundingBox(dummy.position()));
            applyShulkerPeek((Shulker) dummy, player);
        } else {
            dummy.setPos(player.getX(), player.getY(), player.getZ());
        }
        dummy.xOld = player.xOld;
        dummy.yOld = player.yOld;
        dummy.zOld = player.zOld;
        dummy.setYRot(player.getYRot());
        dummy.yRotO = player.yRotO;
        dummy.setXRot(player.getXRot());
        dummy.xRotO = player.xRotO;
        dummy.setDeltaMovement(player.getDeltaMovement());
        dummy.setOnGround(player.onGround());
        dummy.tickCount = player.tickCount;
        dummy.setPose(player.getPose());
        dummy.setShiftKeyDown(player.isShiftKeyDown());

        if (dummy instanceof Armadillo armadillo) {
            boolean tucked = player.isShiftKeyDown();
            Armadillo.ArmadilloState targetState = tucked ? Armadillo.ArmadilloState.SCARED : Armadillo.ArmadilloState.IDLE;
            if (armadillo.getState() != targetState) {
                armadillo.switchToState(targetState);
            }
            if (tucked) {
                armadillo.rollOutAnimationState.stop();
                armadillo.rollUpAnimationState.stop();
                armadillo.peekAnimationState.stop();
            }
        }

        if (dummy instanceof LivingEntity d) {
            d.yBodyRot = player.yBodyRot;
            d.yBodyRotO = player.yBodyRotO;
            d.yHeadRot = player.yHeadRot;
            d.yHeadRotO = player.yHeadRotO;
            d.walkAnimation.setSpeed(player.walkAnimation.speed());
            d.swinging = player.swinging;
            d.swingTime = player.swingTime;
            d.attackAnim = player.attackAnim;
            d.oAttackAnim = player.oAttackAnim;
            d.hurtTime = player.hurtTime;
            // Mirror what the player is holding so humanoid disguises show the held item.
            d.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem());
            d.setItemInHand(InteractionHand.OFF_HAND, player.getOffhandItem());
        }
    }

    public static void clear() {
        DUMMIES.clear();
        PARTICLE_DUMMIES.clear();
        LAST_MOVING_TICK.clear();
        WATER_ENTER_TICK.clear();
        SHULKER_PEEKS.clear();
    }

    private static void applyShulkerPeek(Shulker shulker, Player player) {
        long now = player.level().getGameTime();
        ShulkerPeek peek = SHULKER_PEEKS.computeIfAbsent(player.getUUID(),
                ignored -> new ShulkerPeek(now));
        if (peek.lastTick != now) {
            if (peek.openTicks > 0) {
                peek.openTicks--;
            } else if (peek.cooldownTicks > 0) {
                peek.cooldownTicks--;
            } else if (player.getRandom().nextInt(40) == 0) {
                peek.openTicks = 20 * (1 + player.getRandom().nextInt(3));
            }
            peek.lastTick = now;
        }

        float target = peek.openTicks > 0 ? 0.3F : 0.0F;
        peek.currentPeekO = peek.currentPeek;
        peek.currentPeek = peek.currentPeek == target
                ? peek.currentPeek
                : peek.currentPeek < target ? Math.min(peek.currentPeek + 0.05F, target) : Math.max(peek.currentPeek - 0.05F, target);
        ShulkerAccessor accessor = (ShulkerAccessor) shulker;
        accessor.ironhold$setRawPeekAmount(peek.openTicks > 0 ? 30 : 0);
        accessor.ironhold$setCurrentPeekAmountO(peek.currentPeekO);
        accessor.ironhold$setCurrentPeekAmount(peek.currentPeek);
    }

    private record ParticleDummy(EntityType<?> type, Entity entity) {}

    private static final class ShulkerPeek {
        long lastTick;
        int cooldownTicks;
        int openTicks;
        float currentPeek;
        float currentPeekO;

        ShulkerPeek(long lastTick) {
            this.lastTick = lastTick;
        }
    }
}
