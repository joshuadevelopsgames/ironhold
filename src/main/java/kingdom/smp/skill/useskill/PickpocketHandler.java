package kingdom.smp.skill.useskill;

import kingdom.smp.ModAttachments;
import kingdom.smp.entity.KnightEntity;
import kingdom.smp.entity.ShroomlingEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skyrim-style pickpocket and reverse-pickpocket (planting) on {@link Villager}
 * and {@link Player} targets.
 *
 * <p><b>Lift</b> — sneak + empty-hand right-click. Picks a random item from
 * the target's accessible loot pool and rolls against the player's Pickpocket
 * skill. Success transfers the item; failure aggros nearby guards.
 *
 * <p><b>Plant (reverse-pickpocket)</b> — sneak + held-item right-click.
 * Transfers one of the held item to the target. Slightly easier roll than
 * lifting; same failure consequences.
 *
 * <p><b>Sneak state coupling</b> — the {@link SneakDetectionTracker}-computed
 * state gates the roll:
 * <ul>
 *   <li>SEEN / DETECTED → auto-fail (a witness saw you reach in).</li>
 *   <li>NEARBY → moderate unwatched bonus.</li>
 *   <li>HIDDEN → full unwatched bonus.</li>
 * </ul>
 *
 * <p><b>Witness escalation</b> — even on a successful roll, a fresh
 * line-of-sight scan runs at the moment of the lift/plant. If any humanoid
 * witness sees the act, the player keeps the loot but still aggros guards
 * and triggers the public broadcast.
 *
 * <p><b>Cooldown</b> — keyed on (thief, target) pairs so multiple thieves
 * can't chain on one victim, and one thief can't spam multiple targets.
 *
 * <p><b>Lift sources for villagers</b> (uniform random):
 * <ul>
 *   <li>Items in the villager's physical pickup inventory (drained on success).</li>
 *   <li>For villagers with trade offers: a coin purse of 1–2 emeralds (synthesized).</li>
 *   <li>For villagers with trade offers: a copy of one trade-offer result item (synthesized).</li>
 * </ul>
 *
 * <p><b>Lift sources for players</b>: any non-empty slot in the target's
 * hotbar or main inventory (slots 0–35). Armor and offhand excluded.
 */
public final class PickpocketHandler {
    private PickpocketHandler() {}

    private static final int COOLDOWN_TICKS = 200;
    private static final float BASE_SUCCESS = 0.10f;
    private static final float PER_LEVEL = 0.015f;
    private static final float MAX_SUCCESS = 0.95f;
    private static final float MIN_SUCCESS = 0.05f;
    private static final float XP_PER_SUCCESS = 5f;
    private static final float XP_PER_FAIL = 1f;
    private static final float XP_PER_PLANT = 3f;

    /** Minimum Pickpocket level required to lift a Shroomling's cap off its head. */
    private static final int SHROOMCAP_MIN_LEVEL = 25;

    /** Planting is easier than lifting — no resistance from grasping the item. */
    private static final float PLANT_DIFFICULTY_BONUS = 0.10f;

    private static final double GUARD_AGGRO_RADIUS = 16.0;
    private static final double BROADCAST_RADIUS = 12.0;
    /** When caught in a village, alert all knights within this radius of any nearby bell. */
    private static final double VILLAGE_KNIGHT_AGGRO_RADIUS = 48.0;
    private static final int VILLAGE_BELL_LOOKUP_RADIUS = 64;

    /** Additive bonus when sneak state == HIDDEN (no humanoid in caution range). */
    private static final float UNWATCHED_BONUS_HIDDEN = 0.50f;
    /** Additive bonus when sneak state == NEARBY (humanoid in range but no LoS). */
    private static final float UNWATCHED_BONUS_NEARBY = 0.20f;

    /** Rarity-indexed difficulty penalty. Steeper than the vanilla 5%/tier curve. */
    private static final float[] RARITY_PENALTY = {0.00f, 0.10f, 0.25f, 0.45f};

