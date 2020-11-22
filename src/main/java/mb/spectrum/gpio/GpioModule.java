package mb.spectrum.gpio;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class GpioModule {

	private static GpioModule ref;
	private GpioController gpio;
	
	private GpioModule() {
		gpio = GpioFactory.getInstance();
	}
	
	public static GpioModule getInstance() {
		synchronized (GpioModule.class) {
			if(ref == null) {
				ref = new GpioModule();
			}
		}
		return ref;
	}
	
    public GpioPinDigitalInput createDigitalHardwareDebouncedInputPin(Pin pin, String name, GpioPinListenerDigital listener) {
    	GpioPinDigitalInput input = gpio.provisionDigitalInputPin(
    			pin, name, PinPullResistance.PULL_DOWN);
    	input.addListener(listener);
    	input.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
    	return input;
    }
    
    public GpioPinDigitalInput createDigitalSoftwareDebouncedInputPin(Pin pin, String name, GpioPinListenerDigital listener) {
    	return createDigitalSoftwareDebouncedInputPin(pin, name, 200, listener);
    }
    
    /**
     * Creates a pin and sets a debounce delay for a specific rotary encoder
     * @param pin {@link Pin} instance
     * @param name Name of the pin
     * @param listener {@link GpioPinListenerDigital} listsner for pin changes
     * @return {@link GpioPinDigitalInput}
     */
    public GpioPinDigitalInput createDigitalSoftwareDebouncedInputPinForRotary(Pin pin, String name, GpioPinListenerDigital listener) {
    	return createDigitalSoftwareDebouncedInputPin(pin, name, 10, listener);
    }
    
    public GpioPinDigitalInput createDigitalSoftwareDebouncedInputPin(Pin pin, String name, int delay, GpioPinListenerDigital listener) {
    	GpioPinDigitalInput input = gpio.provisionDigitalInputPin(
    			pin, name, PinPullResistance.PULL_UP);
    	input.setDebounce(delay);
    	input.addListener(listener);
    	input.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
    	return input;
    }
    
    public void close() {
    	gpio.shutdown();
    }
}
