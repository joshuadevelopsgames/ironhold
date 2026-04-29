package kingdom.smp.rtf.concurrent.cache;

public interface ExpiringEntry {
    long getTimestamp();
    
    default void close() {
    }
}
