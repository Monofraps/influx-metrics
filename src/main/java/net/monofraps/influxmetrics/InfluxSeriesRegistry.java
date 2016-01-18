package net.monofraps.influxmetrics;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import net.monofraps.influxmetrics.fields.IMeasurementField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InfluxSeriesRegistry {
    private Collection<RegistryEventListener> registryListeners = new ArrayList<>();
    private ConcurrentHashMap<MeasurementSeriesKey, InfluxSeries> measurementSeries = new ConcurrentHashMap<>();

    public Collection<InfluxSeries> getSeries() {
        return measurementSeries.values();
    }

    public TimeSeries timeSeries(String measurementName, List<MetricTag> tags, List<IMeasurementField> fields) {
        final MeasurementSeriesKey seriesKey = new MeasurementSeriesKey(measurementName, tags);
        final InfluxSeries series = measurementSeries.computeIfAbsent(seriesKey, measurementSeriesKey -> {
            final TimeSeries timeSeries = TimeSeries.withName(measurementName).withTags(tags).withFields(fields).build();
            notifySeriesRegistered(timeSeries);

            return timeSeries;
        });

        Preconditions.checkState(series instanceof TimeSeries, "Series of different type exists already");
        return (TimeSeries) series;
    }

    public <T> EventSeries<T> eventSeries(String measurementName, List<MetricTag> tags, Class<T> klass) {
        final MeasurementSeriesKey seriesKey = new MeasurementSeriesKey(measurementName, tags);
        final InfluxSeries series = measurementSeries.computeIfAbsent(seriesKey, measurementSeriesKey -> {
            final EventSeries<T> eventSeries = EventSeries.fromPojo(klass, measurementName).withTags(tags).build();
            notifySeriesRegistered(eventSeries);

            return eventSeries;
        });

        Preconditions.checkState(series instanceof EventSeries, "Series of different type exists already");
        return (EventSeries<T>) series;
    }

    private void notifySeriesRegistered(final InfluxSeries series) {
        registryListeners.forEach(listener -> listener.onSeriesRegistered(series));
    }

    private void notifySeriesRemoved(final InfluxSeries series) {
        registryListeners.forEach(listener -> listener.onSeriesRemoved(series));
    }

    public void registerEventListener(final RegistryEventListener registryEventListener) {
        registryListeners.add(registryEventListener);
    }

    public void removeEventListener(final RegistryEventListener registryEventListener) {
        registryListeners.remove(registryEventListener);
    }

    private static class MeasurementSeriesKey {
        private final Integer hashCode;

        public MeasurementSeriesKey(String measurementName, List<MetricTag> tags) {
            hashCode = Objects.hashCode(measurementName, tags);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MeasurementSeriesKey that = (MeasurementSeriesKey) o;
            return Objects.equal(hashCode, that.hashCode);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(hashCode);
        }
    }
}
