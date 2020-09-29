package cs451.channel;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class FailureDetector implements Closeable {

    private final int myId;

    private final double ITERATION_TIME = 0.2;
    private final int MAX_TIMEOUT = 20;

    private final Map<Integer, Integer> lastTimeSeen = new HashMap<>();

    private final int currentIteration = 0;
    private final Runnable sendPing;
    private final Consumer<Integer> onCrash;

    private final Thread pingThread;

    public FailureDetector(final int myId, final Runnable sendPing, final Consumer<Integer> onCrash) {
        this.myId = myId;
        this.sendPing = sendPing;
        this.onCrash = onCrash;

        this.pingThread = new Thread(this::runner);
        this.pingThread.setDaemon(true);
        this.pingThread.start();
    }

    public void stillAlive(final int pid) {
        synchronized (lastTimeSeen) {
            lastTimeSeen.compute(pid, (key, cur) -> currentIteration);
        }
    }

    public void runner() {
        try {
            while (true) {
                sendPing.run();

                Set<Entry<Integer, Integer>> procs;
                synchronized (lastTimeSeen) {
                    procs = lastTimeSeen.entrySet();
                }

                for (var kvp : procs) {
                    if (kvp.getValue() < currentIteration - MAX_TIMEOUT) {
                        onCrash.accept(kvp.getKey());
                        lastTimeSeen.remove(kvp.getKey());
                    }
                }

                Thread.sleep((int) (ITERATION_TIME * 1000));
            }
        } catch (final InterruptedException ex) {
            // nothing
        }
    }

    @Override
    public void close() throws IOException {
        this.pingThread.interrupt();
        this.pingThread.stop();
    }

    public void cleanup() throws IOException {
        this.pingThread.interrupt();
        this.pingThread.join();
    }
}
