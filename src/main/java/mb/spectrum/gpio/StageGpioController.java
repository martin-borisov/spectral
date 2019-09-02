package mb.spectrum.gpio;

import static mb.spectrum.gpio.StageGpioController.ButtonState.PRESSED;
import static mb.spectrum.gpio.StageGpioController.ButtonState.RELEASED;

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
import mb.spectrum.UiUtils;
import mb.spectrum.gpio.RotaryEncoderHandler.Direction;

public class StageGpioController implements GpioPinListenerDigital {
    
	public enum ButtonState {
		PRESSED, RELEASED
	}
	
	private GpioModule gpio = GpioModule.getInstance();
	private Stage stage;
	private volatile Direction rotaryADirectionFlag, rotaryBDirectionFlag;
	private ButtonState buttonAState = RELEASED, buttonBState = RELEASED;
	private Timer bothButtonsHoldTimer, buttonBHoldTimer;
	private boolean buttonBHoldEventWasJustExecuted;
	
	public StageGpioController(Stage stage) {
		this.stage = stage;
		bothButtonsHoldTimer = null;
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
					fireUpDownRotaryTurnEvent(rotaryADirectionFlag);
					rotaryADirectionFlag = null;
				}
				
				if(rotaryBDirectionFlag != null) {
					fireLeftRightRotaryTurnEvent(rotaryBDirectionFlag);
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
			
			buttonAState = RELEASED;
			if(buttonBState == RELEASED) {
				onButtonAReleased();
			}
			cancelBothButtonsPressedTimer();
		} else if (RaspiPin.GPIO_00.equals(pin.getPin())) {
			
			buttonBState = RELEASED;
			if(buttonAState == RELEASED) {
				onButtonBReleased();
			}
			cancelBothButtonsPressedTimer();
		}
	}

	private void handleLowEvent(GpioPin pin) {
		if(RaspiPin.GPIO_23.equals(pin.getPin())) {
			
			buttonAState = PRESSED;
			if(buttonBState == RELEASED) {
				onButtonAPressed();
			} else {
				onBothButtonsPressed();
			}
		} else if (RaspiPin.GPIO_00.equals(pin.getPin())) {
			
			buttonBState = PRESSED;
			if(buttonAState == RELEASED) {
				onButtonBPressed();
			} else {
				onBothButtonsPressed();
			}
		}
	}
	
	private void onButtonAPressed() {
	}
	
	private void onButtonAReleased() {
		triggerKeyPress(KeyCode.ENTER, true);
	}
	
	private void onButtonBPressed() {
		buttonBHoldTimer = new Timer(true);
		buttonBHoldTimer.schedule(new TimerTask() {
			public void run() {
				triggerKeyPress(KeyCode.SPACE, true, false);
				buttonBHoldEventWasJustExecuted = true;
			}
		}, 2000);
	}
	
	private void onButtonBReleased() {
		cancelButtonBHoldTimer();
		if(!buttonBHoldEventWasJustExecuted) {
			triggerKeyPress(KeyCode.SPACE, false);
		} else {
			buttonBHoldEventWasJustExecuted = false;
		}
	}
	
	private void onBothButtonsPressed() {
		cancelButtonBHoldTimer();
		bothButtonsHoldTimer = new Timer(true);
		bothButtonsHoldTimer.schedule(new TimerTask() {
			public void run() {
				Platform.runLater(new Runnable() {
					public void run() {
						UiUtils.createAndShowShutdownPrompt(stage, true);
					}
				});
			}
		}, 3000);
	}
	
	private void cancelBothButtonsPressedTimer() {
		if(bothButtonsHoldTimer != null) {
			bothButtonsHoldTimer.cancel();
			bothButtonsHoldTimer.purge();
			bothButtonsHoldTimer = null;
		}
	}
	
	private void cancelButtonBHoldTimer() {
		if(buttonBHoldTimer != null) {
			buttonBHoldTimer.cancel();
			buttonBHoldTimer.purge();
			buttonBHoldTimer = null;
		}
	}
	
	private void triggerKeyPress(KeyCode code, boolean focused) {
		triggerKeyPress(code, false, focused);
	}
	
	private void triggerKeyPress(KeyCode code, boolean controlDown, boolean focused) {
		KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, null, null, code, false, controlDown, 
		        false, false);
		
		// Check if a button is currently in focus
		// as we want to be able to dispatch the event directly to it and not the stage
		if(focused && 
				stage.getScene().focusOwnerProperty().get() != null) {
			fireFocusedControlEvent(event);
		} else {
			fireStageEvent(event);
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
		
		// Check if a slider is currently in focus
		// as we want to be able to dispatch the event directly to it and not the stage
		Node focusNode = stage.getScene().focusOwnerProperty().get();
		if(focusNode != null) {
			KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
					Direction.RIGHT.equals(direction) ? KeyCode.UP : KeyCode.DOWN, 
					        false, false, false, false);
			
			fireFocusedControlEvent(event);
		} else {
			
			// Shift down set for special list property handling
			KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
					Direction.RIGHT.equals(direction) ? KeyCode.UP : KeyCode.DOWN, 
					        true, false, false, false);
			
			fireStageEvent(event);
		}
	}
	
	private void fireLeftRightRotaryTurnEvent(Direction direction) {
		KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, null, null, 
				Direction.RIGHT.equals(direction) ? KeyCode.RIGHT : KeyCode.LEFT, 
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
	
	public void close() {
		gpio.close();
	}
}
