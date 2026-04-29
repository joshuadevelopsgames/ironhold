package kingdom.smp.rtf.feature.template.buffer;

public interface BufferIterator {
    boolean isEmpty();

    boolean next();

    int nextIndex();
}