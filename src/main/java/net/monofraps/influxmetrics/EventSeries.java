package net.monofraps.influxmetrics;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.monofraps.influxmetrics.reflection.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * An EventSeries is a special type of InfluxSeries which creates data points at irregular intervals.
 *
 *
 * @param <T> Event model type.
 */
public class EventSeries<T> implements InfluxSeries {
    private static final Logger logger = LoggerFactory.getLogger(EventSeries.class);
    private final String measurementName;
    private final Map<String, String> tags;
    private final BlockingQueue<DataPoint> events = new LinkedBlockingQueue<>();
    private final ImmutableCollection<GetterField> fields;

    protected EventSeries(String measurementName, List<MetricTag> tags, ImmutableCollection<Method> fields) {
        this.measurementName = measurementName;
        this.tags = tags.stream().collect(Collectors.toMap(MetricTag::getTagName, MetricTag::getTagValue));

        this.fields = ImmutableList.copyOf(fields.stream().map(fieldGetter -> new GetterField(fieldGetter.getName().substring(3), fieldGetter)).collect(Collectors.toList()));
    }

    /**
     * Creates an EventSeries builder using the event model T (klass).
     * The method collects all public getters defined by `klass` and superclasses of `klass` as fields of the event model.
     * @param klass The class to scan for event fields.
     * @param name The event series' name.
     * @param <T> The event model type.
     * @return A pre-configured EventSeries builder.
     */
    public static <T> EventSeries.Builder<T> fromPojo(Class<T> klass, String name) {
        final List<Field> deepFields = ReflectionUtils.getDeepFields(klass);
        final List<Method> methods = ReflectionUtils.getPublicGetters(klass);
        final List<Method> fields = methods.stream().filter(m -> deepFields.stream().filter(f -> m.getName().substring(3).equalsIgnoreCase(f.getName())).findAny().isPresent()).collect(Collectors.toList());

        return new Builder<>(name, fields);
    }

    public void commitEvent(T event) {
        try {
            final Map<String, Object> fieldValues = new HashMap<>(fields.size(), 1f);
            for (GetterField fieldGetter : fields) {
                fieldValues.put(fieldGetter.getName(), fieldGetter.invoke(event));
            }
            events.offer(new DataPoint(System.currentTimeMillis(), TimeUnit.MILLISECONDS, fieldValues));
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("Failed to get event field value", e);
        }
    }

    @Override
    public List<String> getFieldNames() {
        return fields.stream().map(GetterField::getName).collect(Collectors.toList());
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
        Collection<DataPoint> valueSets = new LinkedList<>();
        events.drainTo(valueSets);

        return valueSets;
    }

    private static class GetterField {
        private final Method getter;
        private final String name;

        public GetterField(String name, Method getter) {

            this.getter = getter;
            this.name = name;
        }

        public Object invoke(Object target) throws InvocationTargetException, IllegalAccessException {
            return getter.invoke(target);
        }

        public String getName() {
            return name;
        }
    }

    public static class Builder<T> {
        protected final String measurementName;
        private final Collection<Method> fields;
        protected List<MetricTag> tags = new ArrayList<>();

        public Builder(final String measurementName, Collection<Method> fields) {
            this.measurementName = measurementName;
            this.fields = fields;
        }

        public Builder<T> withTag(final MetricTag tag) {
            this.tags.add(tag);
            return this;
        }

        public Builder<T> withTags(final Collection<MetricTag> tags) {
            this.tags.addAll(tags);
            return this;
        }

        public EventSeries<T> build() {
            return new EventSeries<>(measurementName, tags, ImmutableSet.copyOf(fields));
        }
    }

}
