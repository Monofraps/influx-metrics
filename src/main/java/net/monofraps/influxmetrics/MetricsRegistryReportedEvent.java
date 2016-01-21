package net.monofraps.influxmetrics;

public class MetricsRegistryReportedEvent {
    private final long reportTime;
    private final int pointCount;

    public MetricsRegistryReportedEvent(long reportTime, int pointCount) {

        this.reportTime = reportTime;
        this.pointCount = pointCount;
    }

    public int getPointCount() {
        return pointCount;
    }

    public long getReportTime() {
        return reportTime;
    }
}
