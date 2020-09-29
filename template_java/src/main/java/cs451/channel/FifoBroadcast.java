package cs451.channel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cs451.Pair;

public class FifoBroadcast extends Channel {

    private final int myId;
    private final Channel urb;

    private final Map<Pair<Integer, Integer>, byte[]> recv = new HashMap<>();
    private final int[] expectedSeqNum;

    private int seqNum = 0;

    public FifoBroadcast(final int myId, final int numProc, final Channel urb) {
        this.myId = myId;
        this.urb = urb;

        this.urb.onReceive(this::doReceive);
        expectedSeqNum = new int[numProc];

    }

    @Override
    public void close() throws IOException {
        urb.close();
    }

    @Override
    public void send(final byte[] data) {
        final var curSeqNum = seqNum++;
        this.urb.send(ChannelUtils.writer().writeInt(myId).writeInt(curSeqNum).writeBytes(data).done());
    }

    private void doReceive(byte[] data) {
        final var reader = ChannelUtils.reader(data);
        final var sender = reader.readInt();
        final var seqNum = reader.readInt();
        final var payload = reader.readEnd();

        if (seqNum == expectedSeqNum[sender - 1]) {
            // this is the packet we are waiting for
            expectedSeqNum[sender - 1]++;
            deliver(payload);

            // go through the backlog
            var msgId = new Pair<>(sender, expectedSeqNum[sender - 1]);
            while (recv.containsKey(msgId)) {
                expectedSeqNum[sender - 1]++;
                deliver(recv.remove(msgId));
                msgId = new Pair<>(sender, expectedSeqNum[sender - 1]);
            }
        } else {
            recv.put(new Pair<>(sender, seqNum), payload);
        }
    }
}
