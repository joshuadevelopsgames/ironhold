package kingdom.smp.rpg.ability;

import java.util.ArrayList;
import java.util.List;

import kingdom.smp.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/** Shared targeting for healer abilities — same-kingdom player allies within range. */
public final class HealSupport {
    private HealSupport() {}

    public static List<ServerPlayer> alliesInRange(ServerLevel level, ServerPlayer caster, double range,
                                                   boolean includeSelf) {
        int kingdom = caster.getData(ModAttachments.PLAYER_RPG.get()).kingdomIndexClamped();
        AABB box = caster.getBoundingBox().inflate(range);
        List<ServerPlayer> out = new ArrayList<>();
        if (includeSelf) {
            out.add(caster);
        }
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (p == caster || p.distanceToSqr(caster) > range * range) {
                continue;
            }
            if (p.getData(ModAttachments.PLAYER_RPG.get()).kingdomIndexClamped() == kingdom) {
                out.add(p);
            }
        }
        return out;
    }
}
