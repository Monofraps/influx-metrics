package net.monofraps.influxmetrics;

public class MetricsReportedEvent {
    private final long reportTime;
    private final long pointCount;
    private final long batchCount;

    public MetricsReportedEvent(long batchCount, long pointCount, long reportTime) {

        this.batchCount = batchCount;
        this.pointCount = pointCount;
        this.reportTime = reportTime;
    }

    public long getBatchCount() {
        return batchCount;
    }

    public long getPointCount() {
        return pointCount;
    }

    public long getReportTime() {
        return reportTime;
    }
}
