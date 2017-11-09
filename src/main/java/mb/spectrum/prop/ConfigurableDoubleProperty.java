package mb.spectrum.prop;

public class ConfigurableDoubleProperty extends ConfigurableProperty<Double> {

	public ConfigurableDoubleProperty(String name, Double minValue, Double maxValue, Double initValue,
			Double increment) {
		super(name, minValue, maxValue, initValue, increment);
	}

	@Override
	public Double increment() {
		Double val = prop.get() + increment;
		val = val > maxValue ? maxValue : val;
		prop.set(val);
		return val;
	}

	@Override
	public Double decrement() {
		Double val = prop.get() - increment;
		val = val < minValue ? minValue : val;
		prop.set(val);
		return val;
	}

}
