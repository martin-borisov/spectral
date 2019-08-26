package mb.spectrum.prop;

import javafx.beans.property.ObjectProperty;

public class ConfigurableIntegerProperty extends ConfigurableProperty<Integer> {

	public ConfigurableIntegerProperty(String name, Integer minValue, Integer maxValue, Integer initValue,
			Integer increment) {
		super(name, minValue, maxValue, initValue, increment);
	}
	
	public ConfigurableIntegerProperty(String name, ObjectProperty<Integer> prop, Integer minValue, Integer maxValue,
            Integer initValue, Integer increment, String unit) {
        super(name, prop, minValue, maxValue, initValue, increment, unit);
    }

    @Override
	public Integer increment() {
		Integer val = prop.get() + increment;
		val = val > maxValue ? maxValue : val;
		prop.set(val);
		return val;
	}

	@Override
	public Integer decrement() {
		Integer val = prop.get() - increment;
		val = val < minValue ? minValue : val;
		prop.set(val);
		return val;
	}
}
