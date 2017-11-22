package mb.spectrum.gpio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class RotaryEncoderHandler implements GpioPinListenerDigital {
	
	public enum Direction {
		LEFT, RIGHT
	}
	
	private Pin pinA, pinB;
	private RotationListener listener;
	private List<PinStateChange> states;
	
	public RotaryEncoderHandler(Pin pinA, Pin pinB, RotationListener listener) {
		this.pinA = pinA;
		this.pinB = pinB;
		this.listener = listener;
		states = Collections.synchronizedList(new ArrayList<>(4));
		provisionPins();
	}
	
	private void provisionPins() {
		GpioModule.getInstance().createDigitalSoftwareDebouncedInputPin(pinA, "Left", this);
		GpioModule.getInstance().createDigitalSoftwareDebouncedInputPin(pinB, "Right", this);
	}
	
	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		addStateChangeAndDecode(new PinStateChange(event.getPin().getName(), event.getState()));
	}
	
	private void addStateChangeAndDecode(PinStateChange state) {
		states.add(0, state);
		if(states.size() >= 4) {
			if(PinState.LOW.equals(states.get(0).getState()) && 
					PinState.LOW.equals(states.get(1).getState()) && 
					PinState.HIGH.equals(states.get(2).getState()) && 
					PinState.HIGH.equals(states.get(3).getState())) {
				if("Left".equals(states.get(0).getPinName()) && 
						"Right".equals(states.get(1).getPinName()) && 
						"Left".equals(states.get(2).getPinName()) && 
						"Right".equals(states.get(3).getPinName())) {
					if(listener != null) {
						listener.rotated(Direction.LEFT);
					}
				} else 	if("Right".equals(states.get(0).getPinName()) && 
						"Left".equals(states.get(1).getPinName()) && 
						"Right".equals(states.get(2).getPinName()) && 
						"Left".equals(states.get(3).getPinName())) {
					if(listener != null) {
						listener.rotated(Direction.RIGHT);
					}
				}
			}
			states.clear();
		}
	}
	
	public interface RotationListener {
		void rotated(Direction direction);
	}
	
	private class PinStateChange {
		private String pinName;
		private PinState state;
		
		public PinStateChange(String pinName, PinState state) {
			this.pinName = pinName;
			this.state = state;
		}

		public String getPinName() {
			return pinName;
		}

		public PinState getState() {
			return state;
		}
	}
}
