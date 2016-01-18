package net.monofraps.influxmetrics;

import com.google.common.collect.ImmutableList;
import net.monofraps.influxmetrics.fields.IMeasurementField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TimeSeries implements InfluxSeries {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String measurementName;
    private final Map<String, String> tags;
    private final List<IMeasurementField> fields;

    public TimeSeries(String measurementName, List<MetricTag> tags, List<IMeasurementField> fields) {
        this.measurementName = measurementName;
        this.tags = tags.stream().collect(Collectors.toMap(MetricTag::getTagName, MetricTag::getTagValue));
        this.fields = fields;
    }

    public static Builder withName(final String measurementName) {
        return new Builder(measurementName);
    }

    @Override
    public String getMeasurementName() {
        return measurementName;
    }

    @Override
    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public Collection<DataPoint> getValueSets() {
        return ImmutableList.of(new DataPoint(System.currentTimeMillis(), TimeUnit.MILLISECONDS, fields.stream().collect(Collectors.toMap(IMeasurementField::getName, field -> {
            Object value = field.getValue();
            if(value == null) {
                logger.error("IMeasurementField::getValue must not return null in series {} for field {}", measurementName, field.getName());
                return "null";
            }

            return value;
        }))));
    }

    @Override
    public List<String> getFieldNames() {
        return fields.stream().map(IMeasurementField::getName).collect(Collectors.toList());
    }

    public List<IMeasurementField> getFields() {
        return fields;
    }

    public static class Builder {
        private final String measurementDefinition;
        private List<MetricTag> tags = new ArrayList<>();
        private List<IMeasurementField> fields = new ArrayList<>();

        public Builder(final String measurementDefinition) {
            this.measurementDefinition = measurementDefinition;
        }

        public Builder withTag(final MetricTag tag) {
            this.tags.add(tag);
            return this;
        }

        public Builder withTags(final Collection<MetricTag> tags) {
            this.tags.addAll(tags);
            return this;
        }

        public Builder withField(final IMeasurementField measurementField) {
            this.fields.add(measurementField);
            return this;
        }

        public Builder withFields(final Collection<IMeasurementField> measurementFields) {
            measurementFields.forEach(this::withField);
            return this;
        }

        public TimeSeries build() {
            return new TimeSeries(measurementDefinition, tags, fields);
        }
    }
}
