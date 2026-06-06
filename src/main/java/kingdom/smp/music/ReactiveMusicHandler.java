package kingdom.smp.music;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * Game-bus glue for Ironhold's reactive-music engine. Registered on
 * {@link net.neoforged.neoforge.common.NeoForge#EVENT_BUS} from {@code IronholdClient}
 * (client dist only).
 *
 * <ul>
 *   <li>{@link ClientTickEvent.Post} — advances the PvP duel state and recomputes the
 *       active {@link MusicTrigger}s.</li>
 *   <li>{@link AttackEntityEvent} — records the local player's outgoing hits on other
 *       players (incoming hits are inferred client-side in {@link PvpEscalation#tick}).</li>
 *   <li>{@link SelectMusicEvent} — overrides vanilla's situational music with the
 *       {@link ReactiveSongbook} selection when one applies.</li>
 * </ul>
 */
public final class ReactiveMusicHandler {
    private ReactiveMusicHandler() {}

    /** Master switch for the whole subsystem. */
    public static boolean enabled = true;

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        PvpEscalation.INSTANCE.tick(mc.player, now);   // update tier first
        ReactiveMusicState.INSTANCE.refresh(mc, now);  // then read it
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!enabled) return;
        if (event.getEntity() instanceof LocalPlayer self
                && event.getTarget() instanceof Player target
                && target != self) {
            PvpEscalation.INSTANCE.recordBlow(PvpEscalation.Side.SELF, target, self.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        if (!enabled) return;
        ReactiveSongbook.Result result = ReactiveSongbook.select(ReactiveMusicState.INSTANCE);
        switch (result.action()) {
            case PLAY -> event.overrideMusic(result.music()); // set + cancel: take precedence
            case SILENCE -> event.overrideMusic(null);        // cancel any playing music
            case PASS -> { /* leave selection to vanilla / other mods */ }
        }
    }
}
