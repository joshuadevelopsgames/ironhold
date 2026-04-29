package kingdom.smp.rtf.concurrent.cache;

public interface SafeCloseable extends AutoCloseable {
    void close();
}
