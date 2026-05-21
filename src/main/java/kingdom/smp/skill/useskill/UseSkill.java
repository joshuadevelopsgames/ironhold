package kingdom.smp.skill.useskill;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Skyrim-style "use-to-level" skills, distinct from the perk-point
 * {@link kingdom.smp.skill.Profession} trees. XP is earned passively from
 * using the skill, and the skill level itself drives mechanical effects
 * (success chance, damage, detection radius, etc.).
 *
 * <p>Levels 0–100. See {@link UseSkillCurve} for the XP→level curve.
 */
public enum UseSkill implements StringRepresentable {
    PICKPOCKET("pickpocket", "Pickpocket", 100),
    SNEAK("sneak", "Sneak", 100),
    FISHING("fishing", "Fishing", 50);

    /** Absolute ceiling — no per-skill cap may exceed this. */
    public static final int MAX_LEVEL = 100;

    public static final Codec<UseSkill> CODEC = StringRepresentable.fromEnum(UseSkill::values);
    public static final StreamCodec<ByteBuf, UseSkill> STREAM_CODEC =
        ByteBufCodecs.VAR_INT.map(UseSkill::byId, UseSkill::ordinal);

    private final String id;
    private final String displayName;
    private final int maxLevel;

    UseSkill(String id, String displayName, int maxLevel) {
        this.id = id;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
    }

    public String displayName() { return displayName; }

    /** Per-skill level cap. Fishing tops out lower than Pickpocket / Sneak. */
    public int maxLevel() { return maxLevel; }

    @Override
    public String getSerializedName() { return id; }

    public static UseSkill byId(int ordinal) {
        UseSkill[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
