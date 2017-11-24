package mb.spectrum.gpio;

import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
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
				fireRotaryTurnEvent(direction);
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
			fireStageEvent(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
					KeyCode.LEFT, false, false, false, false));
		} else if (RaspiPin.GPIO_05.equals(pin.getPin())) {
			fireStageEvent(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
					KeyCode.RIGHT, false, false, false, false));
		}
	}
	
	private void fireStageEvent(Event event) {
		Platform.runLater(new Runnable() {
			public void run() {
				stage.fireEvent(event);
			}
		});
	}
	
	private void fireFocusedControlEvent(Event event) {
		Platform.runLater(new Runnable() {
			public void run() {
				Node focusNode = stage.getScene().focusOwnerProperty().get();
				if(focusNode != null) {
					focusNode.fireEvent(event);
				}
			}
		});
	}
	
	private void fireRotaryTurnEvent(Direction direction) {
		KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
				Direction.RIGHT.equals(direction) ? KeyCode.UP : KeyCode.DOWN, 
						false, false, false, false);
		Node focusNode = stage.getScene().focusOwnerProperty().get();
		if(focusNode != null) {
			fireFocusedControlEvent(event);
		} else {
			fireStageEvent(event);
		}
	}
	
	public void close() {
		gpio.close();
	}
}
