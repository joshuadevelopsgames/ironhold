package kingdom.smp.quest;

import kingdom.smp.ModEntities;
import kingdom.smp.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The quest library — the static catalog of every {@link QuestDef}.
 *
 * <p>Built lazily on first access so item/entity holders ({@link ModItems}, {@link ModEntities})
 * are fully registered before we construct {@link ItemStack}s and read {@link EntityType}s.
 * Add new quests in {@link #build(Map)}; ids are permanent once shipped (they key the
 * skill-point reward milestone).
 */
public final class Quests {
    private Quests() {}

    private static final int MIN = 20 * 60;

    private static volatile Map<String, QuestDef> catalog;

    private static Map<String, QuestDef> catalog() {
        Map<String, QuestDef> local = catalog;
        if (local == null) {
            synchronized (Quests.class) {
                local = catalog;
                if (local == null) {
                    local = new LinkedHashMap<>();
                    build(local);
                    catalog = local;
                }
            }
        }
        return local;
    }

    public static QuestDef byId(String id) {
        return catalog().get(id);
    }

    public static List<QuestDef> all() {
        return new ArrayList<>(catalog().values());
    }

    /** All quests offered by the given NPC giver key, in catalog order. */
    public static List<QuestDef> byGiver(String giver) {
        List<QuestDef> out = new ArrayList<>();
        for (QuestDef def : catalog().values()) {
            if (def.giver().equals(giver)) out.add(def);
        }
        return out;
    }

    private static void add(Map<String, QuestDef> out, QuestDef def) {
        out.put(def.id(), def);
    }

    private static ItemStack stack(net.minecraft.world.item.Item item, int n) {
        return new ItemStack(item, n);
    }

    // ── The library ──────────────────────────────────────────────────────────
    private static void build(Map<String, QuestDef> out) {

        // —— Vanilla starters (quest board, no specific giver) ——
        add(out, new QuestDef(
                "hunt_the_pack", "board",
                "Hunt the Pack",
                List.of("Wolves prey on the herds.", "Cull them and bring proof."),
                List.of(
                        QuestObjective.collect(Items.RABBIT_HIDE, 5, "Rabbit Hide"),
                        QuestObjective.collect(Items.BONE, 3, "Bone")),
                QuestReward.of(1, 60, stack(Items.EMERALD, 4)),
                10 * MIN));

        add(out, new QuestDef(
                "nightwatch", "board",
                "Nightwatch",
                List.of("The dead walk after dusk.", "Thin their ranks."),
                List.of(QuestObjective.slay(EntityType.ZOMBIE, 12, Items.ROTTEN_FLESH, "Zombies slain")),
                QuestReward.of(0, 75, stack(Items.COOKED_BEEF, 8)),
                10 * MIN));

        add(out, new QuestDef(
                "spelunker", "board",
                "Spelunker",
                List.of("The realm needs ore.", "Dig deep and haul it up."),
                List.of(
                        QuestObjective.collect(Items.RAW_IRON, 12, "Raw Iron"),
                        QuestObjective.collect(Items.COAL, 16, "Coal")),
                QuestReward.of(1, 80, stack(Items.IRON_PICKAXE, 1)),
                20 * MIN));

        // —— Blacksmith Tobias (smithing line) ——
        add(out, new QuestDef(
                "tobias_steel_for_the_realm", "blacksmith_tobias",
                "Steel for the Realm",
                List.of("Tobias needs steel for", "the next batch of arms."),
                List.of(QuestObjective.deliver(ModItems.STEEL_INGOT.get(), 8, "Steel Ingots")),
                QuestReward.of(1, 120, stack(ModItems.GOLD_COIN.get(), 6)),
                20 * MIN));

        add(out, new QuestDef(
                "tobias_scrap_run", "blacksmith_tobias",
                "Scrap Run",
                List.of("Bring Tobias raw iron", "to keep the forge fed."),
                List.of(QuestObjective.collect(Items.IRON_INGOT, 16, "Iron Ingots")),
                QuestReward.of(0, 50, stack(ModItems.GOLD_COIN.get(), 8)),
                15 * MIN));

        // —— Captain Roselind (martial line) ——
        add(out, new QuestDef(
                "roselind_cull_the_filchers", "captain_roselind",
                "Cull the Filchers",
                List.of("Filchers plague the roads.", "Put them down."),
                List.of(QuestObjective.slay(ModEntities.FILCHER.get(), 10,
                        ModItems.FILCHER_CROWN.get(), "Filchers slain")),
                QuestReward.of(1, 150, stack(ModItems.GOLD_COIN.get(), 10)),
                15 * MIN));
    }
}
