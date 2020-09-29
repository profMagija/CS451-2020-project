package cs451.channel;

import java.util.Set;

import cs451.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.io.IOException;

public class UniformReliableBroadcast extends Channel {

    private int seqNum = 0;
    private final int myId;
    private final Channel beb;
    private final FailureDetector fd;

    private final Set<Integer> correct = new HashSet<>();
    private final Set<Pair<Integer, Integer>> delivered = new HashSet<>();
    private final Map<Pair<Integer, Integer>, byte[]> forward = new HashMap<>();

    private final Map<Pair<Integer, Integer>, Set<Integer>> ack = new HashMap<>();

    private static final byte PKT_TYPE_DATA = 0;
    private static final byte PKT_TYPE_PING = 1;

    public UniformReliableBroadcast(final int myId, final int numProc, final Channel beb) {
        this.myId = myId;
        this.beb = beb;
        this.fd = new FailureDetector(myId, this::sendPing, this::onCrash);

        for (int i = 0; i < numProc; i++) {
            correct.add(i + 1);
        }

        this.beb.onReceive(this::doReceive);
    }

    @Override
    public void send(byte[] data) {
        final var curSeq = seqNum++;
        forward.put(new Pair<>(this.myId, curSeq), data);
        this.beb.send(ChannelUtils.writer().writeByte(PKT_TYPE_DATA).writeInt(myId).writeInt(myId).writeInt(curSeq)
                .writeBytes(data).done());
    }

    @Override
    public void close() throws IOException {
        this.beb.close();
    }

    private void sendPing() {
        this.beb.send(ChannelUtils.writer().writeByte(PKT_TYPE_PING).writeInt(myId).done());
    }

    private void onCrash(Integer process) {
        correct.remove(process);

        for (var kvp : forward.entrySet()) {
            checkDelivery(kvp.getKey().getKey(), kvp.getKey().getValue(), kvp.getValue());
        }
    }

    private void doReceive(byte[] data) {
        final var reader = ChannelUtils.reader(data);
        final var packetType = reader.readByte();
        final var sender = reader.readInt();
        fd.stillAlive(sender);

        if (packetType != PKT_TYPE_DATA)
            return;

        var origSender = reader.readInt();
        var seqNum = reader.readInt();
        var payload = reader.readEnd();

        var msgId = new Pair<>(origSender, seqNum);

        ack.compute(msgId, (key, acks) -> {
            if (acks == null)
                acks = new HashSet<>();

            acks.add(sender);
            return acks;
        });

        if (!forward.containsKey(msgId)) {
            forward.put(msgId, payload);
            this.beb.send(ChannelUtils.writer().writeByte(PKT_TYPE_DATA).writeInt(myId).writeInt(origSender)
                    .writeInt(seqNum).writeBytes(payload).done());
        }

        checkDelivery(origSender, seqNum, payload);
    }

    private void checkDelivery(final int sender, final int seqNum, final byte[] payload) {
        var msgId = new Pair<>(sender, seqNum);

        if (delivered.contains(msgId))
            return;

        var acks = ack.get(msgId);
        if (acks == null)
            return;

        for (var cp : correct) {
            if (!acks.contains(cp))
                return;
        }

        forward.remove(msgId); // we can now remove it

        delivered.add(msgId);
        this.deliver(payload);
    }

}
