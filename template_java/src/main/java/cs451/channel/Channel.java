package cs451.channel;

import java.io.Closeable;
import java.util.function.Consumer;

/**
 * Channel
 */
public abstract class Channel implements Closeable {
    public abstract void send(byte[] data);

    public final void onReceive(Consumer<byte[]> c) {
        if (consumer != null && c != null)
            throw new IllegalStateException("consumer already set");
        consumer = c;
    }

    protected final void deliver(byte[] data) {
        Consumer<byte[]> cons = this.consumer;
        if (cons == null) {
            System.err.println(" [udp] no receiver !!");
        } else {
            cons.accept(data);
        }
    }

    public void cleanup() {
    }

    private Consumer<byte[]> consumer;
}