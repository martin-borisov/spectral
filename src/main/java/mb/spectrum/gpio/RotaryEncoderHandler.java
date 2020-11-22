package mb.spectrum.gpio;

import static com.pi4j.io.gpio.PinState.HIGH;
import static com.pi4j.io.gpio.PinState.LOW;

import java.util.LinkedList;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class RotaryEncoderHandler {
	
	public enum Direction {
		LEFT, RIGHT
	}
	
	public static final String PIN_A = "A";
	public static final String PIN_B = "B";
	private static final int MAX_TIME_BETWEEN_NEAR_PINS = 10;
	
	// This value is tuned for a specific rotary encoder
	private static final long MIN_DIR_SWITCH_INTERVAL_MS = 200;
	
	private Pin pinA, pinB;
	private RotationListener listener;
	private GpioPinListenerDigital pinListener;
	private LinkedList<PinEvent> queue;
	private long lastRotationTime;
	private Direction lastRoationDir;

    public RotaryEncoderHandler(Pin pinA, Pin pinB, RotationListener listener) {
        this.pinA = pinA;
        this.pinB = pinB;
        this.listener = listener;
        initQueue();
        provisionPins();
    }
    
    public void setPinListener(GpioPinListenerDigital pinListener) {
        this.pinListener = pinListener;
    }
    
    private void initQueue() {
        queue = new LinkedList<>();
        for (int i = 0; i < 4; i++) {
            queue.add(PinEvent.createEmpty());
        }
    }
	
	private void provisionPins() {
	    
	    GpioPinListenerDigital digitalPinListener = new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                System.out.println(new PinEvent(event.getPin().getName(), event.getState(), System.currentTimeMillis()));
                addEventToQueue(new PinEvent(event.getPin().getName(), event.getState(), System.currentTimeMillis()));
                if(pinListener != null) {
                    pinListener.handleGpioPinDigitalStateChangeEvent(event);
                }
            }
        };
	    
		GpioModule.getInstance().createDigitalSoftwareDebouncedInputPinForRotary(pinA, PIN_A, digitalPinListener);
		GpioModule.getInstance().createDigitalSoftwareDebouncedInputPinForRotary(pinB, PIN_B, digitalPinListener);
	}
	
	private synchronized void addEventToQueue(PinEvent event) {
	    queue.removeFirst();
	    queue.add(event);
	    detectAction();
	}
	
	/**
	 * Detects the rotation pattern of a specific rotary encoder
	 */
	private void detectAction() {
        PinEvent e1 = queue.get(0);
        PinEvent e2 = queue.get(1);
        PinEvent e3 = queue.get(2);
        PinEvent e4 = queue.get(3);
        
        /*
        Right: B[UP] -> A[UP] -> A + B[DOWN]
        Left : A[DOWN] -> B[DOWN] -> A + B[UP]
         */
        if(Math.abs(e4.getTimeStamp() - e3.getTimeStamp()) <= MAX_TIME_BETWEEN_NEAR_PINS) {
            if(HIGH.equals(e1.getPinState()) && PIN_B.equals(e1.getPinName()) && 
                HIGH.equals(e2.getPinState()) && PIN_A.equals(e2.getPinName()) && 
                LOW.equals(e3.getPinState()) && 
                LOW.equals(e4.getPinState())) {
            
                tiggerRotated(Direction.RIGHT);
            
            } else if(LOW.equals(e1.getPinState()) && PIN_A.equals(e1.getPinName()) && 
                LOW.equals(e2.getPinState()) && PIN_B.equals(e2.getPinName()) && 
                HIGH.equals(e3.getPinState()) && 
                HIGH.equals(e4.getPinState())) {
            
                tiggerRotated(Direction.LEFT);
            }
        }
	}
	
	private void tiggerRotated(Direction dir) {
	    
	    // Make sure direction can be switched only after a specific amount of time has elapsed
	    if(dir.equals(lastRoationDir) || 
	            (System.currentTimeMillis() - lastRotationTime) > MIN_DIR_SWITCH_INTERVAL_MS) {
	        listener.rotated(dir);
	        lastRoationDir = dir;
	        lastRotationTime = System.currentTimeMillis();
	    }
    }
	
    public interface RotationListener {
        void rotated(Direction direction);
    }
}
