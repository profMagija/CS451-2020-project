package cs451.channel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;

import cs451.Host;

/**
 * UdpHost
 */
public class UdpHost {

    public UdpHost(Host me) {
        this.me = me;
        System.out.println(" created host " + me.getIp() + ":" + me.getPort());
        try {
            this.socket = new DatagramSocket(me.getPort(), InetAddress.getByName(me.getIp()));
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public void startHost() {
        recvThread = new Thread(this::recv, "UDP receive thread");
        // recvThread.setDaemon(true);
        recvThread.start();
    }

    public UDPChannel channel(Host peer) {
        UDPChannel chan = new UDPChannel(this, peer);
        channels.add(chan);
        return chan;
    }

    void send(DatagramPacket pkt) {
        try {
            socket.send(pkt);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    void recv() {
        DatagramPacket pkt = new DatagramPacket(new byte[1500], 1500);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                socket.receive(pkt);
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted())
                    e.printStackTrace();
                else {
                    return;
                }
            }

            boolean received = false;

            for (UDPChannel chan : this.channels) {
                if (chan.isReceiver(pkt)) {
                    byte[] data = new byte[pkt.getLength()];
                    // System.out.println(" [udp] got " + ChannelUtils.toStr(data));
                    System.arraycopy(pkt.getData(), 0, data, 0, pkt.getLength());
                    chan.doReceive(data);
                    received = true;
                    break;
                }
            }

            if (!received) {
                System.err.println(" undelivered packet from " + pkt.getAddress() + ":" + pkt.getPort());
            }
        }
    }

    public void cleanup() {
        recvThread.interrupt();
        socket.close();
        try {
            recvThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    List<UDPChannel> channels = new ArrayList<>();
    Host me;
    DatagramSocket socket;
    Thread recvThread;
}