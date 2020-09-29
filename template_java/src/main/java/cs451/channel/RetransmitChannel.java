package cs451.channel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * RetransmitChannel
 */
public class RetransmitChannel extends Channel {

    public RetransmitChannel(Channel chan, boolean allowOoo) {
        this.chan = chan;
        this.chan.onReceive(this::doReceive);
        this.allowOoo = allowOoo;

        rtxThread = new Thread(this::retransmitLoop, "Retransmit thread");
        // rtxThread.setDaemon(true);
        rtxThread.start();
    }

    private void doReceive(byte[] data) {
        ChannelUtils.ByteReader rdr = ChannelUtils.reader(data);
        byte type = rdr.readByte();
        int seqNum = rdr.readInt();

        if (type == PACKET_TYPE_DATA) {
            // received a new packet
            byte[] payload = rdr.readEnd();
            sendAck(PACKET_TYPE_ACK, seqNum); // ack the packet anyway
            if (seqNum == recvSeqNum) {
                // this is a packet we are waiting for
                recvSeqNum++;
                deliver(payload);
                // go through all packets and try to deliver
                while (recvPackets.containsKey(recvSeqNum)) {
                    Packet deliverable = recvPackets.remove(recvSeqNum);
                    recvSeqNum++;
                    if (!allowOoo) {
                        deliver(deliverable.data);
                    }
                }
            } else if (seqNum > recvSeqNum && !recvPackets.containsKey(seqNum)) {
                // this is a future packet, add it to map
                // System.out.printf(" [rtx] got %d, but expecting %d\n", seqNum, recvSeqNum);
                sendAck(PACKET_TYPE_RTX_REQ, recvSeqNum); // request the one we are waiting for
                if (allowOoo) {
                    deliver(payload);
                }
                recvPackets.put(seqNum, new Packet(seqNum, allowOoo ? null : payload));
            }
        } else if (type == PACKET_TYPE_ACK) {
            // a packet is acknowledged, remove it from backlog
            // sendAck(PACKET_TYPE_ACK2, seqNum);
            if (sentPackets.containsKey(seqNum)) {
                synchronized (sentPackets) {
                    sentPackets.remove(seqNum);
                }
            }
            // } else if (type == PACKET_TYPE_ACK2) {

        } else if (type == PACKET_TYPE_RTX_REQ) {
            Packet p = sentPackets.get(seqNum);
            if (p != null)
                send(p.sequenceNumber, p.data);
        } else {
            System.err.println(" [rtx] unknown packet type " + type);
        }
    }

    private void sendAck(byte ackType, int seqNum) {
        byte[] packet = ChannelUtils.writer().writeByte(ackType).writeInt(seqNum).done();
        // System.out.println(" [rtx] sending ack " + ackType + " " +
        // ChannelUtils.toStr(packet));
        chan.send(packet);
    }

    public void send(byte[] data) {
        int curSeqNum = sequenceNumber++;
        synchronized (sentPackets) {
            sentPackets.put(curSeqNum, new Packet(curSeqNum, data));
        }
        send(curSeqNum, data);
    }

    private void send(int seqNumber, byte[] data) {
        byte[] packet = ChannelUtils.writer().writeByte((byte) 0).writeInt(seqNumber).writeBytes(data).done();
        chan.send(packet);
    }

    public void close() throws IOException {
        rtxThread.interrupt();
        try {
            rtxThread.join();
        } catch (InterruptedException e) {
            return;
        }
    }

    Channel chan;
    int sequenceNumber = 0;
    int recvSeqNum = 0;
    boolean allowOoo;

    Map<Integer, Packet> sentPackets = new HashMap<>();
    Map<Integer, Packet> recvPackets = new HashMap<>();

    static final byte PACKET_TYPE_DATA = 0; // 00 [seq:int] [data...]
    static final byte PACKET_TYPE_ACK = 1; // 01 [seq:int]
    static final byte PACKET_TYPE_ACK2 = 2; // 02 [seq:int]
    static final byte PACKET_TYPE_RTX_REQ = 3; // 03 [seq:int]

    static final long RTX_CYCLE = 100; // 100ms

    volatile boolean running = true;

    private void retransmitLoop() {
        while (running) {
            // if (!running) {
            // System.out.println("remaining " + sentPackets.size() + " packets");
            // }
            synchronized (sentPackets) {
                sentPackets.values().forEach(p -> {
                    // System.out.printf(" [rtx] retransmitting %d\n", p.sequenceNumber);
                    send(p.sequenceNumber, p.data);
                });
            }
            try {
                Thread.sleep(RTX_CYCLE);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public void cleanup() {
        running = false;
        rtxThread.interrupt();
        try {
            rtxThread.join();
        } catch (InterruptedException e) {
        }
        chan.cleanup();
    }

    Thread rtxThread;

    private static class Packet {
        byte[] data;
        int sequenceNumber;

        Packet(int s, byte[] d) {
            data = d;
            sequenceNumber = s;
        }
    }
}