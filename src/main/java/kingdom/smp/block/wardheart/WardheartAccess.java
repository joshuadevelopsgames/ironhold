package kingdom.smp.block.wardheart;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum WardheartAccess implements StringRepresentable {
    EVERYONE        ("everyone"),
    OWNER_ONLY      ("owner_only"),
    MOBS_BLOCKED    ("mobs_blocked");

    private final String name;
    WardheartAccess(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }

    public Component displayName() {
        return Component.translatable("ironhold.wardheart.access." + name);
    }

    public WardheartAccess next() {
        WardheartAccess[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static WardheartAccess byName(String n) {
        if (n == null) return MOBS_BLOCKED;
        for (WardheartAccess a : values()) if (a.name.equals(n)) return a;
        return MOBS_BLOCKED;
    }
}
