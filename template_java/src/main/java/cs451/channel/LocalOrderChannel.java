package cs451.channel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;

public class LocalOrderChannel extends Channel {

    public LocalOrderChannel(Channel chan) {
        this.chan = chan;
        chan.onReceive(this::doReceive);
    }

    @Override
    public void cleanup() {
        chan.cleanup();
    }

    @Override
    public void close() throws IOException {
        chan.close();
    }

    public void onReceiveDependant(BiConsumer<Integer, byte[]> consumer) {
        depConsumer = consumer;
        onReceive(data -> {
            final var br = ChannelUtils.reader(data);
            final var uuid = br.readInt();
            final var payload = br.readEnd();

            depConsumer.accept(uuid, payload);
        });
    }

    @Override
    public void send(byte[] data) {
        sendDependent(data, Collections.emptyList());
    }

    public int sendDependent(byte[] payload, List<Integer> deps) {
        final var uuid = random.nextInt();
        // System.out.println(" " + uuid + " depends on " + deps.size() + " things");
        var wr = ChannelUtils.writer();
        wr.writeInt(deps.size());
        for (int dep : deps) {
            wr.writeInt(dep);
        }
        wr.writeInt(uuid);
        wr.writeBytes(payload);

        chan.send(wr.done());

        return uuid;
    }

    private void doReceive(byte[] data) {
        final var br = ChannelUtils.reader(data);
        final int depCount = br.readInt();
        final int[] deps = new int[depCount];
        for (int i = 0; i < depCount; i++) {
            deps[i] = br.readInt();
        }

        int unmet = 0;

        final var pkt = new Packet(br.readEnd());

        for (int i = 0; i < depCount; i++) {
            if (!received.contains(deps[i])) {
                addWaiting(deps[i], pkt);
                pkt.increment();
                unmet++;
            }
        }

        if (unmet == 0) {
            doDelivery(pkt);
        }
    }

    private void doDelivery(Packet pkt) {
        received.add(pkt.uuid);
        deliver(pkt.data);

        if (waiting.containsKey(pkt.uuid)) {
            for (Packet depPkt : waiting.get(pkt.uuid)) {
                if (depPkt.decrementAndCheck()) {
                    doDelivery(depPkt);
                }
            }
        }
    }

    private void addWaiting(int dep, Packet pkt) {
        waiting.putIfAbsent(dep, new ArrayList<>()).add(pkt);
    }

    private final Set<Integer> received = new HashSet<>();
    private final Map<Integer, List<Packet>> waiting = new HashMap<>();
    private final Random random = new Random(System.nanoTime());

    private final Channel chan;
    private BiConsumer<Integer, byte[]> depConsumer;

    private static final class Packet {
        Packet(byte[] data) {
            this.uuid = ChannelUtils.reader(data).readInt();
            this.data = data;
        }

        boolean decrementAndCheck() {
            return --waiting <= 0;
        }

        void increment() {
            ++waiting;
        }

        private final int uuid;
        private final byte[] data;

        private int waiting = 0;
    }

}
