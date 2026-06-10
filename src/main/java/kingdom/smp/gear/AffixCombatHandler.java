package kingdom.smp.gear;

import kingdom.smp.ModEffects;
import kingdom.smp.effect.KillBurstEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Affix combat effects. Offensive (weapon) affixes modify outgoing damage + fire on-hit effects;
 * defensive (armor) affixes modify incoming damage + reflect. Registered to the game bus in
 * {@code Ironhold}. Spec: {@code specs/fantasia-ports/07-gear-affixes.md}.
 *
 * <p>Voltaic ships as a recursion-safe chain <i>stun</i> this increment. Prospector/Scholar/Enduring
 * are deferred (need break-speed / xp / durability hooks).
 */
public final class AffixCombatHandler {
    private AffixCombatHandler() {}

    private static final EquipmentSlot[] ARMOR_SLOTS =
        { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    /** Guards Thorns reflect from recursing (player-vs-player both wearing Thorns). */
    private static boolean reflecting = false;

    @SubscribeEvent
    public static void onHit(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();

        // ── Offensive: attacker is a player with an affixed weapon ──
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (!AffixData.get(weapon).isEmpty()) {
                float base = event.getAmount();
                float amount = base;
                if (AffixData.has(weapon, Affix.SAVAGE) && isFamilyTarget(victim)) {
                    amount += base * AffixData.rollOf(weapon, Affix.SAVAGE);
                }
                if (AffixData.has(weapon, Affix.PIERCING)) {
                    double armor = victim.getAttributeValue(Attributes.ARMOR);
                    amount += (float) (AffixData.rollOf(weapon, Affix.PIERCING) * armor * 0.5);
                }
                if (AffixData.has(weapon, Affix.BRUTAL)
                        && attacker.getRandom().nextFloat() < AffixData.rollOf(weapon, Affix.BRUTAL)) {
                    amount += base * 0.5f;
                }
                if (amount != base) {
                    event.setAmount(amount);
                }
                applyOnHit(attacker, victim, weapon, event.getAmount());
            }
        }

        // ── Defensive: victim is a player with armor affixes ──
        if (victim instanceof ServerPlayer defender) {
            float reduce = 0f;
            float reflect = 0f;
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack piece = defender.getItemBySlot(slot);
                reduce += AffixData.rollOf(piece, Affix.WARDED);
                reflect += AffixData.rollOf(piece, Affix.THORNS);
            }
            if (reduce > 0f) {
                event.setAmount(event.getAmount() * Math.max(0f, 1f - reduce));
            }
            if (reflect > 0f && !reflecting
                    && event.getSource().getEntity() instanceof LivingEntity src && src != defender
                    && defender.level() instanceof ServerLevel level) {
                float reflectDmg = event.getAmount() * reflect;
                if (reflectDmg > 0f) {
                    reflecting = true;
                    try {
                        src.hurtServer(level, level.damageSources().thorns(defender), reflectDmg);
                    } finally {
                        reflecting = false;
                    }
                }
            }
        }
    }

    private static void applyOnHit(ServerPlayer attacker, LivingEntity victim, ItemStack weapon, float amount) {
        if (AffixData.has(weapon, Affix.LEECHING)) {
            attacker.heal(amount * AffixData.rollOf(weapon, Affix.LEECHING));
        }
        if (AffixData.has(weapon, Affix.SERRATED)
                && attacker.getRandom().nextFloat() < AffixData.rollOf(weapon, Affix.SERRATED)) {
            victim.addEffect(new MobEffectInstance(ModEffects.BLEEDING_EFFECT, 100, 0));
        }
        if (AffixData.has(weapon, Affix.CONCUSSIVE)
                && attacker.getRandom().nextFloat() < AffixData.rollOf(weapon, Affix.CONCUSSIVE)) {
            victim.knockback(0.6, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
        }
        if (AffixData.has(weapon, Affix.VOLTAIC)
                && attacker.getRandom().nextFloat() < AffixData.rollOf(weapon, Affix.VOLTAIC)) {
            voltaicChain(attacker, victim);
        }
    }

    private static boolean isFamilyTarget(LivingEntity victim) {
        var holder = victim.getType().builtInRegistryHolder();
        return holder.is(EntityTypeTags.UNDEAD)
            || holder.is(EntityTypeTags.ARTHROPOD)
            || holder.is(EntityTypeTags.ILLAGER);
    }

    private static void voltaicChain(ServerPlayer attacker, LivingEntity victim) {
        if (!(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        AABB box = victim.getBoundingBox().inflate(4.0);
        int hits = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == victim || e == attacker || e.isAlliedTo(attacker)) {
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getEyeY(), e.getZ(), 8, 0.2, 0.3, 0.2, 0.1);
            if (++hits >= 2) {
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        if (!AffixData.has(killer.getMainHandItem(), Affix.SOULRENDING)) {
            return;
        }
        if (event.getEntity().level() instanceof ServerLevel level) {
            KillBurstEffect.spawn(level, event.getEntity(), 5);
        }
    }
}
