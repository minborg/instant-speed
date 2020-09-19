package org.minborg.instantspeed.zgc;

import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;

import java.io.File;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;
import static net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder.single;

/*
Java15 on MAC :
0.018s][info][gc] Using The Z Garbage Collector
Total memory usage: 1,024 MiB
Java version: OpenJDK Runtime Environment 15+36 AdoptOpenJDK
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access using Lookup on net.openhft.chronicle.core.Jvm (file:/Users/pemi/.m2/repository/net/openhft/chronicle-core/2.20.28/chronicle-core-2.20.28.jar) to class java.lang.reflect.AccessibleObject
WARNING: Please consider reporting this to the maintainers of net.openhft.chronicle.core.Jvm
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
Sep 17, 2020 3:42:59 PM net.openhft.chronicle.core.cleaner.impl.reflect.ReflectionBasedByteBufferCleanerService <clinit>
INFO: Make sure you have set the command line option "--illegal-access=permit --add-exports java.base/jdk.internal.ref=ALL-UNNAMED"
The Chronicle Queue is persisted to /Users/pemi/git/minborg/instant-speed/zgc/data/queuePerforming phase WARMUP with a duration of 10 s.
Wrote 1000000 messages in 10 seconds (100000 msg/s)
Performing phase BENCHMARK with a duration of 60 s.
Wrote 6000000 messages in 62 seconds (96774 msg/s)
Distribution of latency in microseconds:
50/90 99/99.9 99.99/99.999 - worst was 0.061 / 0.067  0.149 / 1.84  26.4 / 37.8 - 515
*/

/*
Java15 on MAC :
Total memory usage: 981 MiB
Java version: OpenJDK Runtime Environment 1.8.0_232-b09 AdoptOpenJDK
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
The Chronicle Queue is persisted to /Users/pemi/git/minborg/instant-speed/zgc/data/queuePerforming phase WARMUP with a duration of 10 s.
Wrote 1,000,000 messages in 10 seconds (100000 msg/s)
Performing phase BENCHMARK with a duration of 60 s.
Wrote 6,000,000 messages in 62 seconds (96774 msg/s)
Latency distribution in microseconds:
50/90 99/99.9 99.99/99.999 - worst was 0.066 / 0.069  0.134 / 1.80  25.4 / 39.0 - 1026
Heap
 PSYoungGen      total 305664K, used 57680K [0x00000007aab00000, 0x00000007c0000000, 0x00000007c0000000)
  eden space 262144K, 22% used [0x00000007aab00000,0x00000007ae354010,0x00000007bab00000)
  from space 43520K, 0% used [0x00000007bd580000,0x00000007bd580000,0x00000007c0000000)
  to   space 43520K, 0% used [0x00000007bab00000,0x00000007bab00000,0x00000007bd580000)
 ParOldGen       total 699392K, used 0K [0x0000000780000000, 0x00000007aab00000, 0x00000007aab00000)
  object space 699392K, 0% used [0x0000000780000000,0x0000000780000000,0x00000007aab00000)
 Metaspace       used 10423K, capacity 10764K, committed 10880K, reserved 1058816K
  class space    used 1171K, capacity 1308K, committed 1408K, reserved 1048576K
 */


public final class LatencyBenchmark {

    // Todo: Check tracing in queue is off

    public static final long MSG_PER_SECOND = 100_000;
    public static final long MSG_PER_BATCH = 100;
    public static final long INTER_MSG_PERIOD_NS = TimeUnit.SECONDS.toNanos(1) / MSG_PER_SECOND;

    public static final Message[] MESSAGE_BATCH = generateBatch();
    public static final String QUEUE_PATH = "data/queue";

