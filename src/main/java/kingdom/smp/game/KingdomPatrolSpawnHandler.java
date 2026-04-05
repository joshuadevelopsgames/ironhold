package kingdom.smp.game;

import kingdom.smp.Ironhold;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Rare overworld spawns get a small kingdom-themed buff (HP, label, cheap weapon) — “patrol” flavor without new mob types.
 */
public final class KingdomPatrolSpawnHandler {
    private static final float CHANCE = 0.04f;
    private static final Identifier HEALTH_MOD =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "patrol_toughness");

    private static final String[] TITLES = {
        "Iron Hold Skirmisher",
        "Verdant Court Raider",
        "Ember Throne Enforcer",
        "Abyssal Pact Stalker",
    };

    private KingdomPatrolSpawnHandler() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (mob.level().isClientSide()) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!isWorldishSpawn(event.getSpawnType())) {
            return;
        }
        if (!mob.getType().builtInRegistryHolder().is(KingdomEntityTags.PATROL_BUFF_CANDIDATES)) {
            return;
        }
        RandomSource rand = mob.getRandom();
        if (rand.nextFloat() > CHANCE) {
            return;
        }
        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.addPermanentModifier(
                new AttributeModifier(HEALTH_MOD, 6.0, AttributeModifier.Operation.ADD_VALUE));
        }
        mob.setHealth(mob.getMaxHealth());
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.08f);
        mob.setCustomName(Component.literal(TITLES[rand.nextInt(TITLES.length)]));
        mob.setCustomNameVisible(true);
        // If you use Notable Bubble Text, set BubbleOwner in config to match and drive /bubbletext from commands/macros.
        mob.getPersistentData().putString("BubbleOwner", mob.getStringUUID());
    }

    private static boolean isWorldishSpawn(EntitySpawnReason reason) {
        return reason == EntitySpawnReason.NATURAL
            || reason == EntitySpawnReason.CHUNK_GENERATION
            || reason == EntitySpawnReason.PATROL
            || reason == EntitySpawnReason.REINFORCEMENT;
    }
}
