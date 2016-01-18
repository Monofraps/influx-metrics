package net.monofraps.influxmetrics;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author monofraps
 */
public interface InfluxSeries {
    String getMeasurementName();

    Map<String, String> getTags();

    Collection<DataPoint> getValueSets();

    List<String> getFieldNames();
}