    public static void main(String[] args) {

        final File queueFile = new File(QUEUE_PATH);

        System.out.format("Total memory usage: %,d MiB%n", Runtime.getRuntime().totalMemory() / (1 << 20));
        System.out.format("Java version: %s %s %s%n", System.getProperty("java.runtime.name"), System.getProperty("java.runtime.version"), System.getProperty("java.vendor"));
        System.out.format("The Chronicle Queue is persisted to %s%n", queueFile.getAbsolutePath());

        final HeapPolluter heapPolluter = new HeapPolluter();
        final Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(heapPolluter);

        final Histogram histogram = new Histogram();

        try (AffinityLock lock = AffinityLock.acquireLock(OS.isLinux() ? "last" : "none")) {
            try (ChronicleQueue sourceQueue = single(QUEUE_PATH).wireType(WireType.BINARY_LIGHT).build()) {
                final ExcerptAppender appender = sourceQueue.acquireAppender();

                for (Phase phase : Phase.values()) {
                    System.out.println("Performing phase " + phase + " with a duration of " + phase.durationS + " s.");
                    histogram.reset();
                    int cnt = 0;
                    final long startNs = System.nanoTime();
                    for (int s = 0; s < (MSG_PER_SECOND * phase.durationS()) / MSG_PER_BATCH; s++) {
                        writeMessageBatch(appender, histogram);
                        cnt += MSG_PER_BATCH;
                    }
                    final long elapsedNs = System.nanoTime() - startNs;
                    final int elapsedS = (int) TimeUnit.NANOSECONDS.toSeconds(elapsedNs);
                    System.out.format("Wrote %,d messages in %,d seconds (%.0f msg/s)%n", cnt, elapsedS, ((double) cnt) / elapsedS);
                }

                System.out.println("Latency distribution in microseconds:");
                System.out.println(histogram.toMicrosFormat());
            }
        }
        heapPolluter.stop();
    }

    private static void writeMessageBatch(final ExcerptAppender appender, final Histogram histogram) {
        try (DocumentContext dc = appender.writingDocument()) {
            final Wire wire = dc.wire();
            for (Message message : MESSAGE_BATCH) {
                final long startNs = System.nanoTime();
                message.apply(wire);
                final long elapsedNs = System.nanoTime() - startNs;
                histogram.sampleNanos(elapsedNs);
                final long nextNs = startNs + INTER_MSG_PERIOD_NS;
                // Spin wait
                while (System.nanoTime() < nextNs) {
                }
            }
        }
    }

    private static Message[] generateBatch() {
        final Random random = new Random(42);
        return IntStream.range(0, (int) MSG_PER_BATCH)
                .mapToObj(i -> generateMessage(random))
                .toArray(Message[]::new);
    }

    private static Message generateMessage(Random random) {

        // Create a random 4-character ticker of chars between 'A' and 'Z'
        final String ticker = random.ints('A', 'Z' + 1)
                .mapToObj(i -> (char) i)
                .limit(4)
                .reduce(new StringBuilder(), StringBuilder::append, StringBuilder::append)
                .toString();

        return new Message(ticker, 50 + random.nextFloat() * 450);
    }


    private enum Phase {
        WARMUP(TimeUnit.MINUTES.toSeconds(2)),
        BENCHMARK(TimeUnit.HOURS.toSeconds(1));

        final int durationS;

        Phase(final long durationS) {
            this.durationS = Math.toIntExact(durationS);
        }

        int durationS() {
            return durationS;
        }
    }

    private static final class Message {

        private final String ticker;
        private final float latest;
        private final int encodedTicker;

        public Message(String ticker, float latest) {
            this.ticker = ticker;
            this.encodedTicker = MessageUtil.encode(ticker);
            this.latest = latest;
        }

        void apply(Wire wire) {
            wire
                    .write("T").int32(encodedTicker)
                    .write("L").float32(latest);
        }

        @Override
        public String toString() {
            return "Message{" +
                    "ticker='" + ticker + '\'' +
                    ", encodedTicker=" + encodedTicker +
                    ", latest=" + latest +
                    '}';
        }
    }


}

