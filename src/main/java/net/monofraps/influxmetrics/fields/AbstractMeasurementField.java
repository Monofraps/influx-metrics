package net.monofraps.influxmetrics.fields;

/**
 * @author monofraps
 */
public abstract class AbstractMeasurementField implements IMeasurementField {
    private final String name;

    public AbstractMeasurementField(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
