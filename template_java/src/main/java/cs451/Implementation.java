package cs451;

import java.io.Writer;
import java.util.List;

public interface Implementation {
    void init(final Host me, List<Host> peers, Writer output, int[][] config);

    void run() throws Exception;
}
