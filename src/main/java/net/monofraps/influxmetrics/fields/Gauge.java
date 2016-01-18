package net.monofraps.influxmetrics.fields;

import java.util.function.Supplier;

public class Gauge<T> extends AbstractMeasurementField {
    private final Supplier<T> supplier;

    public Gauge(String name, Supplier<T> supplier) {
        super(name);
        this.supplier = supplier;
    }

    @Override
    public T getValue() {
        return supplier.get();
    }
}
