package mb.spectrum.gpio;

import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import mb.spectrum.gpio.RotaryEncoderHandler.Direction;

public class StageGpioController implements GpioPinListenerDigital {
	
	private GpioModule gpio = GpioModule.getInstance();
	private Stage stage;
	
	public StageGpioController(Stage stage) {
		this.stage = stage;
		setupPins();
		setupRotaryEncoder();
	}
	
	private void setupPins() {
		gpio.createDigitalHardwareDebouncedInputPin(RaspiPin.GPIO_04, "Button A", this);
		gpio.createDigitalHardwareDebouncedInputPin(RaspiPin.GPIO_05, "Button B", this);
	}
	
	private void setupRotaryEncoder() {
		new RotaryEncoderHandler(RaspiPin.GPIO_25, RaspiPin.GPIO_27, 
				new RotaryEncoderHandler.RotationListener() {
			public void rotated(Direction direction) {
				switch (direction) {
				case LEFT:
					fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
							KeyCode.DOWN, false, false, false, false));
					break;

				case RIGHT:
					fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
							KeyCode.UP, false, false, false, false));
					break;
				}
			}
		});
	}
	
	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		switch (event.getState()) {
		case HIGH:
			handleHighEvent(event.getPin());
			break;
			
		case LOW:
			break;
		}
	}
	
	private void handleHighEvent(GpioPin pin) {
		if(RaspiPin.GPIO_04.equals(pin.getPin())) {
			fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
					KeyCode.LEFT, false, false, false, false));
		} else if (RaspiPin.GPIO_05.equals(pin.getPin())) {
			fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
					KeyCode.RIGHT, false, false, false, false));
		}
	}
	
	private void fireEvent(Event event) {
		Platform.runLater(new Runnable() {
			public void run() {
				stage.fireEvent(event);
			}
		});
	}
	
	public void close() {
		gpio.close();
	}
}
