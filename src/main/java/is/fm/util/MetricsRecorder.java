package is.fm.util;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MetricsRecorder {

    private static final Logger l = LogManager.getLogger(MetricsRecorder.class);

    private static final int HISTORY = 100;
    private static final Map<String, MetricsRecorder> metrics = new ConcurrentHashMap<>();

    private final String name;
    private final int[] callTimes = new int[HISTORY];
    private final boolean[] callSuccess = new boolean[HISTORY];
    private long sumTimes = 0;
    private int successCnt = 0;
    private int cnt = 0;

    private String TU = "ms"; // default time unit

    private volatile int printInterval = 0; // suppressed by default
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

    // not synchronized
    public synchronized void recordInvocation(final long time, final boolean success) {
        if (printInterval > 0) {

            final int idx = cnt % HISTORY;

            int oldVal = callTimes[idx];
            // noinspection all
            int timeInt = (int) time < 0 ? 0 : (int) time;
            callTimes[idx] = timeInt;
            sumTimes -= oldVal;
            sumTimes += timeInt;

            boolean wasSuccess = callSuccess[idx];
            callSuccess[idx] = success;
            if (wasSuccess) --successCnt;
            if (success) ++successCnt;

            ++cnt;
            if (cnt % printInterval == 0) {
                l.info(this.toString());
                if (printFullStats)
                    l.info(printFullInfo());
            }
        }
    }

    public long avgTime() {
        return sumTimes / HISTORY;
    }

    public int errorRate() {
        return 100 - successRate();
    }

    public int successRate() {
        if (cnt < 10)
            return 100;
        return successCnt * 100 / HISTORY;
    }

    public String getMonitorName() {
        return name;
    }

    public String toString() {
        int maxTime = 0;
        // noinspection all
        for (int i = 0, length = callTimes.length; i < length; i++) {
            if (callTimes[i] > maxTime)
                maxTime = callTimes[i];
        }
        return new StringBuilder(76)
                .append(name)
                .append(" avgTime = ")
                .append(avgTime())
                .append(" ")
                .append(TU)
                .append(" maxTime = ")
                .append(maxTime).append(" ")
                .append(TU)
                .append(" successRate = ")
                .append(successRate())
                .append("%").toString();
    }

    public String printFullInfo() {
        int[] copy = callTimes.clone();
        Arrays.sort(copy);
        return new StringBuilder(64)
                .append("min = ")
                .append(copy[0]).append(TU)
                .append(", tp10 = ")
                .append(copy[(int) (copy.length * 0.1)]).append(TU)
                .append(", tp50 = ")
                .append(copy[copy.length / 2]).append(TU)
                .append(", tp90 = ")
                .append(copy[(int) (copy.length * 0.9)]).append(TU)
                .append(", max = ")
                .append(copy[copy.length - 1]).append(TU).toString();
    }
}
