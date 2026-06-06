package kingdom.smp.disguise;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import kingdom.smp.ModAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Plays the disguise mob's ambient sounds (a cow's moo, a zombie's groan, etc.) at the same
 * random intervals the real mob would, from the disguised player's position. Driven server-side
 * so every nearby player hears it. Movement/step sounds already come from the player's own
 * footsteps (those are block-based, identical for any entity), so only ambient sounds are added.
 */
public final class DisguiseSoundHandler {
    private DisguiseSoundHandler() {}

    /** Server-side, never-ticked stand-ins used purely to fetch each type's ambient sound. */
    private static final Map<EntityType<?>, Entity> DUMMIES = new HashMap<>();

    /** Vanilla's per-mob ambient-sound countdown, tracked per disguised player. */
    private static final Map<UUID, Integer> AMBIENT_TIME = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        DisguiseState state = player.getData(ModAttachments.DISGUISE.get());
        if (!state.active() || state.entityTypeId().isEmpty()) {
            AMBIENT_TIME.remove(player.getUUID());
            return;
        }

        EntityType<?> type = EntityType.byString(state.entityTypeId().get().toString()).orElse(null);
        if (type == null || !(dummyFor(type, level) instanceof Mob mob)) {
            return;
        }

        // Replicates Mob#baseTick: the countdown climbs each tick and, once it beats a random
        // roll, fires the ambient sound and resets by the mob's own interval.
        int time = AMBIENT_TIME.getOrDefault(player.getUUID(), 0);
        if (player.getRandom().nextInt(1000) < time) {
            mob.setPos(player.getX(), player.getY(), player.getZ());
            mob.playAmbientSound();
            AMBIENT_TIME.put(player.getUUID(), -mob.getAmbientSoundInterval());
        } else {
            AMBIENT_TIME.put(player.getUUID(), time + 1);
        }
    }

    private static Entity dummyFor(EntityType<?> type, ServerLevel level) {
        Entity cached = DUMMIES.get(type);
        if (cached != null && cached.level() == level) {
            return cached;
        }
        Entity created = type.create(level, EntitySpawnReason.LOAD);
        if (created != null) {
            DUMMIES.put(type, created);
        }
        return created;
    }
}
