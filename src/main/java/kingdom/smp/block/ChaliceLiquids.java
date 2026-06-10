package kingdom.smp.block;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import kingdom.smp.alcohol.AlcoholService;
import kingdom.smp.alcohol.AlcoholicDrinkItem;
import org.jspecify.annotations.Nullable;

/**
 * Maps a held container item onto the liquid the {@link ChaliceBlock} should hold,
 * and resolves what happens when a player drinks that liquid back out.
 *
 * <p>The chalice is a reusable vessel: pour a liquid in from its normal container
 * ({@link #fromHeld}) and {@code ChaliceRenderer} draws its surface inside the cup,
 * tinted by {@link Fill#rgb()}. Right-click empty-handed to drink it — each liquid
 * has a fitting effect ({@link #applyDrinkEffects}). Potions carry their full
 * {@link PotionContents} through so drinking applies the brew's real effects (and
 * its proper colour); a gulp of lava sets you ablaze until you die or reach water.
 */
public final class ChaliceLiquids {
    private ChaliceLiquids() {}

    /** Default water-bottle / empty-potion tint, matching vanilla's uncrafted potion colour. */
    private static final int POTION_DEFAULT_RGB = 0x385DC6;

    /** Lava burn long enough to be lethal from full health — i.e. "until dead or water". */
    private static final int LAVA_FIRE_TICKS = 20 * 600; // 10 min; vanilla water/rain still douses it

    /**
     * One pour's worth of state.
     *
     * @param id        stable liquid key (persisted + synced; drives tint + drink effect)
     * @param rgb       0xRRGGBB surface tint
     * @param alpha     surface opacity 0..1 (water/potion are slightly see-through)
     * @param emissive  render full-bright (lava glows in the dark)
     * @param container empty container handed back to the player (null = consumed)
     * @param sound     poured-in sound
     * @param potion    full potion contents when {@code id == "potion"}, else null
     */
    public record Fill(String id, int rgb, float alpha, boolean emissive,
                       @Nullable Item container, SoundEvent sound, @Nullable PotionContents potion) {}

    /** Resolve the liquid a held stack would pour in, or {@code null} if it isn't a container we accept. */
    @Nullable
    public static Fill fromHeld(ItemStack stack) {
        Item it = stack.getItem();
        if (it == Items.WATER_BUCKET) {
            return new Fill("water", 0x3F76E4, 0.80f, false, Items.BUCKET, SoundEvents.BUCKET_EMPTY, null);
        }
        if (it == Items.LAVA_BUCKET) {
            return new Fill("lava", 0xFF8A12, 1.0f, true, Items.BUCKET, SoundEvents.BUCKET_EMPTY_LAVA, null);
        }
        if (it == Items.MILK_BUCKET) {
            return new Fill("milk", 0xF7F4EC, 1.0f, false, Items.BUCKET, SoundEvents.BUCKET_EMPTY, null);
        }
        if (it == Items.POWDER_SNOW_BUCKET) {
            return new Fill("powder_snow", 0xE5EEF2, 0.95f, false, Items.BUCKET, SoundEvents.BUCKET_EMPTY_POWDER_SNOW, null);
        }
        if (it == Items.HONEY_BOTTLE) {
            return new Fill("honey", 0xF9A722, 1.0f, false, Items.GLASS_BOTTLE, SoundEvents.BOTTLE_EMPTY, null);
        }
        if (it instanceof AlcoholicDrinkItem drink) {
            return new Fill("alcohol:" + drink.drink().id(), drink.drink().color(),
                0.90f, false, Items.GLASS_BOTTLE, SoundEvents.BOTTLE_EMPTY, null);
        }
        if (it == Items.POTION || it == Items.SPLASH_POTION || it == Items.LINGERING_POTION) {
            PotionContents pc = stack.get(DataComponents.POTION_CONTENTS);
            int color = pc != null ? (pc.getColor() & 0xFFFFFF) : POTION_DEFAULT_RGB;
            return new Fill("potion", color, 0.80f, false, Items.GLASS_BOTTLE, SoundEvents.BOTTLE_EMPTY, pc);
        }
        return null;
    }

    /**
     * Apply the effect of drinking the chalice's current liquid to {@code player}
     * (server side). Mirrors how each liquid would behave if you actually swallowed it.
     */
    public static void applyDrinkEffects(ServerLevel level, Player player, String id,
                                         @Nullable PotionContents potion) {
        if (id.startsWith("alcohol:") && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            String drinkId = id.substring("alcohol:".length());
            for (AlcoholicDrinkItem.Drink drink : AlcoholicDrinkItem.Drink.values()) {
                if (drink.id().equals(drinkId)) {
                    AlcoholicDrinkItem.applyDrinkBenefit(serverPlayer, drink);
                    AlcoholService.drink(serverPlayer, drink);
                    return;
                }
            }
        }
        switch (id) {
            case "water" ->
                // A cool drink douses you — also the antidote to a chalice of lava.
                player.setRemainingFireTicks(0);
            case "lava" -> {
                // Burn until dead or until you reach water (vanilla water/rain clears fire).
                player.setRemainingFireTicks(LAVA_FIRE_TICKS);
                player.hurtServer(level, level.damageSources().lava(), 4.0f);
            }
            case "milk" ->
                player.removeAllEffects();
            case "honey" -> {
                player.removeEffect(MobEffects.POISON);
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2, 1)); // restores a little hunger
            }
            case "powder_snow" -> {
                // A mouthful of powdered snow chills you to the bone.
                player.setTicksFrozen(Math.max(player.getTicksFrozen(), 300)); // > freeze threshold → freeze damage
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 1));
            }
            case "potion" -> {
                if (potion != null) {
                    for (MobEffectInstance e : potion.getAllEffects()) {
                        player.addEffect(new MobEffectInstance(e));
                    }
                }
            }
            default -> { }
        }
    }

    /** Display name for the held liquid, used in messaging if needed. */
    public static String displayName(String id) {
        if (id.startsWith("alcohol:")) {
            return switch (id.substring("alcohol:".length())) {
                case "small_ale" -> "Small Ale";
                case "honey_mead" -> "Honey Mead";
                case "apple_cider" -> "Apple Cider";
                case "berry_wine" -> "Berry Wine";
                default -> "a Drink";
            };
        }
        return switch (id) {
            case "water"       -> "Water";
            case "lava"        -> "Lava";
            case "milk"        -> "Milk";
            case "powder_snow" -> "Powder Snow";
            case "honey"       -> "Honey";
            case "potion"      -> "a Potion";
            default            -> id;
        };
    }
}
