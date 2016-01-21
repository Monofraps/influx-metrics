package net.monofraps.influxmetrics.fields;

import java.util.concurrent.atomic.AtomicLong;

public class Counter extends AbstractMeasurementField {
    private final AtomicLong currentValue = new AtomicLong(0);

    public Counter(final String name) {
        super(name);
    }

    public void inc() {
        currentValue.incrementAndGet();
    }

    public void dec() {
        currentValue.decrementAndGet();
    }

    @Override
    public Long getValue() {
        return currentValue.get();
    }

    public synchronized void setValue(final long value) {
        currentValue.set(value);
    }
}
