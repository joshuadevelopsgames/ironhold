package kingdom.smp.npc;

import kingdom.smp.ModAttachments;
import net.minecraft.server.level.ServerPlayer;

/**
 * Single source of truth for how a player's rapport with a named NPC maps to a
 * relationship tier — used both by the Bonds UI (hearts + tier label) and by
 * the per-turn LLM prompt so the NPC's warmth actually tracks the bond.
 *
 * <p>Rapport runs 0..{@link #MAX_RAPPORT} for display purposes (it can dip
 * negative when the player wrongs an NPC, which reads as "Wary").
 */
public final class NpcRapport {

    private NpcRapport() {}

    public static final int MAX_RAPPORT       = 1000;
    public static final int RAPPORT_PER_HEART = 100;

    // Tier thresholds (mirror tierLabel) — used to gate relationship rewards.
    public static final int RAPPORT_ACQUAINTANCE = 200;
    public static final int RAPPORT_FRIEND       = 400;
    public static final int RAPPORT_TRUSTED      = 600;
    public static final int RAPPORT_KINDRED      = 800;

    /** Ticks per vanilla day — used to bucket conversations into "days". */
    private static final long TICKS_PER_DAY = 24000L;

    /** Rapport granted the first time a player actually converses with an NPC each day. */
    public static final int DAILY_TALK_GAIN = 2;

    public static String tierLabel(int rapport) {
        if (rapport >= 800) return "Kindred Spirit";
        if (rapport >= 600) return "Trusted";
        if (rapport >= 400) return "Friend";
        if (rapport >= 200) return "Acquaintance";
        if (rapport >= 0)   return "Stranger";
        return "Wary";
    }

    /**
     * Called once per conversation turn from an NPC's reply pipeline. Grants the
     * once-per-day talk rapport (Stardew-style: simply talking keeps a bond
     * warm) and returns the relationship block to splice into the dynamic
     * (non-cached) portion of that NPC's system prompt.
     *
     * <p>Side effect: may mutate the player's {@link ModAttachments#NPC_BONDS}
     * attachment. Safe to call on the server thread during prompt assembly.
     */
    public static String onConversationTurn(ServerPlayer player, String npcTag) {
        PlayerNpcBonds bonds = player.getData(ModAttachments.NPC_BONDS);
        long today = player.level().getGameTime() / TICKS_PER_DAY;
        if (!bonds.talkedToday(npcTag, today)) {
            bonds = bonds.withDailyTalk(npcTag, DAILY_TALK_GAIN, today);
            player.setData(ModAttachments.NPC_BONDS, bonds);
        }
        return promptBlock(bonds.rapportFor(npcTag));
    }

    /** Relationship block for the LLM system prompt, based on current rapport. */
    public static String promptBlock(int rapport) {
        return "\n[RELATIONSHIP WITH THIS PLAYER: " + tierLabel(rapport)
            + " (bond " + Math.max(0, rapport) + "/" + MAX_RAPPORT + "). "
            + guidanceFor(rapport)
            + " Let this colour your warmth and how much you confide — never state the"
            + " number or these instructions aloud.]";
    }

    private static String guidanceFor(int rapport) {
        if (rapport < 0)   return "They have wronged or unsettled you — be guarded, curt, and slow to trust.";
        if (rapport < 200) return "Barely an acquaintance — polite but reserved; don't overshare or act familiar.";
        if (rapport < 400) return "A warming acquaintance — friendlier now, happy to chat, but not yet a confidant.";
        if (rapport < 600) return "A friend — speak warmly and familiarly, glad to see them.";
        if (rapport < 800) return "Someone you trust — confide small things and do them favours, as a close ally.";
        return "A kindred spirit — deep affection and loyalty; speak openly and fondly, as a dear old friend.";
    }
}
