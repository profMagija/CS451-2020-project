package cs451;

import java.util.ArrayList;
import java.util.List;

import cs451.channel.Channel;
import cs451.channel.ChannelUtils;
import cs451.channel.RetransmitChannel;
import cs451.channel.UdpHost;

/**
 * Implementation
 */
public class Implementation {

    Host me;
    List<Host> peers;
    int packets;

    UdpHost host;

    List<Channel> channels = new ArrayList<>();

    public void init(Host me, List<Host> peers, int packets) {
        this.me = me;
        this.peers = peers;
        this.packets = packets;

        host = new UdpHost(me);

        for (Host peer : peers) {
            Channel c = new RetransmitChannel(host.channel(peer));

            channels.add(c);

            c.onReceive(d -> System.out.println(peer.getId() + " : " + ChannelUtils.toStr(d)));
        }
    }

    public void run() {

        for (int i = 1; i <= this.packets; i++) {
            for (Channel channel : channels) {
                System.out.println("sending " + i);
                channel.send(new byte[] { (byte) i });
            }
        }

        System.out.println("cleaning up");

        for (Channel channel : channels) {
            channel.cleanup();
        }

        host.cleanup();

    }
}