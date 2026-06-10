package kingdom.smp.command;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.Command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /testbook} — gives a written book with a hand-test checklist for every Fantasia-port feature
 * built so far (Phases 1–6). Registered to the game bus in {@code Ironhold}.
 */
public final class TestBookCommand {
    private TestBookCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("testbook").requires(src -> kingdom.smp.perms.Perms.check(
                    src, kingdom.smp.perms.ModPermissions.COMMAND_ADMIN))
                .executes(ctx -> give(ctx.getSource())));
    }

    private static int give(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            return 0;
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();
        for (String page : PAGES) {
            // The book screen wasn't honoring legacy §-codes, so strip them and set black directly on
            // the component's STYLE (the font always respects an explicit style color).
            String plain = page.replaceAll("§.", "");
            pages.add(Filterable.passThrough(
                Component.literal(plain).withStyle(s -> s.withColor(0x000000))));
        }
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,
            new WrittenBookContent(Filterable.passThrough("Ironhold Test Guide"), "Ironhold", 0, pages, false));
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        src.sendSuccess(() -> Component.literal("Gave the Ironhold Test Guide."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static final String[] PAGES = {
        "§l§nIronhold Test Guide§r\n\nA checklist for the new systems (Phases 1-6).\n\nMost items are in the creative §oIronhold§r tab. Use /op + creative.\n\nTurn keepInventory §lOFF§r to test Soulbound & the Shrine.",

        "§l⚔ Parry §r[key R]\n\nTap R to parry (~0.25s window).\n\nA hit inside it is negated, the attacker staggers, projectiles reflect, and your ability cooldowns refund.\n\nMiss/early = ~1.2s lockout. Try a skeleton: §otime§r the arrow, don't spam.",

        "§l🌀 Dodge §r[Left-Alt]\n\nShort hop in your move direction (no input = backhop).\n\nPerfect-dodge: you are invulnerable §lonly§r if you dodge just before a hit lands. A late dodge just repositions.\n\nCooldown ~1.5s.",

        "§l✦ Soulbound§r\n\nRun: /enchant @s ironhold:soulbound\non a held sword. (Also drops in loot.)\n\nWith keepInventory OFF, die.\nThe Soulbound item returns on respawn with a little wear; everything else drops.",

        "§l⟡ Ender Shrine §r(1/2)\n\nCraft an §lEnder Totem§r:\nTotem of Undying + Ender Pearl.\n\nCraft an §lEnder Shrine§r:\na ring of crying obsidian around an Ender Eye, over a Totem of Undying.",

        "§l⟡ Ender Shrine §r(2/2)\n\nRight-click the shrine (empty hand) to §lbind§r it as home.\n\nUse Ender Totems on it to add charges (max 5).\n\nWith §lno totem§r in hand, die in the same dimension -> you revive at the shrine, healed.",

        "§l◉ Coin Purse§r\n\nGet a Coin Purse + Gold Coins.\n\nRight-click: bank all loose coins.\nSneak + right-click: withdraw 64.\n\nHover the purse to see its balance.",

        "§l♛ Boss Artifacts §r(1/2)\n\nKill the King Enderman -> Ender Regalia.\nKill the Stone Golem -> Stoneblood Amulet.\n(First kill only - not farmable.)\n\nOpen your Equipment screen and slot the artifact.",

        "§l♛ Boss Artifacts §r(2/2)\n\nStoneblood Amulet: -15% damage; immune to knockback & slowness.\n\nEnder Regalia: endermen ignore you and can't hurt you. Press §lG§r to blink forward.",

        "§l✶ Gear Affixes §r(1/2)\n\nHold a weapon / armor / tool.\n/affix roll  -> rolls affixes.\nCount = quality tier (Good 2, Mint 3).\n/affix clear -> removes them.\n\nCheck the tooltip for the affix list.",

        "§l✶ Affixes §r(2/2)\n\nExamples: Keen +dmg, Leeching lifesteal, Serrated bleed, Stalwart +armor, Warded -dmg, Thorns reflect.\n\n§lReforge:§r sneak + right-click Master Tobias holding affixed gear -> reroll for coins (needs Blacksmithing Novice).",

        "§l✚ Healer Kits§r\n\nBe a Medic/Cleric/Saint/Bishop.\nAbilities on Z / X / C:\n  Mend - heal an ally\n  Sanctuary - AoE regen\n  Cleanse - strip debuffs\n\nStand by a §llit campfire§r, out of combat -> slow heal.",

        "§l🎒 Kits & Diet§r\n\nPromote at a Class Stone -> you receive that class's starter kit (Squire/Knight/Mage/Archer...).\n\n§lDiet:§r eat 3+ §odifferent§r food groups (meat/grain/veg/fruit/sweet) -> 'Well Fed' (Health Boost). 5 groups adds Regen.",

        "§l⌛ End Gate§r\n\nBuild an End Portal frame. BEFORE beating the King Enderman AND Stone Golem, an Eye of Ender will §lnot§r seat in a frame.\n\nBeat both bosses, then the eyes seat normally.",

        "§l⏳ Not Yet In-Game§r\n\nDeferred (need assets / world-building):\n- Villager marry/hire/recruit\n- Bard music\n- Auroras & night sky\n- Boss arenas & story dimension\n- Affix lock-and-reroll GUI\n\nDon't test for these yet."
    };
}
