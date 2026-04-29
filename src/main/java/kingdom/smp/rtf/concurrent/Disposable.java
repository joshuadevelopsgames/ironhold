package kingdom.smp.rtf.concurrent;

public interface Disposable {
    void dispose();
    
    public interface Listener<T> {
        void onDispose(T ctx);
    }
}
