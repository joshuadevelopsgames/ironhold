package kingdom.smp.mixin;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.EnumSet;
import java.util.List;

/**
 * Exposes mutable accessors for {@link ClientboundPlayerInfoUpdatePacket}'s
 * private {@code actions} and {@code entries} fields. The vanilla packet has
 * no public constructor that accepts arbitrary {@code Entry} lists — both
 * public constructors derive entries from real {@link net.minecraft.server.level.ServerPlayer}s.
 *
 * <p>Ironhold needs to announce synthetic Kangarude/Kangabrine player-list
 * entries (no real player exists for them), so we instantiate the packet with
 * an empty player collection and overwrite both fields via this accessor.
 */
@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface PlayerInfoUpdatePacketAccessor {
    @Mutable
    @Accessor("actions")
    void ironhold$setActions(EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions);

    @Mutable
    @Accessor("entries")
    void ironhold$setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}
