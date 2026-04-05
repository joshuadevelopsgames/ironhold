package kingdom.smp.rpg;

import java.util.List;

/** Catalog of class skills for UI; gameplay wiring comes later. */
public final class ClassSkills {
    private ClassSkills() {}

    public static List<ClassSkillEntry> profileEntries(PlayerClass c) {
        return switch (c) {
            case PEASANT -> List.of(
                    new ClassSkillEntry("Hearth Sense", "Passive", "Slightly better foraging luck.", "Rank I — stub"),
                    new ClassSkillEntry("Call to Arms", "Active", "Opens class selection when ready.", "Use /k2 classgui"),
                    new ClassSkillEntry("—", "—", "Promote to a real class to unlock paths.", "—"));
            case KNIGHT -> List.of(
                    new ClassSkillEntry("Iron Will", "Passive", "Tankier; slower movement (class stats).", "Active"),
                    new ClassSkillEntry("Heavy Plate Training", "Passive", "Carry & melee focus.", "Active"),
                    new ClassSkillEntry("Shield Wall", "Active", "Mitigate damage for allies nearby.", "Not bound — soon"),
                    new ClassSkillEntry("Cavalry Charge", "Active", "Burst move + knockback.", "Not bound — soon"));
            case RANGER -> List.of(
                    new ClassSkillEntry("Tracker", "Passive", "Move speed; ranged bias (class stats).", "Active"),
                    new ClassSkillEntry("True Shot", "Passive", "Steadier projectile damage (planned).", "Stub"),
                    new ClassSkillEntry("Volley Mark", "Active", "Mark target; bonus team damage.", "Not bound — soon"),
                    new ClassSkillEntry("Beast Lure", "Active", "Draw mob attention briefly.", "Not bound — soon"));
            case WIZARD -> List.of(
                    new ClassSkillEntry("Arcane Sight", "Passive", "See others class/level (planned).", "Stub"),
                    new ClassSkillEntry("Mana Surge", "Passive", "Spell damage window (planned).", "Stub"),
                    new ClassSkillEntry("Blink", "Active", "Short teleport; respect blocks.", "Not bound — soon"),
                    new ClassSkillEntry("Nova", "Active", "AoE burst; long cooldown.", "Not bound — soon"));
            case CLERIC -> List.of(
                    new ClassSkillEntry("Mend", "Active", "Heal ally burst.", "Not bound — soon"),
                    new ClassSkillEntry("Sanctuary", "Active", "Zone: reduce hostile damage.", "Not bound — soon"),
                    new ClassSkillEntry("Cleanse", "Active", "Strip one harmful effect.", "Not bound — soon"),
                    new ClassSkillEntry("Resilience", "Passive", "Healing intake up (planned).", "Stub"));
            case ROGUE -> List.of(
                    new ClassSkillEntry("Shadow Step", "Active", "Short blink from stealth.", "Not bound — soon"),
                    new ClassSkillEntry("Assassinate", "Active", "Backstab bonus damage (planned).", "Stub"),
                    new ClassSkillEntry("Pick Pocket", "Active", "Bonus loot from mobs (planned).", "Stub"),
                    new ClassSkillEntry("Evasion", "Passive", "Move & atk speed (class stats).", "Active"));
        };
    }
}
