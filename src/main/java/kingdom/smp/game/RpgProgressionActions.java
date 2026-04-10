package kingdom.smp.game;

import kingdom.smp.ModAttachments;
import kingdom.smp.net.ModNetworking;
import kingdom.smp.net.OpenClassSelectionPayload;
import kingdom.smp.rpg.CompletedClasses;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import kingdom.smp.rpg.RpgProgression;
import kingdom.smp.world.KingdomWorldData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side apply class XP + kingdom pool + immediate sync (shared by commands and gameplay). */
public final class RpgProgressionActions {
    private RpgProgressionActions() {}

    /** @return RPG data after grant (unchanged if {@code amount <= 0}). */
    public static PlayerKingdomRpgData grantClassXp(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return player.getData(ModAttachments.PLAYER_RPG.get());
        }
        PlayerKingdomRpgData cur = player.getData(ModAttachments.PLAYER_RPG.get());
        int oldLevel = cur.classLevel();
        PlayerKingdomRpgData leveled = RpgProgression.addClassXp(cur, amount);
        player.setData(ModAttachments.PLAYER_RPG.get(), leveled);
        KingdomWorldData world = overworldData(player);
        world.addKingdomXp(leveled.kingdomIndexClamped(), amount);
        ClassStatHandler.apply(player, leveled);
        RpgXpBarSync.sync(player, leveled);
        ModNetworking.syncToClient(player);

        if (leveled.classLevel() > oldLevel) {
            celebrateLevelUp(player, leveled, oldLevel);
            checkPromotion(player, leveled, oldLevel);
        }

        return leveled;
    }

    // ── Level-up celebration ─────────────────────────────────────────────────

    private static void celebrateLevelUp(ServerPlayer player, PlayerKingdomRpgData rpg, int oldLevel) {
        int newLevel = rpg.classLevel();
        PlayerClass pc = rpg.playerClass();
        int levelsGained = newLevel - oldLevel;
        boolean isMilestone = newLevel % 5 == 0;

        // ── Title text for the player ────────────────────────────────────────
        int color = classColorRgb(pc);
        Component title = Component.literal("\u2B50 Level Up! \u2B50")
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFD700)).withBold(true));
        Component subtitle = Component.literal(pc.id() + " Level " + newLevel)
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));

        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 15));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));

        // ── Sounds ───────────────────────────────────────────────────────────
        ServerLevel level = (ServerLevel) player.level();
        // Primary fanfare
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
            SoundSource.PLAYERS, 1.0f, 1.0f);
        // Layered sparkle
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
            SoundSource.PLAYERS, 0.6f, 1.4f);

        if (isMilestone) {
            // Extra epic sound for milestone levels (5, 10, 15, ...)
            level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        // ── Particles ────────────────────────────────────────────────────────
        double px = player.getX();
        double py = player.getY() + 1.0;
        double pz = player.getZ();

        // Totem-like burst around the player
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, px, py, pz,
            30 + levelsGained * 10, 0.6, 1.0, 0.6, 0.3);
        // Ring of happy villager particles
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py + 0.5, pz,
            15, 1.0, 0.5, 1.0, 0.1);

        if (isMilestone) {
            // Extra firework burst for milestone
            level.sendParticles(ParticleTypes.FIREWORK, px, py + 1.5, pz,
                50, 0.8, 1.2, 0.8, 0.4);
            level.sendParticles(ParticleTypes.END_ROD, px, py, pz,
                25, 0.5, 1.5, 0.5, 0.05);
        }

        // ── Chat messages ────────────────────────────────────────────────────
        var server = player.level().getServer();
        if (server == null) return;

        String name = player.getName().getString();
        if (isMilestone) {
            // Broadcast milestone to the whole server
            server.getPlayerList().broadcastSystemMessage(
                Component.empty()
                    .append(Component.literal("\u2728 ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFD700))))
                    .append(Component.literal(name).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(true)))
                    .append(Component.literal(" has reached ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xCCCCCC))))
                    .append(Component.literal(pc.id() + " Level " + newLevel).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFD700)).withBold(true)))
                    .append(Component.literal("!").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xCCCCCC))))
                    .append(Component.literal(" \u2728").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFD700)))),
                false);
        } else {
            // Personal chat message for regular levels
            player.sendSystemMessage(
                Component.empty()
                    .append(Component.literal("\u2191 ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55))))
                    .append(Component.literal(pc.id()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))))
                    .append(Component.literal(" Level " + newLevel).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFD700)).withBold(true)))
                    .append(levelsGained > 1
                        ? Component.literal(" (+" + levelsGained + " levels!)").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55)))
                        : Component.literal(" reached!").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA)))));
        }
    }

    // ── Promotion check ───────────────────────────────────────────────────

    private static void checkPromotion(ServerPlayer player, PlayerKingdomRpgData rpg, int oldLevel) {
        PlayerClass pc = rpg.playerClass();
        int tier = pc.tier();
        if (tier >= 4) return; // Tier 4 is max

        if (!RpgProgression.justReachedPromotion(tier, oldLevel, rpg.classLevel())) return;

        // Mark current class as completed
        CompletedClasses completed = player.getData(ModAttachments.COMPLETED_CLASSES.get());
        CompletedClasses updated = completed.withCompleted(pc);
        player.setData(ModAttachments.COMPLETED_CLASSES.get(), updated);

        // Check if any next-tier class is actually available for this player
        int nextTier = tier + 1;
        boolean hasOptions = false;
        for (PlayerClass candidate : PlayerClass.values()) {
            if (candidate.tier() == nextTier && candidate.canUnlock(updated.asSet())) {
                hasOptions = true;
                break;
            }
        }

        if (!hasOptions) {
            // No available promotions (e.g. hybrids need two paths) — just notify
            player.sendSystemMessage(
                Component.literal("\u00A7eMastered " + pc.id() + "! Complete another path to unlock hybrid classes."));
            return;
        }

        // Promotion title
        int color = classColorRgb(pc);
        Component title = Component.literal("\u2694 Class Mastered! \u2694")
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF)).withBold(true));
        Component subtitle = Component.literal("Choose your next path...")
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xCCCCCC)));

        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));

        // Extra epic promotion sound
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
            SoundSource.PLAYERS, 1.2f, 0.8f);

        // Open the class selection screen
        PacketDistributor.sendToPlayer(player, new OpenClassSelectionPayload());
    }

    private static int classColorRgb(PlayerClass pc) {
        return switch (pc) {
            case KNIGHT  -> 0xCCCCCC;
            case RANGER  -> 0x55AA55;
            case WIZARD  -> 0x7755FF;
            case CLERIC  -> 0xFFDD55;
            case ROGUE   -> 0xAA3333;
            case PEASANT -> 0x888888;
            default      -> 0xFFFFFF;
        };
    }

    private static KingdomWorldData overworldData(ServerPlayer player) {
        ServerLevel here = (ServerLevel) player.level();
        ServerLevel ow = here.getServer().getLevel(Level.OVERWORLD);
        return ow.getDataStorage().computeIfAbsent(KingdomWorldData.TYPE);
    }
}
