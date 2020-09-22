package cs451;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    private static FileWriter output;

    private static void handleSignal() {
        // immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        // write/flush output file if necessary
        System.out.println("Writing output.");

        try {
            output.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().halt(0);
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        final Implementation impl = new Implementation();

        try {
            output = new FileWriter(parser.output());
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        List<String> cfgLines;
        try {
            cfgLines = Files.readAllLines(Paths.get(parser.config()));
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        final var configNumbers = new int[cfgLines.size()][];

        for (int i = 0; i < cfgLines.size(); i++) {
            final var parts = cfgLines.get(i).split("\\s+");
            configNumbers[i] = new int[parts.length];
            for (int j = 0; j < parts.length; j++) {
                configNumbers[i][j] = Integer.parseInt(parts[j]);
            }
        }

        Coordinator coordinator = new Coordinator(parser.myId(), parser.barrierIp(), parser.barrierPort(),
                parser.signalIp(), parser.signalPort());

        impl.init(parser.hosts().stream().filter(x -> x.getId() == parser.myId()).findAny().get(),
                parser.hosts().stream().filter(x -> x.getId() != parser.myId()).collect(Collectors.toList()), output,
                configNumbers);

        System.out.println("Waiting for all processes for finish initialization");
        coordinator.waitOnBarrier();

        System.out.println("Broadcasting messages...");

        try {
            impl.run();
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("Signaling end of broadcasting messages");
        coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