    /** Penalty per successful theft from the same victim, before pickpocket-level mitigation. */
    private static final float REPEAT_PENALTY_PER_STEAL = 0.04f;
    private static final float REPEAT_PENALTY_CAP = 0.40f;
    /** Floor on the penalty multiplier — even at max pickpocket level, some wariness sticks. */
    private static final float REPEAT_PENALTY_MIN_MULT = 0.20f;

    private static final int PLAYER_MAIN_INV_SIZE = 36;

    /** CustomData key flagging an itemstack as "planted by a player" — lifts these first. */
    private static final String PLANTED_TAG = "ironhold_planted";

    private static final Map<CooldownKey, Long> lastAttempt = new ConcurrentHashMap<>();
    /** Successful-theft counter, keyed on (thief, victim). Drives the repeat-victim penalty. */
    private static final Map<CooldownKey, Integer> theftCount = new ConcurrentHashMap<>();

    private record CooldownKey(UUID thief, UUID target) {}
    private record LiftCandidate(ItemStack item, Runnable onSuccess) {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!player.isCrouching()) return;
        if (player.isCreative() || player.isSpectator()) return;

        LivingEntity target;
        if (event.getTarget() instanceof Villager villager) {
            target = villager;
        } else if (event.getTarget() instanceof KnightEntity knight) {
            target = knight;
        } else if (event.getTarget() instanceof ShroomlingEntity shroomling) {
            target = shroomling;
        } else if (event.getTarget() instanceof ServerPlayer victim && victim != player) {
            if (victim.isCreative() || victim.isSpectator()) return;
            target = victim;
        } else {
            return;
        }

        CooldownKey key = new CooldownKey(player.getUUID(), target.getUUID());
        long now = target.level().getGameTime();
        Long last = lastAttempt.get(key);
        if (last != null && now - last < COOLDOWN_TICKS) {
            actionBar(player, "§7Wait a moment.");
            cancel(event);
            return;
        }
        lastAttempt.put(key, now);

        // Capture sleep state so we can restore it after the interaction.
        // Pickpocketing should NOT wake sleeping villagers or players —
        // a quiet lift in the night is the whole point of bedroom raids.
        boolean wasSleeping = target.isSleeping();
        java.util.Optional<net.minecraft.core.BlockPos> sleepPos =
            wasSleeping ? target.getSleepingPos() : java.util.Optional.empty();

        if (target instanceof ShroomlingEntity shroomling) {
            // Shroomlings have exactly one liftable thing — their cap — and nothing
            // can be planted on a mushroom. Route both hands to the cap-lift path.
            if (player.getMainHandItem().isEmpty()) {
                doLiftShroomcap(player, shroomling);
            } else {
                actionBar(player, "§7You can't slip anything onto a Shroomling.");
            }
        } else if (player.getMainHandItem().isEmpty()) {
            doLift(player, target);
        } else {
            doPlant(player, target);
        }

        if (wasSleeping && !target.isSleeping() && sleepPos.isPresent()) {
            target.startSleeping(sleepPos.get());
        }

