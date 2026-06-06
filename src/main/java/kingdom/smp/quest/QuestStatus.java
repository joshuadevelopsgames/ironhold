package kingdom.smp.quest;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Lifecycle of a quest a player has accepted. Absence of a {@link QuestProgress} means "not started". */
public enum QuestStatus implements StringRepresentable {
    /** Accepted; the timer is running and objectives are being tracked. */
    ACTIVE("active"),
    /** Every objective met before the deadline; awaiting redeem to pay out. */
    COMPLETE("complete"),
    /** Rewards collected. Terminal. */
    CLAIMED("claimed"),
    /** Deadline passed before completion. Terminal (re-offerable by the giver). */
    FAILED("failed");

    public static final Codec<QuestStatus> CODEC = StringRepresentable.fromEnum(QuestStatus::values);

    private final String id;

    QuestStatus(String id) { this.id = id; }

    @Override
    public String getSerializedName() { return id; }

    public boolean terminal() { return this == CLAIMED || this == FAILED; }
}
