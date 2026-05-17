package kingdom.smp.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kingdom.smp.ModAttachments;
import kingdom.smp.skill.useskill.PlayerUseSkills;
import kingdom.smp.skill.useskill.UseSkill;
import kingdom.smp.skill.useskill.UseSkillCurve;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /skills} — print the player's use-skill levels (Pickpocket, Sneak,
 * etc.) with XP into next level and a visual progress bar. Available to all
 * players (no permission gate — these are personal stats).
 */
public final class SkillsCommand {
    private SkillsCommand() {}

    private static final int BAR_WIDTH = 20;

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("skills")
                .executes(ctx -> showSkills(ctx.getSource()))
        );
    }

    private static int showSkills(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerUseSkills skills = player.getData(ModAttachments.USE_SKILLS.get());

        player.sendSystemMessage(Component.literal("§6§l═══ Your Skills ═══"));
        for (UseSkill skill : UseSkill.values()) {
            int level = skills.levelFor(skill);
            float totalXp = skills.xpFor(skill);
            float xpIntoLevel = UseSkillCurve.xpIntoLevel(totalXp);
            float xpForNext = UseSkillCurve.xpForNext(level);

            String bar = makeBar(xpIntoLevel, xpForNext);
            String xpDisplay = level >= UseSkill.MAX_LEVEL
                ? "§a§lMAX"
                : "§f" + Math.round(xpIntoLevel) + "§7/§f" + Math.round(xpForNext) + " §7XP";

            player.sendSystemMessage(Component.literal(
                "§e" + skill.displayName()
                + " §8— §7Lv. §a" + level
                + " §8(§r" + xpDisplay + "§8) "
                + bar));
        }
        return 1;
    }

    private static String makeBar(float current, float total) {
        if (total <= 0) return "§a[" + "█".repeat(BAR_WIDTH) + "§a]";
        float pct = Math.min(1f, current / total);
        int filled = Math.round(BAR_WIDTH * pct);
        return "§8[§a" + "█".repeat(filled) + "§8" + "░".repeat(BAR_WIDTH - filled) + "§8]";
    }
}
