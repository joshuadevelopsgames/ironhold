package kingdom.smp.skill;

import kingdom.smp.net.SyncUseSkillsPayload;
import kingdom.smp.skill.useskill.UseSkill;
import kingdom.smp.skill.useskill.UseSkillCurve;

import java.util.EnumMap;
import java.util.Map;

/**
 * Client-side mirror of the player's use-to-level skill XP. Updated by
 * {@link kingdom.smp.net.ModNetworking}'s S2C handler whenever the server pushes a
 * {@link SyncUseSkillsPayload}. Read by the Practice tab of
 * {@link kingdom.smp.client.screen.SkillTreeScreen}.
 *
 * Single static field, since there's only one local player on the client at a time.
 */
public final class ClientUseSkillData {
    private ClientUseSkillData() {}

    private static Map<UseSkill, Float> xp = new EnumMap<>(UseSkill.class);
    private static boolean received = false;

    public static void receive(SyncUseSkillsPayload payload) {
        xp = new EnumMap<>(payload.xp());
        received = true;
    }

    public static boolean hasReceived() { return received; }

    public static float xpFor(UseSkill skill) {
        return xp.getOrDefault(skill, 0f);
    }

    public static int levelFor(UseSkill skill) {
        return Math.min(skill.maxLevel(), UseSkillCurve.levelFor(xpFor(skill)));
    }

    /** Progress 0..1 toward the next level (1.0 if at the per-skill cap). */
    public static float progressFor(UseSkill skill) {
        int level = levelFor(skill);
        if (level >= skill.maxLevel()) return 1f;
        float into = UseSkillCurve.xpIntoLevel(xpFor(skill));
        float span = UseSkillCurve.xpForNext(level);
        return span <= 0f ? 0f : Math.max(0f, Math.min(1f, into / span));
    }
}
