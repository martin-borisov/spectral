package mb.spectrum.prop;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public abstract class ConfigurableProperty<T> {
	
	protected String name;
	protected ObjectProperty<T> prop;
	protected T minValue, maxValue, initValue, increment;
	protected String unit;
	
	public ConfigurableProperty(String name, T minValue, T maxValue, T initValue, T increment) {
		this.name = name;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.initValue = initValue;
		this.increment = increment;
		this.prop = new SimpleObjectProperty<>(null, name, initValue);
	}
	
	public ConfigurableProperty(String name, ObjectProperty<T> prop, T minValue, T maxValue, T initValue, T increment,
            String unit) {
	    this(name, minValue, maxValue, initValue, increment);
        this.unit = unit;
    }

    public abstract T increment();
	public abstract T decrement();
	
	public String getName() {
		return name;
	}

	public ObjectProperty<T> getProp() {
		return prop;
	}
	
	public T getMinValue() {
		return minValue;
	}
	
	public T getMaxValue() {
		return maxValue;
	}
	
	public T getInitValue() {
		return initValue;
	}
	
	public T getIncrement() {
		return increment;
	}
	
	public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public T get() {
		return prop.get();
	}
}
