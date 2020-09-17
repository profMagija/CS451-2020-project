package cs451;

import java.io.IOException;
import java.io.Writer;
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
    Writer output;

    List<Channel> channels = new ArrayList<>();

    public void init(Host me, List<Host> peers, int packets, Writer output) {
        this.me = me;
        this.peers = peers;
        this.packets = packets;
        this.output = output;

        host = new UdpHost(me);

        for (Host peer : peers) {
            Channel c = new RetransmitChannel(host.channel(peer));

            channels.add(c);

            c.onReceive(d -> {
                try {
                    output.append("d " + peer.getId() + " " + d[0] + "\n");
                } catch (Exception e) {
                    // TODO: handle exception
                }
            });
        }
    }

    public void run() throws Exception {

        for (int i = 1; i <= this.packets; i++) {
            for (Channel channel : channels) {
                synchronized (output) {
                    output.append("b" + i + "\n");
                }
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