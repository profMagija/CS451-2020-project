package cs451.channel;

import java.io.IOException;

public class LoggerChannel extends Channel {

    public LoggerChannel(int id, Channel chan) {
        this.id = id;
        this.chan = chan;
        this.chan.onReceive(data -> {
            System.out.printf(" [ %d ] << %s\n", id, ChannelUtils.toStr(data));
            deliver(data);
        });
    }

    @Override
    public void close() throws IOException {
        chan.close();
    }

    @Override
    public void cleanup() {
        chan.cleanup();
    }

    @Override
    public void send(byte[] data) {
        System.out.printf(" [ %d ] >> %s\n", id, ChannelUtils.toStr(data));
        chan.send(data);
    }

    private final int id;
    private final Channel chan;

}
