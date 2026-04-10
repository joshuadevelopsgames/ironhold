package kingdom.smp;

import kingdom.smp.accessory.AccessoryInventory;
import kingdom.smp.game.CloudJumpState;
import kingdom.smp.rpg.CompletedClasses;
import kingdom.smp.rpg.PlayerKingdomRpgData;
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

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
