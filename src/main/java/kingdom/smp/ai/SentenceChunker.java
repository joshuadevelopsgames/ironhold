package kingdom.smp.ai;

import java.util.function.Consumer;

/**
 * Accumulates streamed LLM text deltas and emits speakable chunks at sentence
 * boundaries, so TTS can start on the first sentence while the model is still
 * writing the rest.
 *
 * <p>Flush rules, tuned for spoken NPC dialogue:
 * <ul>
 *   <li>A boundary is a terminator ({@code . ! ? …}) followed by whitespace —
 *       requiring the whitespace to already be present avoids splitting
 *       decimals ("3.5") and lets "..." / "?!" runs complete first.</li>
 *   <li>No flush while inside an unclosed {@code (...)}, {@code [...]} or
 *       {@code *...*} span — those are stage directions that
 *       {@code sanitizeForSpeech} strips as a pair; splitting one in half
 *       would leak asterisk text into the voice line.</li>
 *   <li>Each flush is at least {@code minFlushLen} chars, batching micro
 *       sentences ("Aye.") into the following one instead of paying a whole
 *       TTS round-trip for them.</li>
 *   <li>{@link #finish()} flushes whatever remains.</li>
 * </ul>
 *
 * <p>Thread-safe; deltas typically arrive on an HTTP executor thread.
 */
public final class SentenceChunker {

    private final StringBuilder pending = new StringBuilder();
    private final Consumer<String> onSentence;
    private final int minFlushLen;
    private boolean finished;

    public SentenceChunker(int minFlushLen, Consumer<String> onSentence) {
        this.minFlushLen = minFlushLen;
        this.onSentence = onSentence;
    }

    /** Append a streamed delta and flush any newly-completed sentences. */
    public synchronized void accept(String delta) {
        if (finished || delta == null || delta.isEmpty()) return;
        pending.append(delta);

        // Find the furthest flushable boundary in one scan, then cut once —
        // batching multiple short sentences into a single TTS call is a
        // feature, not a bug.
        int flushUpTo = -1;
        int bracketDepth = 0;
        boolean inStarSpan = false;
        for (int i = 0; i < pending.length() - 1; i++) {
            char c = pending.charAt(i);
            switch (c) {
                case '(', '[' -> bracketDepth++;
                case ')', ']' -> bracketDepth = Math.max(0, bracketDepth - 1);
                case '*' -> inStarSpan = !inStarSpan;
                default -> { }
            }
            if ((c == '.' || c == '!' || c == '?' || c == '…')
                && bracketDepth == 0 && !inStarSpan
                && Character.isWhitespace(pending.charAt(i + 1))
                && i + 1 >= minFlushLen) {
                flushUpTo = i + 1;
            }
        }
        if (flushUpTo > 0) {
            String out = pending.substring(0, flushUpTo).trim();
            pending.delete(0, flushUpTo);
            if (!out.isEmpty()) onSentence.accept(out);
        }
    }

    /** Flush the trailing remainder. Further {@link #accept} calls are ignored. */
    public synchronized void finish() {
        if (finished) return;
        finished = true;
        String out = pending.toString().trim();
        pending.setLength(0);
        if (!out.isEmpty()) onSentence.accept(out);
    }
}
