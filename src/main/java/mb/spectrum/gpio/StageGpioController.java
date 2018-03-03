package mb.spectrum.gpio;

import java.util.Timer;
import java.util.TimerTask;

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
import mb.spectrum.SystemUtils;
import mb.spectrum.gpio.RotaryEncoderHandler.Direction;

public class StageGpioController implements GpioPinListenerDigital {
	
	private GpioModule gpio = GpioModule.getInstance();
	private Stage stage;
	private volatile Direction rotaryADirectionFlag, rotaryBDirectionFlag;
	private boolean buttonAPressed, buttonBPressed;
	private Timer bothButtonsPressedTimer;
	
	public StageGpioController(Stage stage) {
		this.stage = stage;
		bothButtonsPressedTimer = null;
		setupPins();
		setupRotaryEncoder();
		setupRotaryEncoderPollThread();
	}
	
	private void setupPins() {
		gpio.createDigitalSoftwareDebouncedInputPin(RaspiPin.GPIO_23, "Button A", this);
		gpio.createDigitalSoftwareDebouncedInputPin(RaspiPin.GPIO_00, "Button B", this);
	}
	
	private void setupRotaryEncoder() {
		new RotaryEncoderHandler(RaspiPin.GPIO_25, RaspiPin.GPIO_27, 
				new RotaryEncoderHandler.RotationListener() {
			public void rotated(Direction direction) {
				rotaryADirectionFlag = direction;
			}
		});
		new RotaryEncoderHandler(RaspiPin.GPIO_02, RaspiPin.GPIO_03, 
				new RotaryEncoderHandler.RotationListener() {
			public void rotated(Direction direction) {
				rotaryBDirectionFlag = direction;
			}
		});
	}
	
	private void setupRotaryEncoderPollThread() {
		
		// The purpose of this is to prevent too frequent switches of views
		// as this is quite a CPU intensive task that might slow down a RPi for example
		new Timer(true).schedule(new TimerTask() {
			public void run() {
				
				if(rotaryADirectionFlag != null) {
					fireLeftRightRotaryTurnEvent(rotaryADirectionFlag);
					rotaryADirectionFlag = null;
				}
				
				if(rotaryBDirectionFlag != null) {
					fireUpDownRotaryTurnEvent(rotaryBDirectionFlag);
					rotaryBDirectionFlag = null;
				}
			}
		}, 100, 100);
	}
	
	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		switch (event.getState()) {
		case HIGH:
			handleHighEvent(event.getPin());
			break;
			
		case LOW:
			handleLowEvent(event.getPin());
			break;
		}
	}
	
	
	private void handleHighEvent(GpioPin pin) {
		if(RaspiPin.GPIO_23.equals(pin.getPin())) {
			if(!buttonBPressed) {
				fireStageEvent(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
						KeyCode.ESCAPE, false, false, false, false));
			}
			cancelShutdownTimer();
			buttonAPressed = false;
		} else if (RaspiPin.GPIO_00.equals(pin.getPin())) {
			if(!buttonAPressed) {
				fireStageEvent(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
						KeyCode.SPACE, false, false, false, false));
			}
			cancelShutdownTimer();
			buttonBPressed = false;
		}
	}

	private void handleLowEvent(GpioPin pin) {
		if(RaspiPin.GPIO_23.equals(pin.getPin())) {
			buttonAPressed = true;
			if(buttonBPressed) {
				onBothButtonsPressed();
			}
		} else if (RaspiPin.GPIO_00.equals(pin.getPin())) {
			buttonBPressed = true;
			if(buttonAPressed) {
				onBothButtonsPressed();
			}
		}
	}
	
	// TODO Similarly create onButtonPressed and onButtonReleased handlers
	private void onBothButtonsPressed() {
		bothButtonsPressedTimer = new Timer(false);
		bothButtonsPressedTimer.schedule(new TimerTask() {
			public void run() {
				SystemUtils.shutdown();
			}
		}, 5000);
	}
	
	private void cancelShutdownTimer() {
		if(bothButtonsPressedTimer != null) {
			bothButtonsPressedTimer.cancel();
			bothButtonsPressedTimer = null;
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
	
	private void fireUpDownRotaryTurnEvent(Direction direction) {
		KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
				Direction.RIGHT.equals(direction) ? KeyCode.UP : KeyCode.DOWN, 
						false, false, false, false);
		
		// Check if a slider is currently in focus
		// as we want to be able to dispatch the event directly to it and not the stage
		Node focusNode = stage.getScene().focusOwnerProperty().get();
		if(focusNode != null) {
			fireFocusedControlEvent(event);
		} else {
			fireStageEvent(event);
		}
	}
	
	private void fireLeftRightRotaryTurnEvent(Direction direction) {
		KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
				Direction.RIGHT.equals(direction) ? KeyCode.RIGHT : KeyCode.LEFT, 
						false, false, false, false);
		fireStageEvent(event);
	}
	
	public void close() {
		gpio.close();
	}
}
