package cs451.channel;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import cs451.Pair;

public class BestEffortBroadcast extends Channel {

    private final List<Pair<Integer, Channel>> peers;

    public BestEffortBroadcast(final int myId, final List<Pair<Integer, Channel>> peers) {
        this.peers = peers;

        for (final var peer : peers) {
            peer.getValue().onReceive(this::doReceive);
        }
    }

    @Override
    public void close() throws IOException {
        for (final var peer : peers) {
            peer.getValue().close();
        }
    }

    @Override
    public void send(final byte[] data) {
        for (final var peer : peers) {
            peer.getValue().send(data);
        }
    }

    private void doReceive(byte[] data) {
        deliver(data);
    }
}
