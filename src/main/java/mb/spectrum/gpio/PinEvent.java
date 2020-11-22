package mb.spectrum.gpio;

import com.pi4j.io.gpio.PinState;

public class PinEvent {
    
    private String pinName;
    private PinState pinState;
    private long timeStamp;
    
    public PinEvent(String pinName, PinState pinState, long timeStamp) {
        this.pinName = pinName;
        this.pinState = pinState;
        this.timeStamp = timeStamp;
    }

    public String getPinName() {
        return pinName;
    }

    public PinState getPinState() {
        return pinState;
    }
    
    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public String toString() {
        return pinName + " : " + pinState + " : " + timeStamp;
    }
    
    public static PinEvent createEmpty() {
        return new PinEvent("", null, -1);
    }
    
    
}
