package mb.spectrum.gpio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang3.builder.EqualsBuilder;

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
	private BlockingQueue<PinStateChange> queue;
	private List<PinStateChange> list;
	
	public RotaryEncoderHandler(Pin pinA, Pin pinB, RotationListener listener) {
		this.pinA = pinA;
		this.pinB = pinB;
		this.listener = listener;
		queue = new ArrayBlockingQueue<>(1000);
		list = Collections.synchronizedList(new ArrayList<>());
		provisionPins();
		startEventProcessing();
	}
	
	private void provisionPins() {
		GpioModule.getInstance().createDigitalSoftwareDebouncedInputPin(pinA, "Left", this);
		GpioModule.getInstance().createDigitalSoftwareDebouncedInputPin(pinB, "Right", this);
	}
	
	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		queue.offer(new PinStateChange(event.getPin().getName(), event.getState()));
	}
	
	private void startEventProcessing() {
		new Thread(new Runnable() {
			public void run() {
				while(true) {
					PinStateChange change;
					try {
						change = queue.take();
						list.add(change);
						decode();
					} catch (InterruptedException e) {
					}
				}
			}
		}).start();
	}
	
	private void decode() {
		Direction direction = EncoderMatchingUtil.match(list);
		if(direction != null) {
			list.clear();
			
			if(listener != null) {
				listener.rotated(direction);
			}
		}
	}
	
	public interface RotationListener {
		void rotated(Direction direction);
	}
	
	public static class PinStateChange {
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

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof PinStateChange)) {
				return false;
			}
	        if (obj == this) {
	            return true;
	        }
	        
	        PinStateChange other = (PinStateChange) obj;
	        return new EqualsBuilder().
	        		append(pinName, other.getPinName()).
	        		append(state, other.getState()).
	        		isEquals();
		}
	}
}
