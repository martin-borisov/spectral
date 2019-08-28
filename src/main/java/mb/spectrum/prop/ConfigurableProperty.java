package mb.spectrum.prop;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

public abstract class ConfigurableProperty<T> {
	
	protected String name;
	protected ObjectProperty<T> prop;
	protected T minValue, maxValue, initValue, increment;
	protected String unit;
	private Map<String, ChangeListener<T>> listeners;
	
	public ConfigurableProperty(String name, T minValue, T maxValue, T initValue, T increment) {
		this.name = name;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.initValue = initValue;
		this.increment = increment;
		prop = new SimpleObjectProperty<>(null, name, initValue);
		listeners = new HashMap<String, ChangeListener<T>>();
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

    public void addListener(ChangeListener<T> listener, String key) {
        if(!listeners.containsKey(key)) {
            listeners.put(key, listener);
            prop.addListener(listener);
        }
    }

    public void removeListener(String key) {
        ChangeListener<T> listener = listeners.remove(key);
        if(listener != null) {
            prop.removeListener(listener);
        }
    }
}
