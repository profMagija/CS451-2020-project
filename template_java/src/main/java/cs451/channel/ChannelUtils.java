package cs451.channel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * ChannelUtils
 */
public class ChannelUtils {

    public static ByteWriter writer() {
        return new ByteWriter();
    }

    public static ByteReader reader(byte[] data) {
        return new ByteReader(data);
    }

    public static String toStr(byte[] data) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < data.length; i++) {
            if (i > 0)
                sb.append(' ');
            sb.append(String.format("%02x", data[i]));
        }

        return sb.toString();
    }

    public static class ByteWriter {

        private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        public ByteWriter writeByte(byte x) {
            buffer.write(x);
            return this;
        }

        public ByteWriter writeInt(int x) {
            buffer.write((x >> 24) & 0xff);
            buffer.write((x >> 16) & 0xff);
            buffer.write((x >> 8) & 0xff);
            buffer.write(x & 0xff);
            return this;
        }

        public ByteWriter writeBytes(byte[] byts) {

            try {
                buffer.write(byts);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public byte[] done() {
            byte[] bts = buffer.toByteArray();
            try {
                buffer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bts;
        }

    }

    public static class ByteReader {
        public ByteReader(byte[] data) {
            this.data = data;
            this.loc = 0;
        }

        public byte readByte() {
            return data[loc++];
        }

        public int readInt() {
            return (data[loc++] << 24) | (data[loc++] << 16) | (data[loc++] << 8) | (data[loc++]);
        }

        public byte[] readEnd() {
            byte[] bts = new byte[data.length - loc];
            System.arraycopy(data, loc, bts, 0, bts.length);
            loc = data.length;
            return bts;
        }

        private byte[] data;
        private int loc;
    }

}