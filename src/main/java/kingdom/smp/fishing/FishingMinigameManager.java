package kingdom.smp.fishing;

import kingdom.smp.ModAttachments;
import kingdom.smp.net.FishingBiteStartPayload;
import kingdom.smp.skill.Profession;
import kingdom.smp.skill.ProfessionRank;
import kingdom.smp.skill.SkillEffects;
import kingdom.smp.skill.useskill.PlayerUseSkills;
import kingdom.smp.skill.useskill.UseSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side state for active fishing-bite minigame sessions. There is at
 * most one session per player at a time. The session is started when the
 * bite mixin observes a 0→positive nibble on a hook owned by the player, and
 * resolved when the client returns a {@code FishingMinigameResultPayload} —
 * or when the player logs out or their hook is removed.
 *
 * <p>On bite we pre-roll the fishing loot table once and stash the result in
 * the session so the client can render the exact item the player will
 * receive. On win, {@link kingdom.smp.mixin.FishingHookRetrieveMixin}
 * substitutes those pre-rolled drops into vanilla {@code retrieve()} via
 * {@link #takePreRolledForRetrieve(ServerPlayer)}, guaranteeing
 * display == catch. Use-skill XP is awarded here in {@link #resolve} rather
 * than via {@code ItemFishedEvent}, so it's deterministic regardless of how
 * vanilla spawns the drops.
 *
 * <p>Hook-zone height grows with the player's Fishing rank and use-skill
 * level so Master fishers / dedicated grinders have a wider catch window
 * than a fresh Novice.
 */
public final class FishingMinigameManager {

    private FishingMinigameManager() {}

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    /** Pre-rolled drops queued for the upcoming {@code FishingHook.retrieve}. */
    private static final Map<UUID, List<ItemStack>> PENDING_PREROLLS = new ConcurrentHashMap<>();

    private record Session(FishingHook hook, List<ItemStack> preRolledDrops) {}

    /**
     * Called by {@link kingdom.smp.mixin.FishingHookBiteMixin} when a bite
     * is first detected for {@code player}'s hook. No-op if the player
     * already has a session.
     */
    public static void tryStart(ServerPlayer player, FishingHook hook) {
        UUID uuid = player.getUUID();
        if (SESSIONS.containsKey(uuid)) return;

        ProfessionRank rank = SkillEffects.rankFor(player, Profession.FISHING);
        PlayerUseSkills useSkills = player.getData(ModAttachments.USE_SKILLS.get());
        int useLevel = useSkills.levelFor(UseSkill.FISHING);
        int hookZone = hookZoneFor(rank, useLevel);
        int motion = pickMotion(player, rank);

        // ── Pre-roll the loot once so the minigame shows the actual catch.
        ItemStack rod = findRod(player);
        List<ItemStack> drops;
        if (rod == null) {
            drops = List.of();
        } else {
            drops = FishingLootRoller.roll(hook, player, rod);
            // Fold the Fishing-profession bonus-drop perk into the pre-roll so it's
            // both reflected in the catch and consistent with the displayed item.
            int bonusChance = SkillEffects.extraDropChancePercent(player, Profession.FISHING);
            if (bonusChance > 0 && !drops.isEmpty()
                    && player.getRandom().nextInt(100) < bonusChance) {
                ItemStack copy = drops.get(0).copy();
                copy.setCount(1);
                drops.add(copy);
            }
        }

        ItemStack preview = drops.isEmpty() ? motionFallback(motion) : drops.get(0).copy();

        SESSIONS.put(uuid, new Session(hook, drops));
        ((IFishingHookMinigame) hook).ironhold$setMinigameActive(true);
        PacketDistributor.sendToPlayer(player,
                new FishingBiteStartPayload(hook.getId(), hookZone, motion, preview));
    }

    /**
     * Called by the network handler when the client returns a result. Win →
     * vanilla retrieve() (the retrieve mixin swaps in our pre-rolled drops),
     * award use-skill XP, damage rod. Loss → hook.discard().
     */
    public static void resolve(ServerPlayer player, boolean won) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) return;
        FishingHook hook = session.hook();
        ((IFishingHookMinigame) hook).ironhold$setMinigameActive(false);

        if (hook.isRemoved() || hook.getPlayerOwner() != player) {
            PENDING_PREROLLS.remove(player.getUUID());
            return;
        }

        if (won) {
            ItemStack rod = player.getMainHandItem();
            InteractionHand hand = InteractionHand.MAIN_HAND;
            if (!(rod.getItem() instanceof FishingRodItem)) {
                rod = player.getOffhandItem();
                hand = InteractionHand.OFF_HAND;
            }
            if (rod.getItem() instanceof FishingRodItem) {
                List<ItemStack> drops = session.preRolledDrops();
                if (drops != null && !drops.isEmpty()) {
                    PENDING_PREROLLS.put(player.getUUID(), drops);
                }
                int damage = hook.retrieve(rod);
                rod.hurtAndBreak(damage, player, hand);
                awardFishingXp(player, drops);
            } else {
                hook.discard();
            }
            PENDING_PREROLLS.remove(player.getUUID());
        } else {
            hook.discard();
        }
    }

    /** Clear any pending session on logout so it doesn't leak. */
    public static void clear(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null && !session.hook().isRemoved()) {
            ((IFishingHookMinigame) session.hook()).ironhold$setMinigameActive(false);
        }
        PENDING_PREROLLS.remove(player.getUUID());
    }

    /**
     * Consumed by {@link kingdom.smp.mixin.FishingHookRetrieveMixin} to
     * substitute the pre-rolled drops into vanilla {@code retrieve()}.
     */
    public static List<ItemStack> takePreRolledForRetrieve(ServerPlayer player) {
        return PENDING_PREROLLS.remove(player.getUUID());
    }

    // ── XP ────────────────────────────────────────────────────────────────

    private static void awardFishingXp(ServerPlayer player, List<ItemStack> drops) {
        if (drops == null || drops.isEmpty()) return;
        float xp = 0f;
        for (ItemStack s : drops) {
            xp += s.is(ItemTags.FISHES) ? 4f : 6f; // treasure/junk worth slightly more
        }
        if (xp <= 0f) return;
        var attach = ModAttachments.USE_SKILLS.get();
        PlayerUseSkills skills = player.getData(attach);
        int prevLevel = skills.levelFor(UseSkill.FISHING);
        PlayerUseSkills next = skills.withAddedXp(UseSkill.FISHING, xp);
        player.setData(attach, next);
        int newLevel = next.levelFor(UseSkill.FISHING);
        if (newLevel > prevLevel) {
            player.sendSystemMessage(Component.literal(
                "§6§lFishing §r§7level up §8(§f" + prevLevel + " §8→ §a" + newLevel + "§8)"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4f, 1.4f);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static ItemStack findRod(ServerPlayer player) {
        ItemStack rod = player.getMainHandItem();
        if (rod.getItem() instanceof FishingRodItem) return rod;
        rod = player.getOffhandItem();
        if (rod.getItem() instanceof FishingRodItem) return rod;
        return null;
    }

    /** Last-resort display if loot rolling somehow produced nothing. */
    private static ItemStack motionFallback(int motion) {
        return switch (motion) {
            case 1 -> new ItemStack(Items.SALMON);
            case 2 -> new ItemStack(Items.PUFFERFISH);
            default -> new ItemStack(Items.COD);
        };
    }

    // ── Tuning ────────────────────────────────────────────────────────────

    /**
     * Pixel height of the hook zone in the minigame screen. Starts short and
     * grows with the Fishing use-skill level so early levels are a genuine
     * challenge and leveling up feels rewarding.
     * <pre>
     *   level 0   →  14 px  (small, hard)
     *   level 25  →  62 px
     *   level 50  → 110 px  (wide, forgiving endgame)
     * </pre>
     * The profession rank adds a small flat bonus on top.
     */
    private static int hookZoneFor(ProfessionRank rank, int useLevel) {
        int cap = UseSkill.FISHING.maxLevel();
        int clamped = Math.max(0, Math.min(cap, useLevel));
        int useBonus = 14 + (clamped * 96) / cap;
        int rankBonus = rank == null ? 0 : switch (rank) {
            case NOVICE -> 4;
            case APPRENTICE -> 8;
            case JOURNEYMAN -> 12;
            case EXPERT -> 16;
            case MASTER -> 20;
        };
        return useBonus + rankBonus;
    }

    /** Pick a fish motion pattern, biased harder at higher ranks. */
    private static int pickMotion(ServerPlayer player, ProfessionRank rank) {
        int roll = player.getRandom().nextInt(100);
        if (rank == null || rank == ProfessionRank.NOVICE) {
            if (roll < 70) return 0;
            if (roll < 95) return 1;
            return 2;
        }
        return switch (rank) {
            case APPRENTICE -> roll < 55 ? 0 : roll < 90 ? 1 : 2;
            case JOURNEYMAN -> roll < 40 ? 0 : roll < 80 ? 1 : 2;
            case EXPERT     -> roll < 30 ? 0 : roll < 70 ? 1 : 2;
            case MASTER     -> roll < 20 ? 0 : roll < 60 ? 1 : 2;
            default         -> 0;
        };
    }
}
