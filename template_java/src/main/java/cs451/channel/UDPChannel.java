package cs451.channel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Consumer;

import cs451.Host;

/**
 * UDPChannel
 */
public class UDPChannel extends Channel {

    public static final double PACKET_DROP_RATE = 0.1;

    public UDPChannel(UdpHost host, Host target) {
        try {
            peer = InetAddress.getByName(target.getIp());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        peerPort = target.getPort();
        this.host = host;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void send(byte[] data) {
        if (PACKET_DROP_RATE > Math.random())
            return;
        DatagramPacket pkt = new DatagramPacket(data, data.length, peer, peerPort);
        host.send(pkt);
    }

    boolean isReceiver(DatagramPacket pkt) {
        return pkt.getPort() == peerPort && pkt.getAddress().equals(peer);
    }

    void doReceive(byte[] pkt) {
        deliver(pkt);
    }

    InetAddress peer;
    int peerPort;
    UdpHost host;

    Consumer<byte[]> consumer;
}