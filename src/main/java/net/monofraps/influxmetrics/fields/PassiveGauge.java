package net.monofraps.influxmetrics.fields;

public class PassiveGauge<T> extends AbstractMeasurementField {
    private T currentValue;

    public PassiveGauge(final String name, final T initialValue) {
        super(name);
        this.currentValue = initialValue;
    }

    public synchronized void setValue(final T newValue) {
        currentValue = newValue;
    }

    @Override
    public T getValue() {
        return currentValue;
    }
}
