package kingdom.smp.rpg.ability;

import kingdom.smp.rpg.PlayerClass;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Static catalog of ability instances + per-class slot mapping.
 * Slot index 0..3 maps to keybinds Z/X/C/V respectively.
 *
 * Knight kit (per the implementation plan):
 *   slot 0 (Z): Shield Wall      [spec TBD — not yet registered]
 *   slot 1 (X): Vanguard Charge  [spec TBD — not yet registered]
 *   slot 2 (C): Iron Ward        ✓
 *   slot 3 (V): Guardian's Vow   ✓
 */
public final class AbilityRegistry {
    public static final int SLOT_COUNT = 4;

    public static final Ability IRON_WARD = new IronWard();
    public static final Ability GUARDIANS_VOW = new GuardiansVow();
    public static final Ability MEND = new Mend();
    public static final Ability SANCTUARY = new Sanctuary();
    public static final Ability CLEANSE = new Cleanse();

    private static final Map<Identifier, Ability> BY_ID = new HashMap<>();
    private static final Map<PlayerClass, Ability[]> BY_CLASS = new HashMap<>();

    static {
        registerSlot(PlayerClass.KNIGHT, 2, IRON_WARD);
        registerSlot(PlayerClass.KNIGHT, 3, GUARDIANS_VOW);
        // Healer kits (Phase 5 ④): give the support classes a real party role.
        registerSlot(PlayerClass.MEDIC, 0, MEND);
        registerSlot(PlayerClass.CLERIC, 0, MEND);
        registerSlot(PlayerClass.CLERIC, 2, CLEANSE);
        registerSlot(PlayerClass.SAINT, 1, SANCTUARY);
        registerSlot(PlayerClass.BISHOP, 1, SANCTUARY);
        registerSlot(PlayerClass.BISHOP, 2, CLEANSE);
    }

    private AbilityRegistry() {}

    private static void registerSlot(PlayerClass clazz, int slot, Ability ability) {
        BY_ID.put(ability.id(), ability);
        BY_CLASS.computeIfAbsent(clazz, c -> new Ability[SLOT_COUNT])[slot] = ability;
    }

    /** Returns the ability bound to {@code slot} for {@code clazz}, or {@code null} if the slot is empty. */
    public static Ability forSlot(PlayerClass clazz, int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return null;
        }
        Ability[] kit = BY_CLASS.get(clazz);
        return kit == null ? null : kit[slot];
    }

    public static Ability byId(Identifier id) {
        return BY_ID.get(id);
    }

    /** Returns true if any slot for {@code clazz} would be unlocked at the given level. */
    public static boolean anyUnlockedFor(PlayerClass clazz, int classLevel) {
        Ability[] kit = BY_CLASS.get(clazz);
        if (kit == null) return false;
        for (Ability a : kit) {
            if (a != null && classLevel >= a.unlockLevel()) {
                return true;
            }
        }
        return false;
    }
}
