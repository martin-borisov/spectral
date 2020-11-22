package mb.spectrum.gpio;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import javafx.beans.property.SimpleBooleanProperty;

public class RoughRotaryEncoderHandler {
	
	public enum Direction {
		LEFT, RIGHT
	}
	
	private Pin pinA, pinB;
	private RotationListener listener;
	
	public RoughRotaryEncoderHandler(Pin pinA, Pin pinB, RotationListener listener) {
		this.pinA = pinA;
		this.pinB = pinB;
		this.listener = listener;
		provisionPins();
	}
	
	private void provisionPins() {
		SimpleBooleanProperty stateA = new SimpleBooleanProperty(true);
		SimpleBooleanProperty stateB = new SimpleBooleanProperty(true);
		
    	stateA.addListener((obs, oldVal, newVal) -> {
    		if(newVal && stateB.get()) {
    			listener.rotated(Direction.RIGHT);
    		}
    	});
    	stateB.addListener((obs, oldVal, newVal) -> {
       		if(newVal && stateA.get()) {
       			listener.rotated(Direction.LEFT);
    		}
    	});
		
		GpioModule.getInstance().createDigitalSoftwareDebouncedInputPinForRotary(pinA, "Left", new GpioPinListenerDigital() {
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		        stateA.set(event.getState().isHigh());
			}
		});
		GpioModule.getInstance().createDigitalSoftwareDebouncedInputPinForRotary(pinB, "Right", new GpioPinListenerDigital() {
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		        stateB.set(event.getState().isHigh());
			}
		});
	}
	
	public interface RotationListener {
		void rotated(Direction direction);
	}

}