/*
0: 50/90 99/99.9 99.99 - worst was 0.626 / 2.73  29.5 / 74.5  292 - 21,300
1: 50/90 99/99.9 99.99 - worst was 0.169 / 0.409  4.06 / 34.7  282 - 304
2: 50/90 99/99.9 99.99 - worst was 0.162 / 0.243  2.20 / 25.9  96.0 - 155
3: 50/90 99/99.9 99.99 - worst was 0.166 / 0.273  2.30 / 28.1  136 - 829
4: 50/90 99/99.9 99.99 - worst was 0.163 / 0.252  2.10 / 21.3  43.9 - 62.1
5: 50/90 99/99.9 99.99 - worst was 0.161 / 0.255  1.85 / 12.9  38.3 - 137
6: 50/90 99/99.9 99.99 - worst was 0.161 / 0.252  1.94 / 15.7  36.7 - 41.3
7: 50/90 99/99.9 99.99 - worst was 0.159 / 0.247  2.00 / 18.1  38.3 - 39.0
8: 50/90 99/99.9 99.99 - worst was 0.160 / 0.256  2.03 / 18.2  35.2 - 90.4
9: 50/90 99/99.9 99.99 - worst was 0.160 / 0.249  1.83 / 21.1  42.6 - 55.2
10: 50/90 99/99.9 99.99 - worst was 0.159 / 0.222  1.98 / 17.7  43.6 - 49.3
11: 50/90 99/99.9 99.99 - worst was 0.160 / 0.234  1.83 / 10.66  38.8 - 66.8
12: 50/90 99/99.9 99.99 - worst was 0.163 / 0.267  1.76 / 9.12  31.4 - 39.3
13: 50/90 99/99.9 99.99 - worst was 0.163 / 0.269  2.25 / 20.3  41.1 - 70.9
14: 50/90 99/99.9 99.99 - worst was 0.164 / 0.269  2.22 / 20.3  40.3 - 96.0
15: 50/90 99/99.9 99.99 - worst was 0.162 / 0.261  2.15 / 19.1  39.3 - 43.6
16: 50/90 99/99.9 99.99 - worst was 0.160 / 0.235  1.85 / 18.5  41.1 - 109.8
17: 50/90 99/99.9 99.99 - worst was 0.162 / 0.222  1.88 / 17.6  38.3 - 43.9
18: 50/90 99/99.9 99.99 - worst was 0.162 / 0.257  2.09 / 17.1  39.3 - 39.6
19: 50/90 99/99.9 99.99 - worst was 0.162 / 0.265  1.98 / 17.9  47.7 - 76.0



0: 50/90 99/99.9 99.99 - worst was 0.421 / 10.78  15,700 / 17,500  17,500 - 17,500
1: 50/90 99/99.9 99.99 - worst was 0.231 / 0.379  17.3 / 29.8  68.4 - 73.5
2: 50/90 99/99.9 99.99 - worst was 0.211 / 0.263  13.9 / 28.9  87.3 - 102.7
3: 50/90 99/99.9 99.99 - worst was 0.216 / 0.285  14.7 / 28.2  200 - 246
4: 50/90 99/99.9 99.99 - worst was 0.206 / 0.234  13.0 / 27.5  36.0 - 48.8
5: 50/90 99/99.9 99.99 - worst was 0.206 / 0.234  12.9 / 27.3  49.5 - 49.5
6: 50/90 99/99.9 99.99 - worst was 0.210 / 0.256  16.0 / 28.4  51.8 - 84.7
7: 50/90 99/99.9 99.99 - worst was 0.206 / 0.235  14.0 / 27.3  55.4 - 58.5
8: 50/90 99/99.9 99.99 - worst was 0.206 / 0.237  15.9 / 32.6  76.5 - 100.6
9: 50/90 99/99.9 99.99 - worst was 0.208 / 0.237  16.2 / 28.5  38.0 - 49.8
10: 50/90 99/99.9 99.99 - worst was 0.207 / 0.245  16.4 / 30.1  49.3 - 50.0
11: 50/90 99/99.9 99.99 - worst was 0.207 / 0.234  12.8 / 26.8  43.9 - 50.6
12: 50/90 99/99.9 99.99 - worst was 0.215 / 0.359  15.8 / 30.4  278 - 278
13: 50/90 99/99.9 99.99 - worst was 0.214 / 0.243  13.5 / 26.2  61.8 - 80.6
14: 50/90 99/99.9 99.99 - worst was 0.208 / 0.234  13.0 / 27.2  43.6 - 44.2
15: 50/90 99/99.9 99.99 - worst was 0.210 / 0.237  12.70 / 25.0  54.1 - 57.7
16: 50/90 99/99.9 99.99 - worst was 0.216 / 0.259  14.7 / 27.3  40.8 - 55.7
17: 50/90 99/99.9 99.99 - worst was 0.213 / 0.239  13.2 / 27.1  50.3 - 70.9
18: 50/90 99/99.9 99.99 - worst was 0.213 / 0.240  12.58 / 26.0  44.7 - 59.8
19: 50/90 99/99.9 99.99 - worst was 0.211 / 0.238  11.74 / 25.9  44.2 - 46.0


 */
