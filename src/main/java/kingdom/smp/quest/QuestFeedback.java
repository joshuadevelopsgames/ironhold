package kingdom.smp.quest;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * On-accept / on-complete / on-fail player feedback: a title + subtitle, a UI sound, and a
 * particle flourish around the player. Deliberately no screen shake or camera kick (per the
 * project's combat-feedback preference). Reuses vanilla UI sounds so no new assets are needed.
 */
public final class QuestFeedback {
    private QuestFeedback() {}

    private static final int GOLD = 0xFFD700;
    private static final int BLUE = 0x55CCFF;
    private static final int RED  = 0xFF5555;

    public static void accepted(ServerPlayer player, QuestDef def) {
        title(player,
                Component.literal("Quest Accepted").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(GOLD)).withBold(true)),
                Component.literal(def.title()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))));
        sound(player, SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 1.2f);
        particles(player, ParticleTypes.HAPPY_VILLAGER, 24);
        player.sendSystemMessage(line("✦ Quest accepted: ", def.title(), GOLD)
                .append(Component.literal("  (" + def.durationSeconds() / 60 + " min)")
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA)))));
    }

    public static void completed(ServerPlayer player, QuestDef def) {
        title(player,
                Component.literal("Objectives Complete").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(BLUE)).withBold(true)),
                Component.literal("Return to redeem your reward").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xCCCCCC))));
        sound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
        particles(player, ParticleTypes.HAPPY_VILLAGER, 30);
    }

    public static void redeemed(ServerPlayer player, QuestDef def) {
        title(player,
                Component.literal("Quest Complete").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(GOLD)).withBold(true)),
                Component.literal(def.title()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))));
        sound(player, SoundEvents.PLAYER_LEVELUP, 0.8f, 1.0f);
        particles(player, ParticleTypes.TOTEM_OF_UNDYING, 40);
    }

    public static void failed(ServerPlayer player, QuestDef def) {
        title(player,
                Component.literal("Quest Failed").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(RED)).withBold(true)),
                Component.literal(def.title() + " — out of time").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xCCCCCC))));
        sound(player, SoundEvents.VILLAGER_NO, 0.7f, 0.8f);
        particles(player, ParticleTypes.SMOKE, 20);
        player.sendSystemMessage(line("✗ Quest failed: ", def.title(), RED));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static MutableComponent line(String prefix, String name, int color) {
        return Component.literal(prefix).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)))
                .append(Component.literal(name).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(true)));
    }

    private static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private static void sound(ServerPlayer player, SoundEvent sound, float vol, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, vol, pitch);
    }

    private static void particles(ServerPlayer player, ParticleOptions particle, int count) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(particle, player.getX(), player.getY() + 1.0, player.getZ(),
                    count, 0.5, 0.6, 0.5, 0.02);
        }
    }
}
