package io.contek.invoker.commons;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MetricsRecorder {

    private static final Logger l = LogManager.getLogger(MetricsRecorder.class);

    public static final int HISTORY = 100;
    private static final Map<String, MetricsRecorder> metrics = new ConcurrentHashMap<>();

    private final String name;
    private final int[] callTimes = new int[HISTORY];
    private final boolean[] callSuccess = new boolean[HISTORY];
    private long sumTimes = 0;
    private int successCnt = 0;
    private int n = 0;
    private int idx = 0;
    private int cnt = 0;

    private String TU = "ms"; // time unit

    private volatile int printInterval = 0;
    private volatile boolean printFullStats = false;

    private MetricsRecorder(String name) {
        this.name = name;
    }

    public static MetricsRecorder getRecorder(String name) {
        return metrics.computeIfAbsent(name, MetricsRecorder::new);
    }

    public void setPrintInterval(int printInterval) {
        this.printInterval = printInterval;
    }

    public void setPrintFullStats(boolean printFullStats) {
        this.printFullStats = printFullStats;
    }

    public void setTimeMicro() {
        TU = "µs";
    }

    public void recordInvocation(long time, boolean success) { // yolo, not synchronized
        if (printInterval > 0) {
            int oldVal = callTimes[idx];
            callTimes[idx] = (int) time;
            sumTimes += (int) time;
            sumTimes -= oldVal;

            boolean wasSuccess = callSuccess[idx];
            callSuccess[idx] = success;
            successCnt += success ? 1 : 0;
            successCnt -= wasSuccess ? 1 : 0;

            idx++;
            idx %= HISTORY;

            if (n < HISTORY) {
                n++;
            }

            cnt++;
            if (cnt % printInterval == 0) {
                l.info(this.toString());
                if (printFullStats)
                    l.info(printFullInfo());
            }
        }
    }

    public long avgTime() {
        return sumTimes / n;
    }

    public int errorRate() {
        if (n < 10)
            return 0;
        return 100 - successCnt * 100 / n;
    }

    public String toString() {
        return "%s avgTime = %d %s successRate = %d%%".formatted(name, avgTime(), TU, 100 - errorRate());
    }

    public String printFullInfo() {
        int[] copy = Arrays.copyOf(callTimes, callTimes.length);
        Arrays.sort(copy);
        return "min = %,d%s, tp10 = %,d%s, tp50 = %,d%s, tp90 = %,d%s, max = %,d%s".formatted(
                copy[0], TU,
                copy[(int) (copy.length * 0.1)], TU,
                copy[copy.length / 2], TU,
                copy[(int) (copy.length * 0.9)], TU,
                copy[copy.length - 1], TU);
    }
}
