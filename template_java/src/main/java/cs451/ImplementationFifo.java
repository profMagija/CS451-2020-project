package cs451;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

import cs451.channel.Channel;
import cs451.channel.ChannelUtils;
import cs451.channel.RetransmitChannel;
import cs451.channel.UdpHost;
import cs451.channel.UniformReliableBroadcast;

/**
 * Implementation
 */
public class ImplementationFifo implements Implementation {

    private Host me;
    private int[][] config;

    UdpHost host;
    Writer output;

    UniformReliableBroadcast channel;

    public void init(final Host me, List<Host> peers, Writer output, int[][] config) {
        this.me = me;
        this.output = output;
        this.config = config;

        host = new UdpHost(me);

        // channel = new LocalOrderChannel(new UniformReliableBroadcast(me.getId(),
        // peers.stream().map(p -> new Pair<>(p.getId(), (Channel) new
        // RetransmitChannel(host.channel(p))))
        // .collect(Collectors.toList())));
        channel = ChannelUtils.stack(//
                UniformReliableBroadcast.class, me.getId(), peers.stream()//
                        .map(p -> new Pair<Integer, Channel>(//
                                p.getId(), //
                                new RetransmitChannel(host.channel(p))))
                        .collect(Collectors.toList()));

        channel.onReceive(data -> {
            var br = ChannelUtils.reader(data);
            var peer = br.readInt();
            var seqNum = br.readInt();

            synchronized (output) {
                try {
                    output.append("d " + peer + " " + seqNum + "\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void run() throws Exception {

        final int packets = this.config[0][0];

        for (int i = 1; i <= packets; i++) {

            channel.send(ChannelUtils.writer().writeInt(me.getId()).writeInt(i).done());
            synchronized (output) {
                output.append("b " + i + "\n");
            }
        }
    }
}