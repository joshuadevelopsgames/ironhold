package kingdom.smp.entity;

import net.minecraft.util.RandomSource;

/**
 * Core temperament archetype for Kingdom Villagers.
 * Drives behavioral AI, emote frequency, and (for talkers) dialogue tone.
 */
public enum VillagerTemperament {
    CHEERFUL("cheerful",
        "Upbeat and warm. Quick to laugh, generous with gifts, hums while working.",
        0.7f, 0.8f, 0.9f),
    GRUMPY("grumpy",
        "Irritable and blunt. Scowls at strangers, works aggressively, slow to trust.",
        0.3f, 0.4f, 0.3f),
    NERVOUS("nervous",
        "Anxious and skittish. Startles easily, avoids conflict, fidgets constantly.",
        0.6f, 0.2f, 0.5f),
    STOIC("stoic",
        "Calm and measured. Rarely emotes, moves deliberately, unshakeable in crisis.",
        0.5f, 0.8f, 0.4f),
    ECCENTRIC("eccentric",
        "Unpredictable and quirky. Stares at random things, sudden mood swings, fascinated by the mundane.",
        0.5f, 0.5f, 0.7f);

    private final String id;
    private final String llmDescription;
    private final float baseFriendliness;
    private final float baseBoldness;
    private final float baseEnergy;

    VillagerTemperament(String id, String llmDescription,
                        float baseFriendliness, float baseBoldness, float baseEnergy) {
        this.id = id;
        this.llmDescription = llmDescription;
        this.baseFriendliness = baseFriendliness;
        this.baseBoldness = baseBoldness;
        this.baseEnergy = baseEnergy;
    }

    public String id() { return id; }
    public String llmDescription() { return llmDescription; }
    public float baseFriendliness() { return baseFriendliness; }
    public float baseBoldness() { return baseBoldness; }
    public float baseEnergy() { return baseEnergy; }

    public static VillagerTemperament random(RandomSource rand) {
        VillagerTemperament[] values = values();
        return values[rand.nextInt(values.length)];
    }

    public static VillagerTemperament fromId(String id) {
        for (VillagerTemperament t : values()) {
            if (t.id.equals(id)) return t;
        }
        return STOIC;
    }
}
