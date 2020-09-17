package cs451.channel;

import java.util.Set;

import cs451.Pair;

import java.util.HashSet;
import java.util.List;

import java.io.IOException;

public class UniformReliableBroadcast extends Channel {

    public UniformReliableBroadcast(final int myId, final List<Pair<Integer, Channel>> peers) {
        this.peers = peers;
        this.delivered = new HashSet<>();
        this.myId = myId;

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
        final int curSeqNum = sequenceNumber++;
        final byte[] packet = ChannelUtils.writer().writeInt(myId).writeInt(curSeqNum).writeBytes(data).done();
        peers.forEach(p -> p.getValue().send(packet));
        deliver(data);
        delivered.add(new Pair<>(myId, curSeqNum));
    }

    @Override
    public void cleanup() {
        peers.forEach(p -> p.getValue().cleanup());
    }

    private void doReceive(final byte[] data) {
        ChannelUtils.ByteReader br = ChannelUtils.reader(data);
        int sender = br.readInt();
        int seqNum = br.readInt();
        byte[] payload = br.readEnd();

        // System.out.println(" [urb] received " + sender + " " + seqNum);

        var pkt = new Pair<>(sender, seqNum);

        if (delivered.contains(pkt))
            return; // already delivered
        delivered.add(pkt);

        deliver(payload);
        peers.forEach(p -> {
            if (p.getKey() != sender) {
                p.getValue().send(data);
            }
        });
    }

    private final List<Pair<Integer, Channel>> peers;
    private final Set<Pair<Integer, Integer>> delivered;
    private final int myId;

    int sequenceNumber = 0;
}
