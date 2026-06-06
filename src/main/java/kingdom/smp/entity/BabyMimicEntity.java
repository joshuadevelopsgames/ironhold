package kingdom.smp.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Baby Mimic — a tiny tameable chest companion that hops like a baby slime.
 * Has a tongue but no teeth. Follows its owner by hopping.
 */
public class BabyMimicEntity extends TamableAnimal {

    private boolean wasOnGround = false;
    /** 5-slot inventory (uses the vanilla hopper GUI). */
    private final SimpleContainer mimicInventory = new SimpleContainer(5);

    public BabyMimicEntity(EntityType<? extends BabyMimicEntity> type, Level level) {
        super(type, level);
        this.moveControl = new BabyMimicMoveControl(this);
        this.setInvulnerable(true);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // Baby mimics are completely immune to all damage
        return false;
    }

    public SimpleContainer getMimicInventory() {
        return mimicInventory;
    }

    /** Drop all inventory contents at the entity's position and clear the inventory. */
    public void dropAllItems() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        for (int i = 0; i < mimicInventory.getContainerSize(); i++) {
            ItemStack stack = mimicInventory.getItem(i);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(serverLevel, stack);
                mimicInventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BabyMimicFollowOwnerGoal(this));
        this.goalSelector.addGoal(3, new BabyMimicRandomDirectionGoal(this));
        this.goalSelector.addGoal(5, new BabyMimicKeepHoppingGoal(this));
    }

    // ── Landing sound ───────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (this.onGround() && !this.wasOnGround) {
            this.playSound(SoundEvents.WOOD_PLACE, 0.4F,
                ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) * 1.4F);
        }
        this.wasOnGround = this.onGround();
    }

    @Override
    public void jumpFromGround() {
        Vec3 vel = this.getDeltaMovement();
        this.setDeltaMovement(vel.x, this.getJumpPower() * 0.7, vel.z);
        this.needsSync = true;
    }

    // ── Taming ──────────────────────────────────────────────────────────────

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isTame()) {
            if (!this.level().isClientSide()) {
                this.tame(player);
                this.navigation.stop();
                this.setTarget(null);
                this.setOrderedToSit(false);
            }
            return InteractionResult.SUCCESS;
        }

        // Only the owner can open the inventory
        if (this.isOwnedBy(player)) {
            if (!this.level().isClientSide()) {
                ServerPlayer sp = (ServerPlayer) player;
                // Play chest open sound
                this.level().playSound(null, this.blockPosition(), SoundEvents.CHEST_OPEN,
                    SoundSource.NEUTRAL, 0.5F, 1.4F);
                // Stop moving while inventory is open
                this.navigation.stop();
                // Open a vanilla hopper menu (5 slots, perfect rendering)
                sp.openMenu(new SimpleMenuProvider(
                    (id, playerInv, p) -> new HopperMenu(id, playerInv, this.mimicInventory),
                    Component.translatable("entity.ironhold.baby_mimic")));
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    // ── Persistence ────────────────────────────────────────────────────────
    // The MimicKey item is the single source of truth for both the custom name
    // and the stored inventory (see MimicKeyItem). The entity itself is NEVER
    // written to disk (shouldBeSaved() == false): on logout / chunk unload it
    // simply vanishes and is re-spawned from the key, restored with its items.
    // This guarantees a player can never end up with a duplicate mimic from a
    // stale on-disk copy lingering in an unloaded chunk.

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /**
     * Drop the stored items when the mimic is explicitly killed (e.g. /kill) and
     * clear the key's stored copy so the re-spawned mimic doesn't duplicate them.
     * Unequip drops are handled by {@link kingdom.smp.item.MimicKeyItem#removeAllCompanions}.
     */
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && reason == RemovalReason.KILLED) {
            dropAllItems();
            if (this.getOwner() instanceof ServerPlayer owner) {
                kingdom.smp.item.MimicKeyItem.clearStoredInventory(owner);
            }
        }
        super.remove(reason);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Slime-style hop movement
    // ═══════════════════════════════════════════════════════════════════════

    private static class BabyMimicMoveControl extends MoveControl {
        private float yRot;
        private int jumpDelay;
        private final BabyMimicEntity mimic;
        private boolean eager;

        BabyMimicMoveControl(BabyMimicEntity mimic) {
            super(mimic);
            this.mimic = mimic;
            this.yRot = 180.0F * mimic.getYRot() / (float) Math.PI;
        }

        void setDirection(float yRot, boolean eager) {
            this.yRot = yRot;
            this.eager = eager;
        }

        void setWantedMovement(double speed) {
            this.speedModifier = speed;
            this.operation = MoveControl.Operation.MOVE_TO;
        }

        @Override
        public void tick() {
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRot, 90.0F));
            this.mob.yHeadRot = this.mob.getYRot();
            this.mob.yBodyRot = this.mob.getYRot();

            if (this.operation != MoveControl.Operation.MOVE_TO) {
                this.mob.setZza(0.0F);
            } else {
                this.operation = MoveControl.Operation.WAIT;
                if (this.mob.onGround()) {
                    this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                    if (this.jumpDelay-- <= 0) {
                        this.jumpDelay = mimic.random.nextInt(20) + 10;
                        if (this.eager) {
                            this.jumpDelay /= 3;
                        }
                        mimic.getJumpControl().jump();
                    } else {
                        this.mob.xxa = 0.0F;
                        this.mob.zza = 0.0F;
                        this.mob.setSpeed(0.0F);
                    }
                } else {
                    this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                }
            }
        }
    }

    /** Follow owner by hopping toward them. */
    private static class BabyMimicFollowOwnerGoal extends Goal {
        private final BabyMimicEntity mimic;

        BabyMimicFollowOwnerGoal(BabyMimicEntity mimic) {
            this.mimic = mimic;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!mimic.isTame() || mimic.isOrderedToSit()) return false;
            LivingEntity owner = mimic.getOwner();
            if (owner == null) return false;
            return mimic.distanceToSqr(owner) > 25.0; // > 5 blocks away
        }

        @Override
        public boolean canContinueToUse() {
            if (!mimic.isTame() || mimic.isOrderedToSit()) return false;
            LivingEntity owner = mimic.getOwner();
            if (owner == null) return false;
            return mimic.distanceToSqr(owner) > 4.0; // > 2 blocks away
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity owner = mimic.getOwner();
            if (owner == null) return;
            mimic.lookAt(owner, 10.0F, 10.0F);

            // Teleport if very far away
            if (mimic.distanceToSqr(owner) > 144.0) {
                mimic.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                return;
            }

            if (mimic.getMoveControl() instanceof BabyMimicMoveControl mc) {
                mc.setDirection(mimic.getYRot(), true);
                mc.setWantedMovement(3.0);
            }
        }
    }

    /** Random hop direction when idle. */
    private static class BabyMimicRandomDirectionGoal extends Goal {
        private final BabyMimicEntity mimic;
        private float chosenDegrees;
        private int nextRandomizeTime;

        BabyMimicRandomDirectionGoal(BabyMimicEntity mimic) {
            this.mimic = mimic;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return mimic.onGround()
                && (!mimic.isTame() || mimic.isOrderedToSit() || mimic.getOwner() == null
                    || mimic.distanceToSqr(mimic.getOwner()) <= 25.0);
        }

        @Override
        public void tick() {
            if (--this.nextRandomizeTime <= 0) {
                this.nextRandomizeTime = this.adjustedTickDelay(40 + mimic.getRandom().nextInt(60));
                this.chosenDegrees = mimic.getRandom().nextInt(360);
            }
            if (mimic.getMoveControl() instanceof BabyMimicMoveControl mc) {
                mc.setDirection(this.chosenDegrees, false);
            }
        }
    }

    /** Keep hopping. */
    private static class BabyMimicKeepHoppingGoal extends Goal {
        private final BabyMimicEntity mimic;

        BabyMimicKeepHoppingGoal(BabyMimicEntity mimic) {
            this.mimic = mimic;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !mimic.isPassenger() && (!mimic.isTame() || !mimic.isOrderedToSit());
        }

        @Override
        public void tick() {
            if (mimic.getMoveControl() instanceof BabyMimicMoveControl mc) {
                mc.setWantedMovement(1.0);
            }
        }
    }
}
