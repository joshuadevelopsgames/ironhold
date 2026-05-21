package kingdom.smp.skill.useskill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-player use-skill XP totals. Stored as a NeoForge attachment on
 * {@link net.minecraft.world.entity.player.Player}; see
 * {@link kingdom.smp.ModAttachments#USE_SKILLS}.
 */
public record PlayerUseSkills(Map<UseSkill, Float> xp) {

    public static final MapCodec<PlayerUseSkills> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.unboundedMap(UseSkill.CODEC, Codec.FLOAT)
            .optionalFieldOf("xp", Map.of())
            .forGetter(PlayerUseSkills::xp)
    ).apply(i, PlayerUseSkills::new));

    public PlayerUseSkills {
        xp = xp.isEmpty() ? new EnumMap<>(UseSkill.class) : new EnumMap<>(xp);
    }

    public static PlayerUseSkills defaultData() {
        return new PlayerUseSkills(new EnumMap<>(UseSkill.class));
    }

    public float xpFor(UseSkill skill) {
        return xp.getOrDefault(skill, 0f);
    }

    public int levelFor(UseSkill skill) {
        return Math.min(skill.maxLevel(), UseSkillCurve.levelFor(xpFor(skill)));
    }

    /** Returns a new instance with the given amount of XP added to the skill. */
    public PlayerUseSkills withAddedXp(UseSkill skill, float amount) {
        EnumMap<UseSkill, Float> next = new EnumMap<>(xp);
        next.merge(skill, amount, Float::sum);
        return new PlayerUseSkills(next);
    }
}