        cancel(event);
    }

    private static void cancel(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    // ── Lift ─────────────────────────────────────────────────────────────────

    private static void doLift(ServerPlayer player, LivingEntity target) {
        LiftCandidate lift;
        if (target instanceof Villager v) {
            lift = pickLiftCandidateVillager(v);
        } else if (target instanceof KnightEntity k) {
            lift = pickLiftCandidateKnight(k);
        } else {
            lift = pickLiftCandidatePlayer((ServerPlayer) target);
        }

        if (lift == null) {
            actionBar(player, "§7Their pockets are empty.");
            return;
        }

        PlayerUseSkills skills = player.getData(ModAttachments.USE_SKILLS.get());
        int pickpocketLevel = skills.levelFor(UseSkill.PICKPOCKET);
        byte sneakState = SneakDetectionTracker.getCachedState(player.getUUID());

        if (sneakState == SneakDetectionTracker.SEEN || sneakState == SneakDetectionTracker.DETECTED) {
            onCaught(player, target, skills, pickpocketLevel, "spotted");
            return;
        }

        CooldownKey thiefVictim = new CooldownKey(player.getUUID(), target.getUUID());
        float repeatPenalty = repeatTheftPenalty(player.getUUID(), target.getUUID(), pickpocketLevel);
        float chance = computeChance(pickpocketLevel, sneakState, rarityPenalty(lift.item()), 0f, repeatPenalty);
        if (target.getRandom().nextFloat() >= chance) {
            onCaught(player, target, skills, pickpocketLevel, "fumbled");
            return;
        }

        theftCount.merge(thiefVictim, 1, Integer::sum);
        ItemStack lifted = lift.item();
        // Snapshot display strings — Inventory.add mutates the stack (drains count to 0)
        // and an empty stack reads its name as "Air".
        int displayCount = lifted.getCount();
        String displayName = lifted.getHoverName().getString();
        lift.onSuccess().run();
        if (!player.getInventory().add(lifted)) {
            player.drop(lifted, false);
        }

        playLiftSound(target);
        spawnSuccessParticles(target);
        actionBar(player, "§aLifted: §f" + displayCount + "× " + displayName);
        awardXp(player, skills, UseSkill.PICKPOCKET, XP_PER_SUCCESS, pickpocketLevel);

        // Witness escalation — even a clean roll can be witnessed at the moment of the lift
        if (anyWitnessHasLos(player, target)) {
            actionBar(player, "§eA witness saw you. Run.");
            aggroGuards(player, target);
            broadcastCaught(player, target);
        }
    }

    // ── Lift: Shroomling cap ──────────────────────────────────────────────────

    /**
     * Lifts a {@link ShroomlingEntity}'s cap off its head. Unlike ordinary lifts
     * this is gated behind {@link #SHROOMCAP_MIN_LEVEL} — a Shroomling's cap is
     * fused on tight, so only a deft thief can peel it free. Success yields the
     * variant-matched shroomcap (blue or orange) and leaves the Shroomling
     * permanently capless. Being hidden still improves the odds (via the sneak
     * bonus in {@link #computeChance}), but a wild mushroom raises no guards, so
     * a fumble just costs the attempt — no village aggro or distrust.
     */
    private static void doLiftShroomcap(ServerPlayer player, ShroomlingEntity shroomling) {
        if (shroomling.isCapless()) {
            actionBar(player, "§7Its cap is already gone.");
            return;
        }

        PlayerUseSkills skills = player.getData(ModAttachments.USE_SKILLS.get());
        int pickpocketLevel = skills.levelFor(UseSkill.PICKPOCKET);
        if (pickpocketLevel < SHROOMCAP_MIN_LEVEL) {
            actionBar(player, "§7Its cap is fused on tight — you're not deft enough. "
                + "§8(Pickpocket " + pickpocketLevel + "/" + SHROOMCAP_MIN_LEVEL + ")");
            return;
        }

        byte sneakState = SneakDetectionTracker.getCachedState(player.getUUID());
        ItemStack cap = new ItemStack(shroomling.isOrange()
            ? kingdom.smp.ModItems.SHROOMCAP_ORANGE.get()
            : kingdom.smp.ModItems.SHROOMCAP.get());

        // The cap's own rarity (uncommon blue / rare orange) feeds the difficulty,
        // so the prized orange cap is meaningfully harder to lift cleanly.
        float chance = computeChance(pickpocketLevel, sneakState, rarityPenalty(cap), 0f, 0f);
        if (shroomling.getRandom().nextFloat() >= chance) {
            actionBar(player, "§cYou fumbled — its cap held fast.");
            awardXp(player, skills, UseSkill.PICKPOCKET, XP_PER_FAIL, pickpocketLevel);
            spawnFailureParticles(shroomling);
            return;
        }

        shroomling.setCapless(true);
        if (!player.getInventory().add(cap)) {
            player.drop(cap, false);
        }
        playLiftSound(shroomling);
        spawnSuccessParticles(shroomling);
        actionBar(player, "§aLifted: §f" + cap.getHoverName().getString());
        awardXp(player, skills, UseSkill.PICKPOCKET, XP_PER_SUCCESS, pickpocketLevel);
    }

    // ── Plant (reverse-pickpocket) ──────────────────────────────────────────

    private static void doPlant(ServerPlayer player, LivingEntity target) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return; // double-check; main check is in onEntityInteract

        PlayerUseSkills skills = player.getData(ModAttachments.USE_SKILLS.get());
        int pickpocketLevel = skills.levelFor(UseSkill.PICKPOCKET);
        byte sneakState = SneakDetectionTracker.getCachedState(player.getUUID());

        if (sneakState == SneakDetectionTracker.SEEN || sneakState == SneakDetectionTracker.DETECTED) {
            onCaught(player, target, skills, pickpocketLevel, "spotted");
            return;
        }

        float repeatPenalty = repeatTheftPenalty(player.getUUID(), target.getUUID(), pickpocketLevel);
        float chance = computeChance(pickpocketLevel, sneakState, 0f, PLANT_DIFFICULTY_BONUS, repeatPenalty);
        if (target.getRandom().nextFloat() >= chance) {
            onCaught(player, target, skills, pickpocketLevel, "fumbled");
            return;
        }

        ItemStack planted = held.split(1);
        String plantedName = planted.getHoverName().getString();
        ItemStack delivered;
        if (target instanceof ServerPlayer) {
            // Player plants go in as an invisible MasqueradeItem — empty slot, hover-reveals.
            delivered = kingdom.smp.item.MasqueradeItem.wrap(
                planted, player.getName().getString(), pickpocketLevel,
                kingdom.smp.ModItems.MASQUERADE.get());
        } else {
            delivered = planted;
            if (target instanceof Villager) {
                markAsPlanted(delivered);
            }
        }
        givePlanted(target, delivered);

        playPlantSound(target);
        spawnSuccessParticles(target);
        actionBar(player, "§aPlanted: §f" + plantedName);
        awardXp(player, skills, UseSkill.PICKPOCKET, XP_PER_PLANT, pickpocketLevel);

        if (anyWitnessHasLos(player, target)) {
            actionBar(player, "§eA witness saw you. Run.");
            aggroGuards(player, target);
            broadcastCaught(player, target);
        }
    }

    private static void givePlanted(LivingEntity target, ItemStack item) {
        if (target instanceof ServerPlayer p) {
            if (!p.getInventory().add(item)) p.drop(item, false);
            return;
        }
        if (target instanceof Villager v) {
            ItemStack leftover = v.getInventory().addItem(item);
            if (!leftover.isEmpty() && v.level() instanceof ServerLevel sl) {
                v.spawnAtLocation(sl, leftover);
            }
        }
    }

    // ── Chance calc ─────────────────────────────────────────────────────────

    private static float computeChance(int pickpocketLevel, byte sneakState,
                                        float rarityPenalty, float extraBonus, float repeatPenalty) {
        float watchBonus = switch (sneakState) {
            case SneakDetectionTracker.HIDDEN -> UNWATCHED_BONUS_HIDDEN;
            case SneakDetectionTracker.NEARBY -> UNWATCHED_BONUS_NEARBY;
            default -> 0f; // SEEN/DETECTED are auto-fail upstream; this is defensive
        };
        return Math.clamp(
            BASE_SUCCESS + pickpocketLevel * PER_LEVEL - rarityPenalty + watchBonus + extraBonus - repeatPenalty,
            MIN_SUCCESS, MAX_SUCCESS);
    }

    /**
     * Per-victim wariness penalty. Grows linearly with successful thefts from
     * this specific victim, capped at {@link #REPEAT_PENALTY_CAP}; mitigated
     * by pickpocket level but never fully eliminated (floor at
     * {@link #REPEAT_PENALTY_MIN_MULT}).
     */
    private static float repeatTheftPenalty(UUID thief, UUID victim, int pickpocketLevel) {
        int count = theftCount.getOrDefault(new CooldownKey(thief, victim), 0);
        if (count <= 0) return 0f;
        float base = Math.min(REPEAT_PENALTY_CAP, count * REPEAT_PENALTY_PER_STEAL);
        float mitigation = Math.max(REPEAT_PENALTY_MIN_MULT, 1f - pickpocketLevel / 100f);
        return base * mitigation;
    }

    private static float rarityPenalty(ItemStack stack) {
        Rarity rarity = stack.getRarity();
        int ord = Math.min(rarity.ordinal(), RARITY_PENALTY.length - 1);
        return RARITY_PENALTY[ord];
    }

    // ── Witness scan (fresh, at moment of action) ──────────────────────────

    /**
     * Fresh line-of-sight check around the thief for humanoid witnesses other
     * than the mark. Used for success-time escalation, separate from the
     * tracker's cached state (which may be up to 5 ticks stale).
     */
    private static boolean anyWitnessHasLos(ServerPlayer thief, LivingEntity exclude) {
        double radius = SneakDetectionTracker.effectiveCautionRadius(thief);
        double radiusSq = radius * radius;
        AABB box = thief.getBoundingBox().inflate(radius);
        for (LivingEntity entity : thief.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (entity == thief || entity == exclude) continue;
            if (!SneakDetectionTracker.countsAsWitness(entity)) continue;
            if (entity.distanceToSqr(thief) > radiusSq) continue;
            if (entity.hasLineOfSight(thief)) return true;
        }
        return false;
    }

    // ── Caught / failure ────────────────────────────────────────────────────

    private static void onCaught(ServerPlayer player, LivingEntity target,
                                  PlayerUseSkills skills, int prevLevel, String reason) {
        awardXp(player, skills, UseSkill.PICKPOCKET, XP_PER_FAIL, prevLevel);

        if (target instanceof Villager villager) {
            villager.getGossips().add(player.getUUID(), GossipType.MAJOR_NEGATIVE, 10);
            villager.getGossips().add(player.getUUID(), GossipType.MINOR_NEGATIVE, 25);
            villager.level().playSound(null, villager.getX(), villager.getY(), villager.getZ(),
                SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0f, 1.0f);
        } else if (target instanceof ServerPlayer victim) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.8f, 1.6f);
            actionBar(victim, "§c" + player.getName().getString() + " tried to pickpocket you!");
        }

        aggroGuards(player, target);
        broadcastCaught(player, target);
        spawnFailureParticles(target);
        recordDistrust(player, target);

        String suffix = reason.isEmpty() ? "!" : " (" + reason + ")!";
        Component msg = Component.literal("§cYou were caught" + suffix).withStyle(ChatFormatting.RED);
        player.connection.send(new ClientboundSetActionBarTextPacket(msg));
    }

    /**
     * Records distrust against the thief on the target and on any nearby
     * villager / knight witness with line of sight. Distrust persists on the
     * entity via NBT — those entities will keep watching this thief on sight.
     */
    private static void recordDistrust(ServerPlayer thief, LivingEntity target) {
        if (target instanceof Villager || target instanceof KnightEntity) {
            DistrustState.markDistrust(target, thief.getUUID());
        }
        double radius = SneakDetectionTracker.effectiveCautionRadius(thief);
        AABB box = thief.getBoundingBox().inflate(radius);
        for (LivingEntity e : thief.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == thief || e == target) continue;
            if (!(e instanceof Villager) && !(e instanceof KnightEntity)) continue;
            if (!e.hasLineOfSight(thief)) continue;
            DistrustState.markDistrust(e, thief.getUUID());
        }
    }

    private static void aggroGuards(ServerPlayer player, LivingEntity target) {
        AABB box = target.getBoundingBox().inflate(GUARD_AGGRO_RADIUS);
        for (IronGolem golem : target.level().getEntitiesOfClass(IronGolem.class, box)) {
            golem.setTarget(player);
        }
        for (KnightEntity knight : target.level().getEntitiesOfClass(KnightEntity.class, box)) {
            knight.setTarget(player);
        }
        alertVillageKnights(player, target);
    }

    /**
     * If the crime happened inside a village (defined by a bell POI within
     * {@link #VILLAGE_BELL_LOOKUP_RADIUS}), aggro every knight inside that
     * village onto the thief. A village's "inside" is a {@link
     * #VILLAGE_KNIGHT_AGGRO_RADIUS}-block radius around each bell.
     */
    private static void alertVillageKnights(ServerPlayer thief, LivingEntity target) {
        if (!(target.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        PoiManager poi = level.getPoiManager();
        BlockPos targetPos = target.blockPosition();
        poi.findAll(
            holder -> holder.is(PoiTypes.MEETING),
            pos -> true,
            targetPos, VILLAGE_BELL_LOOKUP_RADIUS,
            PoiManager.Occupancy.ANY
        ).forEach(bell -> {
            AABB villageBox = new AABB(bell).inflate(VILLAGE_KNIGHT_AGGRO_RADIUS);
            for (KnightEntity knight : level.getEntitiesOfClass(KnightEntity.class, villageBox)) {
                knight.setTarget(thief);
            }
        });
    }

    private static void broadcastCaught(ServerPlayer player, LivingEntity target) {
        String thiefName = player.getName().getString();
        String victimName = target instanceof Player p ? p.getName().getString() : "a villager";
        Component msg = Component.literal("§c[Thief] §f" + thiefName
            + " §7was caught pickpocketing §f" + victimName);
        AABB box = target.getBoundingBox().inflate(BROADCAST_RADIUS);
        for (ServerPlayer nearby : target.level().getEntitiesOfClass(ServerPlayer.class, box)) {
            if (nearby == player) continue;
            if (target instanceof ServerPlayer victim && nearby == victim) continue; // already action-barred
            nearby.sendSystemMessage(msg);
        }
    }

    // ── Audio / particles ───────────────────────────────────────────────────

    private static void playLiftSound(LivingEntity target) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 0.5f, 1.6f);
    }

    private static void playPlantSound(LivingEntity target) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 0.5f, 0.8f);
    }

    private static void spawnSuccessParticles(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.POOF,
            target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
            3, 0.2, 0.2, 0.2, 0.0);
    }

    private static void spawnFailureParticles(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.ANGRY_VILLAGER,
            target.getX(), target.getY() + target.getBbHeight() + 0.2, target.getZ(),
            5, 0.3, 0.1, 0.3, 0.0);
    }

    // ── XP ──────────────────────────────────────────────────────────────────

    private static void awardXp(ServerPlayer player, PlayerUseSkills skills,
                                  UseSkill skill, float amount, int prevLevel) {
        PlayerUseSkills next = skills.withAddedXp(skill, amount);
        player.setData(ModAttachments.USE_SKILLS.get(), next);
        int newLevel = next.levelFor(skill);
        if (newLevel > prevLevel) {
            player.sendSystemMessage(Component.literal(
                "§6§l" + skill.displayName() + " §r§7level up §8(§f" + prevLevel + " §8→ §a" + newLevel + "§8)"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4f, 1.4f);
        }
    }

    // ── Lift-candidate construction ─────────────────────────────────────────

    private static LiftCandidate pickLiftCandidateVillager(Villager villager) {
        List<LiftCandidate> plantedCandidates = new ArrayList<>();
        List<LiftCandidate> naturalCandidates = new ArrayList<>();

        int size = villager.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack item = villager.getInventory().getItem(i);
            if (item.isEmpty()) continue;
            final int slot = i;
            LiftCandidate c = new LiftCandidate(item.copyWithCount(1), () -> drainVillagerSlot(villager, slot));
            if (isPlanted(item)) {
                plantedCandidates.add(c);
            } else {
                naturalCandidates.add(c);
            }
        }

        // Planted items are stolen FIRST — they shadow the rest of the loot pool
        // until exhausted. Reverse-pickpocketing a cursed item onto a villager
        // means the next thief who hits them gets that cursed item, guaranteed.
        if (!plantedCandidates.isEmpty()) {
            return plantedCandidates.get(villager.getRandom().nextInt(plantedCandidates.size()));
        }

        List<LiftCandidate> candidates = new ArrayList<>(naturalCandidates);
        if (!villager.getOffers().isEmpty()) {
            int emeralds = 1 + villager.getRandom().nextInt(2);
            candidates.add(new LiftCandidate(new ItemStack(Items.EMERALD, emeralds), () -> {}));

            for (MerchantOffer offer : villager.getOffers()) {
                ItemStack sample = offer.getResult().copy();
                if (sample.isEmpty()) continue;
                sample.setCount(1);
                candidates.add(new LiftCandidate(sample, () -> {}));
            }
        }

        if (candidates.isEmpty()) return null;
        return candidates.get(villager.getRandom().nextInt(candidates.size()));
    }

    private static void markAsPlanted(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(PLANTED_TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static boolean isPlanted(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        return data.copyTag().getBooleanOr(PLANTED_TAG, false);
    }

    /**
     * Knights carry chainmail / iron gear and a bit of pay. Loot is synthesized —
     * we don't strip their actually-equipped armor, just generate a thematic
     * piece (so they don't end up running around naked after a successful lift).
     */
    private static LiftCandidate pickLiftCandidateKnight(KnightEntity knight) {
        var random = knight.getRandom();
        ItemStack[] pool = {
            new ItemStack(Items.CHAINMAIL_HELMET),
            new ItemStack(Items.CHAINMAIL_CHESTPLATE),
            new ItemStack(Items.CHAINMAIL_LEGGINGS),
            new ItemStack(Items.CHAINMAIL_BOOTS),
            new ItemStack(Items.IRON_HELMET),
            new ItemStack(Items.IRON_CHESTPLATE),
            new ItemStack(Items.IRON_LEGGINGS),
            new ItemStack(Items.IRON_BOOTS),
            new ItemStack(Items.IRON_SWORD),
            new ItemStack(Items.IRON_AXE),
            new ItemStack(Items.IRON_INGOT, 1 + random.nextInt(3)),
            new ItemStack(Items.EMERALD, 1 + random.nextInt(2))
        };
        return new LiftCandidate(pool[random.nextInt(pool.length)], () -> {});
    }

    private static LiftCandidate pickLiftCandidatePlayer(ServerPlayer victim) {
        // Fool's gold is the always-targeted item for pickpocketing players.
        // Scan the inventory first and return it if found.
        var inv = victim.getInventory();
        int limit = Math.min(PLAYER_MAIN_INV_SIZE, inv.getContainerSize());
        for (int i = 0; i < limit; i++) {
            ItemStack item = inv.getItem(i);
            if (item.getItem() == kingdom.smp.ModItems.FOOLS_GOLD.get()) {
                final int slot = i;
                return new LiftCandidate(item.copyWithCount(1), () -> drainPlayerSlot(victim, slot));
            }
        }

        // No fool's gold — fall back to random items from the main inventory.
        List<LiftCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            ItemStack item = inv.getItem(i);
            if (item.isEmpty()) continue;
            final int slot = i;
            candidates.add(new LiftCandidate(item.copyWithCount(1), () -> drainPlayerSlot(victim, slot)));
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(victim.getRandom().nextInt(candidates.size()));
    }

    private static void drainVillagerSlot(Villager villager, int slot) {
        ItemStack current = villager.getInventory().getItem(slot);
        ItemStack remaining = current.copyWithCount(current.getCount() - 1);
        villager.getInventory().setItem(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
    }

    private static void drainPlayerSlot(Player victim, int slot) {
        ItemStack current = victim.getInventory().getItem(slot);
        ItemStack remaining = current.copyWithCount(current.getCount() - 1);
        victim.getInventory().setItem(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
    }

    private static void actionBar(ServerPlayer player, String text) {
        player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(text)));
    }
}
