package kingdom.smp.game;

import kingdom.smp.Ironhold;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

/**
 * Terraria-style random death messages. Called by {@code ServerPlayerDeathMixin}
 * to replace the vanilla death message with a randomly selected one.
 *
 * The replacement masks the real cause-of-death from players, which makes
 * debugging impossible from chat alone — we log the underlying
 * {@link DamageSource} to the server console on every death so admins can grep
 * {@code latest.log} after the fact.
 */
public final class DeathMessageHandler {
    private DeathMessageHandler() {}

    private static final int GENERIC_COUNT = 70;
    private static final int KILLED_COUNT = 30;

    /**
     * Cause-specific flavor pools. When a death's cause maps to one of these,
     * we draw from its dedicated pool instead of the generic one. Each key
     * corresponds to lang entries {@code ironhold.death.<key>.<0..count-1>},
     * single-arg ({@code %s} = victim name).
     */
    private enum DeathCategory {
        FALL("fall", 5),
        CREEPER("creeper", 4),
        SKELETON("skeleton", 2),
        ENDERMAN("enderman", 3),
        FIRE("fire", 3),
        DROWN("drown", 2),
        STARVE("starve", 2),
        SUFFOCATE("suffocate", 1),
        VOID("void", 2),
        ANIMAL("animal", 3),
        PVP("pvp", 3),
        LIGHTNING("lightning", 1),
        SILVERFISH("silverfish", 1),
        WARDEN("warden", 1),
        ANVIL("anvil", 1);

        final String key;
        final int count;
        DeathCategory(String key, int count) { this.key = key; this.count = count; }
    }

    /**
     * Build a death message for the given player.
     * Called from the mixin that replaces {@code CombatTracker.getDeathMessage()}.
     *
     * Cause-specific pools take priority; anything uncategorized falls back to
     * the dry generic / killed-by pools.
     */
    public static Component buildDeathMessage(ServerPlayer player, DamageSource source, CombatTracker tracker) {
        Entity killer = source != null ? source.getEntity() : null;

        // Server-side breadcrumb so the real cause survives the flavor-message
        // replacement. Grep latest.log for "death-cause" to find any player death.
        logDeathCause(player, source, killer);

        String playerName = player.getDisplayName().getString();

        DeathCategory category = categorize(source, killer);
        if (category != null) {
            int index = player.getRandom().nextInt(category.count);
            return Component.translatable("ironhold.death." + category.key + "." + index, playerName);
        }

        if (killer != null && killer != player) {
            String killerName = killer.getDisplayName().getString();
            int index = player.getRandom().nextInt(KILLED_COUNT);
            return Component.translatable("ironhold.death.killed." + index, playerName, killerName);
        } else {
            int index = player.getRandom().nextInt(GENERIC_COUNT);
            return Component.translatable("ironhold.death." + index, playerName);
        }
    }

    /**
     * Map a death's cause to a flavor category, or null if it should use the
     * generic / killed-by pools. Environmental damage types are checked first,
     * then the killer entity type.
     */
    private static DeathCategory categorize(DamageSource source, Entity killer) {
        if (source != null) {
            if (source.is(DamageTypes.FALL)) return DeathCategory.FALL;
            if (source.is(DamageTypes.LAVA) || source.is(DamageTypes.IN_FIRE)
                    || source.is(DamageTypes.ON_FIRE)) return DeathCategory.FIRE;
            if (source.is(DamageTypes.DROWN)) return DeathCategory.DROWN;
            if (source.is(DamageTypes.STARVE)) return DeathCategory.STARVE;
            if (source.is(DamageTypes.IN_WALL)) return DeathCategory.SUFFOCATE;
            if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) return DeathCategory.VOID;
            if (source.is(DamageTypes.LIGHTNING_BOLT)) return DeathCategory.LIGHTNING;
            if (source.is(DamageTypes.FALLING_ANVIL)) return DeathCategory.ANVIL;
            if (source.is(DamageTypes.SONIC_BOOM)) return DeathCategory.WARDEN;
        }
        if (killer != null) {
            if (killer instanceof Player) return DeathCategory.PVP;
            EntityType<?> t = killer.getType();
            if (t == EntityType.CREEPER) return DeathCategory.CREEPER;
            if (t == EntityType.SKELETON || t == EntityType.STRAY || t == EntityType.BOGGED) return DeathCategory.SKELETON;
            if (t == EntityType.ENDERMAN) return DeathCategory.ENDERMAN;
            if (t == EntityType.WARDEN) return DeathCategory.WARDEN;
            if (t == EntityType.SILVERFISH) return DeathCategory.SILVERFISH;
            if (t == EntityType.WOLF || t == EntityType.LLAMA || t == EntityType.TRADER_LLAMA
                    || t == EntityType.CAT || t == EntityType.CHICKEN) return DeathCategory.ANIMAL;
        }
        return null;
    }

    private static void logDeathCause(ServerPlayer player, DamageSource source, Entity killer) {
        String typeId = "unknown";
        if (source != null) {
            try {
                typeId = source.typeHolder().unwrapKey()
                        .map(k -> k.identifier().toString())
                        .orElse(source.type().msgId());
            } catch (Throwable t) {
                typeId = "unknown";
            }
        }
        Entity direct = source != null ? source.getDirectEntity() : null;
        String directDesc = direct == null || direct == killer ? "" :
                " direct=" + direct.getType().getDescription().getString()
                        + "(" + direct.getDisplayName().getString() + ")";
        String killerDesc = killer == null ? "" :
                " killer=" + killer.getType().getDescription().getString()
                        + "(" + killer.getDisplayName().getString() + ")";
        Ironhold.LOGGER.info(
                "death-cause player={} type={} pos=({},{},{}) hp={}/{}{}{}",
                player.getName().getString(),
                typeId,
                (int) player.getX(), (int) player.getY(), (int) player.getZ(),
                player.getHealth(), player.getMaxHealth(),
                killerDesc, directDesc);
    }
}
