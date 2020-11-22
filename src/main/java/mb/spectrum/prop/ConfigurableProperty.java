package mb.spectrum.prop;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

public abstract class ConfigurableProperty<T> {
	
	protected String name;
	protected ObjectProperty<T> prop;
	protected T minValue, maxValue, initValue, increment;
	protected String unit;
	private Map<String, ChangeListener<T>> listeners;
	
    private static final int UPDATE_FINISHED_INTERVAL_MS = 1000;
    private Timer updateFinishedTimer;
    private TimerTask updateFinishedTask;
	
	public ConfigurableProperty(String name, T minValue, T maxValue, T initValue, T increment) {
		this.name = name;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.initValue = initValue;
		this.increment = increment;
		prop = new SimpleObjectProperty<>(null, name, initValue);
		listeners = new HashMap<String, ChangeListener<T>>();
		initTimer();
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
    
    public void addListener(ChangeListener<? super T> listener) {
        prop.addListener(listener);
    }

    public void removeListener(ChangeListener<? super T> listener) {
        prop.removeListener(listener);
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
    
    public void addUpdateFinishedListener(ChangeListener<? super T> listener) {
        prop.addListener((obs, oldVal, newVal) -> {
            
            // Create a timer on the fly to avoid redundant threads
            if(updateFinishedTimer == null) {
                updateFinishedTimer = new Timer("Property Timer"); 
            }

            updateFinishedTask.cancel();
            updateFinishedTimer.schedule(updateFinishedTask = new TimerTask() {
                public void run() {
                    Platform.runLater(new Runnable() {
                        public void run() {
                            listener.changed(obs, oldVal, newVal);
                            
                            // When the task is done kill the timer thread
                            updateFinishedTimer.cancel();
                            updateFinishedTimer = null;
                        }
                    });
                }
            }, UPDATE_FINISHED_INTERVAL_MS);
        });
    }
    
    private void initTimer() {
        updateFinishedTask = new TimerTask() {
            public void run() {
            }
        };
    }
}
