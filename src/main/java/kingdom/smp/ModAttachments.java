package kingdom.smp;

import kingdom.smp.accessory.AccessoryInventory;
import kingdom.smp.disguise.DisguiseState;
import kingdom.smp.food.KnownRecipes;
import kingdom.smp.game.ActivePromotionStone;
import kingdom.smp.game.CloudJumpState;
import kingdom.smp.npc.PlayerNpcBonds;
import kingdom.smp.rpg.CompletedClasses;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import kingdom.smp.rpg.ability.AbilityCooldowns;
import kingdom.smp.rpg.ability.GuardianVowData;
import kingdom.smp.rpg.ability.TauntMarkData;
import kingdom.smp.skill.useskill.PlayerUseSkills;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private ModAttachments() {}

    /** Syncs RPG data to the client so UI (inventory weight cap) matches the server. */
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerKingdomRpgData> PLAYER_RPG_STREAM =
        StreamCodec.composite(
            ByteBufCodecs.INT,
            PlayerKingdomRpgData::kingdomIndex,
            ByteBufCodecs.INT,
            PlayerKingdomRpgData::classIndex,
            ByteBufCodecs.INT,
            PlayerKingdomRpgData::classLevel,
            ByteBufCodecs.INT,
            PlayerKingdomRpgData::xpIntoLevel,
            PlayerKingdomRpgData::new);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Ironhold.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerKingdomRpgData>> PLAYER_RPG =
        ATTACHMENT_TYPES.register(
            "player_rpg",
            () -> AttachmentType.builder(PlayerKingdomRpgData::defaultData)
                .serialize(PlayerKingdomRpgData.CODEC)
                .copyOnDeath()
                .sync(ModAttachments.PLAYER_RPG_STREAM)
                .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AccessoryInventory>> ACCESSORY_INV =
        ATTACHMENT_TYPES.register(
            "accessory_inv",
            () -> AttachmentType.builder(AccessoryInventory::defaultData)
                .serialize(AccessoryInventory.MAP_CODEC)
                .copyOnDeath()
                .sync(AccessoryInventory.STREAM_CODEC)
                .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CompletedClasses>> COMPLETED_CLASSES =
        ATTACHMENT_TYPES.register(
            "completed_classes",
            () -> AttachmentType.builder(CompletedClasses::empty)
                .serialize(CompletedClasses.CODEC)
                .copyOnDeath()
                .build());

    /** Mid-air cloud jump charge (server-side; resets on ground). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CloudJumpState>> CLOUD_JUMP =
        ATTACHMENT_TYPES.register(
            "cloud_jump",
            () -> AttachmentType.builder(() -> CloudJumpState.CHARGED)
                .serialize(CloudJumpState.MAP_CODEC)
                .copyOnDeath()
                .build());

    /**
     * Per-player active-ability cooldowns. Persisted across logout, synced to client for the
     * cooldown HUD. <b>Not</b> copyOnDeath — death resets all cooldowns to 0 (per spec).
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AbilityCooldowns>> ABILITY_COOLDOWNS =
        ATTACHMENT_TYPES.register(
            "ability_cooldowns",
            () -> AttachmentType.builder(AbilityCooldowns::empty)
                .serialize(AbilityCooldowns.CODEC)
                .sync(AbilityCooldowns.STREAM_CODEC)
                .build());

    /** Iron Word "Marked" debuff on a mob. Ephemeral — not serialized, not synced. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TauntMarkData>> TAUNT_MARK =
        ATTACHMENT_TYPES.register(
            "taunt_mark",
            () -> AttachmentType.builder(() -> TauntMarkData.EMPTY).build());

    /** Guardian's Vow link on the protected ally. Ephemeral — not serialized, not synced. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<GuardianVowData>> GUARDIAN_VOW =
        ATTACHMENT_TYPES.register(
            "guardian_vow",
            () -> AttachmentType.builder(() -> GuardianVowData.EMPTY).build());

    /**
     * What entity the player is currently disguised as (Master of Disguise tome). Ephemeral —
     * not serialized (any damage clears it) and not auto-synced; the server pushes changes to
     * tracking clients via {@link kingdom.smp.net.SyncDisguisePayload}.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DisguiseState>> DISGUISE =
        ATTACHMENT_TYPES.register(
            "disguise",
            () -> AttachmentType.builder(() -> DisguiseState.NONE).build());

    /** Skyrim-style use-to-level skill XP (Pickpocket, etc.). Persists across death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerUseSkills>> USE_SKILLS =
        ATTACHMENT_TYPES.register(
            "use_skills",
            () -> AttachmentType.builder(PlayerUseSkills::defaultData)
                .serialize(PlayerUseSkills.CODEC)
                .copyOnDeath()
                .build());

    /**
     * Per-player Stardew-style bonds with each named NPC (rapport, daily gift counter).
     * Survives death so the player keeps their friendships through respawns.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerNpcBonds>> NPC_BONDS =
        ATTACHMENT_TYPES.register(
            "npc_bonds",
            () -> AttachmentType.builder(PlayerNpcBonds::empty)
                .serialize(PlayerNpcBonds.CODEC)
                .copyOnDeath()
                .sync(PlayerNpcBonds.STREAM_CODEC)
                .build());

    /**
     * Per-player set of cooking-recipe ids the player has learned. Backs the knowledge half
     * of the cooking progression model (rank gate is read directly from {@link kingdom.smp.skill.SkillSavedData}).
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<KnownRecipes>> KNOWN_RECIPES =
        ATTACHMENT_TYPES.register(
            "known_recipes",
            () -> AttachmentType.builder(KnownRecipes::empty)
                .serialize(KnownRecipes.MAP_CODEC)
                .copyOnDeath()
                .build());

    /**
     * Last server-day (gameTime / 24000) the player claimed their Tallykeeper
     * daily reward. -1 = never. copyOnDeath so a death doesn't grant a second
     * payout the same day.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> TALLYKEEPER_LAST_CLAIM_DAY =
        ATTACHMENT_TYPES.register(
            "tallykeeper_last_claim_day",
            () -> AttachmentType.builder(() -> -1L)
                .serialize(com.mojang.serialization.Codec.LONG.fieldOf("day"))
                .copyOnDeath()
                .build());

    /**
     * Marks a naturally-generated chest's block entity as already counted by the
     * mimic-spawn system, so a chunk reloading never re-tallies (or re-converts) it.
     * Serialized into the chest's NBT; defaults to {@code false}.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> MIMIC_CHECKED =
        ATTACHMENT_TYPES.register(
            "mimic_checked",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                .serialize(com.mojang.serialization.Codec.BOOL.fieldOf("checked"))
                .build());

    /**
     * The class-stone pillar a player has summoned for a pending promotion (or
     * {@link ActivePromotionStone#NONE}). copyOnDeath so the stone they were sent
     * to find isn't lost on respawn. Not synced — used purely server-side.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ActivePromotionStone>> ACTIVE_PROMOTION_STONE =
        ATTACHMENT_TYPES.register(
            "active_promotion_stone",
            () -> AttachmentType.builder(() -> ActivePromotionStone.NONE)
                .serialize(ActivePromotionStone.CODEC)
                .copyOnDeath()
                .build());

    /**
     * Ticks a cow / mooshroom has accumulated inside the moon dimension. Once it crosses the
     * conversion threshold the animal turns into a Moonshroom (see
     * {@link kingdom.smp.moon.MoonAnimalConversionHandler}). Serialized so the countdown survives
     * chunk unloads and server restarts; defaults to 0. Server-only, so not synced.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> MOON_EXPOSURE_TICKS =
        ATTACHMENT_TYPES.register(
            "moon_exposure_ticks",
            () -> AttachmentType.builder(() -> 0)
                .serialize(com.mojang.serialization.Codec.INT.fieldOf("ticks"))
                .build());

    /** UUID string of the player who owns a locked chest, shelf, or armor stand. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> LOCK_OWNER =
        ATTACHMENT_TYPES.register(
            "lock_owner",
            () -> AttachmentType.builder(() -> "")
                .serialize(com.mojang.serialization.Codec.STRING.fieldOf("owner"))
                .build());

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
