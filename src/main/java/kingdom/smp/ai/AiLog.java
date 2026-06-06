package kingdom.smp.ai;

/** Helpers for logging AI provider responses without leaking account/quota detail. */
public final class AiLog {
    private AiLog() {}

    private static final int MAX = 300;

    /** Truncate a response body to a safe length for logging. Error bodies from
     *  these providers can echo request/account context, so never log them whole. */
    public static String snippet(String body) {
        if (body == null) return "<null>";
        body = body.strip();
        if (body.length() <= MAX) return body;
        return body.substring(0, MAX) + "…(" + body.length() + " chars total)";
    }
}
