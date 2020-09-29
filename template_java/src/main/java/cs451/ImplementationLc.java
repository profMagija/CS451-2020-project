package cs451;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import cs451.channel.BestEffortBroadcast;
import cs451.channel.Channel;
import cs451.channel.ChannelUtils;
import cs451.channel.LocalOrderChannel;
import cs451.channel.RetransmitChannel;
import cs451.channel.UdpHost;
import cs451.channel.UniformReliableBroadcast;

/**
 * Implementation
 */
public class ImplementationLc implements Implementation {

    private Host me;
    private int[][] config;

    UdpHost host;
    Writer output;

    Set<Integer> dependants;
    BlockingQueue<Integer>[] depMsgs;

    LocalOrderChannel channel;

    @SuppressWarnings("unchecked")
    public void init(final Host me, List<Host> peers, Writer output, int[][] config) {
        this.me = me;
        this.output = output;
        this.config = config;

        host = new UdpHost(me);

        var peerChans = peers.stream()//
                .map(p -> new Pair<Integer, Channel>(//
                        p.getId(), //
                        new RetransmitChannel(host.channel(p), true)))
                .collect(Collectors.toList());

        channel = ChannelUtils.stack(//
                LocalOrderChannel.class, //
                // LoggerChannel.class, me.getId(), //
                // UniformReliableBroadcast.class, me.getId(), peers.size() + 1,
                BestEffortBroadcast.class, me.getId(), peerChans);

        depMsgs = new BlockingQueue[peers.size() + 1];

        for (int i = 1; i < config.length; i++) {
            if (config[i][0] == me.getId()) {
                for (int j = 1; j < config[i].length; j++) {
                    depMsgs[j - 1] = new ArrayBlockingQueue<>(config[0][0]);
                }
                break;
            }
        }

        channel.onReceiveDependant((uuid, data) -> {
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

            if (depMsgs[peer - 1] != null) {
                // System.out.printf(" [%d] -> [%d] gotten ...\n", peer, me.getId());
                try {
                    depMsgs[peer - 1].put(uuid);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        host.startHost();
    }

    public void run() throws Exception {

        final int packets = this.config[0][0];

        for (int i = 1; i <= packets; i++) {
            final var deps = new ArrayList<Integer>();

            for (var depMsgQueue : depMsgs) {
                if (depMsgQueue != null) {
                    deps.add(depMsgQueue.take());
                }
            }

            synchronized (output) {
                output.append("b " + i + "\n");
            }
            channel.sendDependent(ChannelUtils.writer().writeInt(me.getId()).writeInt(i).done(), deps);
        }

        channel.cleanup();
    }
}