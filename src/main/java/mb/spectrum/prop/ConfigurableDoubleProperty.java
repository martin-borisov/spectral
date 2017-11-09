package mb.spectrum.prop;

import java.math.BigDecimal;

/**
 * The arithmetic in this class is done with {@link BigDecimal} to avoid {@link Double} precision errors
 */
public class ConfigurableDoubleProperty extends ConfigurableProperty<Double> {

	public ConfigurableDoubleProperty(String name, Double minValue, Double maxValue, Double initValue,
			Double increment) {
		super(name, minValue, maxValue, initValue, increment);
	}

	@Override
	public Double increment() {
		Double val = BigDecimal.valueOf(prop.get()).add(BigDecimal.valueOf(increment)).doubleValue();
		val = val > maxValue ? maxValue : val;
		prop.set(val);
		return val;
	}

	@Override
	public Double decrement() {
		Double val = BigDecimal.valueOf(prop.get()).subtract(BigDecimal.valueOf(increment)).doubleValue();
		val = val < minValue ? minValue : val;
		prop.set(val);
		return val;
	}

}
