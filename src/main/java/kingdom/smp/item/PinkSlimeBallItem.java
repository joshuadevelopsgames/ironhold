package kingdom.smp.item;

import java.util.List;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.accessory.AccessoryInventory;
import kingdom.smp.accessory.AccessoryItem;
import kingdom.smp.entity.SlimePetEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * Pink Slime Ball — an accessory that summons a head-pet companion (Je11ie or Cheakie) while
 * equipped. A per-second tick reconciles the number of live companions to the number of equipped
 * balls: one ball → one pet, two balls → two pets, etc. Pets are stateless and non-persistent, so
 * extras left behind by a death or teleport are trimmed (and vanish on chunk unload) rather than
 * duplicating. Companions are despawned when the ball is unequipped (handled in
 * {@code AccessoryTickHandler}).
 */
public class PinkSlimeBallItem extends AccessoryItem {

    public PinkSlimeBallItem(Properties props) {
        super(props);
    }

    @Override
    public void onAccessoryTick(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (player.tickCount % 20 != 0) return; // once per second
        // Only the first equipped ball drives spawning, so N balls produce exactly N pets.
        if (!isFirstBall(serverPlayer, stack)) return;

        ServerLevel level = (ServerLevel) serverPlayer.level();
        int desired = equippedBallCount(serverPlayer);
        List<? extends SlimePetEntity> pets = companionsOf(serverPlayer);

        // Trim any extras (e.g. a stray left behind by a teleport or death).
        for (int i = pets.size() - 1; i >= desired; i--) {
            pets.get(i).discard();
        }
        // Top up to the desired count.
        for (int i = pets.size(); i < desired; i++) {
            EntityType<? extends SlimePetEntity> type = level.getRandom().nextBoolean()
                ? kingdom.smp.ModEntities.SLIME_PET_JE11IE.get()
                : kingdom.smp.ModEntities.SLIME_PET_CHEAKIE.get();
            spawnPet(serverPlayer, level, type);
        }
    }

    /** True only for the lowest-slot equipped Pink Slime Ball, so reconciliation runs once. */
    private static boolean isFirstBall(ServerPlayer player, ItemStack stack) {
        AccessoryInventory inv = player.getData(ModAttachments.ACCESSORY_INV.get());
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            if (inv.getItem(i).getItem() instanceof PinkSlimeBallItem) {
                return inv.getItem(i) == stack;
            }
        }
        return false;
    }

    private static int equippedBallCount(ServerPlayer player) {
        AccessoryInventory inv = player.getData(ModAttachments.ACCESSORY_INV.get());
        int n = 0;
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            if (inv.getItem(i).getItem() instanceof PinkSlimeBallItem) n++;
        }
        return n;
    }

    /** Every live companion owned by this player, searched across the whole loaded level. */
    private static List<? extends SlimePetEntity> companionsOf(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        return level.getEntities(EntityTypeTest.forClass(SlimePetEntity.class),
            e -> e.isTame() && e.isOwnedBy(player));
    }

    /** Shatter the equipped Pink Slime Ball (called when a companion is killed). */
    public static void breakBall(ServerPlayer player) {
        AccessoryInventory inv = player.getData(ModAttachments.ACCESSORY_INV.get());
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            if (inv.getItem(i).getItem() instanceof PinkSlimeBallItem) {
                inv.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
                player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.SLIME_DEATH,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);
                removeAllCompanions(player);
                break;
            }
        }
    }

    /** Despawn every Slime Pet owned by this player (called on unequip / respawn). */
    public static void removeAllCompanions(ServerPlayer player) {
        for (SlimePetEntity pet : companionsOf(player)) {
            pet.discard();
        }
    }

    public static boolean isEquipped(Player player) {
        AccessoryInventory inv = player.getData(ModAttachments.ACCESSORY_INV.get());
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            if (inv.getItem(i).getItem() instanceof PinkSlimeBallItem) return true;
        }
        return false;
    }

    private void spawnPet(ServerPlayer player, ServerLevel level,
                          EntityType<? extends SlimePetEntity> type) {
        SlimePetEntity pet = type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (pet == null) return;
        pet.setPos(player.getX() + (level.getRandom().nextDouble() - 0.5),
            player.getY(),
            player.getZ() + (level.getRandom().nextDouble() - 0.5));
        pet.tame(player);
        pet.setOrderedToSit(false);
        level.addFreshEntity(pet);
    }

    @Override
    public List<Component> getAccessoryTooltip() {
        return List.of(
            Component.literal("  Summons Je11ie or Cheakie,").withStyle(ChatFormatting.LIGHT_PURPLE),
            Component.literal("  a slime-headed pet that bites").withStyle(ChatFormatting.GRAY),
            Component.literal("  foes and leaves them Slimed.").withStyle(ChatFormatting.GRAY),
            Component.literal("  Shatters if the pet is slain.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
