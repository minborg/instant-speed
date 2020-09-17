package org.minborg.instantspeed.queue;

import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.WireType;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder.single;

public final class Benchmark {

    public static final int DURATION_S = 20;
    public static final int MSG_PER_SECOND = 20_000;
    public static final long INTER_MSG_PERIOD_NS = TimeUnit.SECONDS.toNanos(1) / MSG_PER_SECOND;

    public static void main(String[] args) {

        System.out.format("Total memory usage: %,d MiB%n", Runtime.getRuntime().totalMemory() / (1 << 20));
        System.out.format("Java version: %s %s %s%n", System.getProperty("java.runtime.name"), System.getProperty("java.runtime.version"), System.getProperty("java.vendor"));

        final var histograms = IntStream.range(0, DURATION_S)
                .mapToObj(i -> new Histogram())
                .collect(toList());

        final var sourceQueue = single("data/source/jlbh/").wireType(WireType.BINARY_LIGHT).build();
        final var appender = sourceQueue.acquireAppender();

        var expectedNs = System.nanoTime();
        for (int s = 0; s < DURATION_S; s++) {
            var histogram = histograms.get(s);
            for (int m = 0; m < MSG_PER_SECOND; m++) {
                writeMessage(appender, histogram, expectedNs);
                expectedNs += INTER_MSG_PERIOD_NS;
                while (System.nanoTime()<expectedNs) {}
            }
        }

        for (int s = 0; s < DURATION_S; s++) {
            System.out.println(s + ": " + histograms.get(s).toMicrosFormat());
        }

    }

    private static void writeMessage(final ExcerptAppender appender,
                                     final Histogram histogram,
                                     final long expectedNs) {
        final long startNs = System.nanoTime();
        try (DocumentContext dc = appender.writingDocument()) {
            dc.wire().write("T").int64(startNs);
        }
        if (0 == 0) {
            histogram.sampleNanos(System.nanoTime() - expectedNs);
        } else {
            histogram.sampleNanos(System.nanoTime() - startNs);
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
