package mb.spectrum.gpio;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.pi4j.io.gpio.PinState;

import mb.spectrum.gpio.RotaryEncoderHandler.Direction;
import mb.spectrum.gpio.RotaryEncoderHandler.PinStateChange;

public class EncoderMatchingUtil {
	
	private static final List<PinStateChange> CLEAN_LEFT = Arrays.asList(
			new PinStateChange("Left", PinState.LOW),
			new PinStateChange("Right", PinState.LOW),
			new PinStateChange("Left", PinState.HIGH),
			new PinStateChange("Right", PinState.HIGH));
	
	private static final List<PinStateChange> CLEAN_RIGHT = Arrays.asList(
			new PinStateChange("Right", PinState.LOW),
			new PinStateChange("Left", PinState.LOW),
			new PinStateChange("Right", PinState.HIGH),
			new PinStateChange("Left", PinState.HIGH));
	
	private static final List<PinStateChange> DIRTY_LEFT = Arrays.asList(
			new PinStateChange("Left", PinState.LOW));
	
	private static final List<PinStateChange> DIRTY_RIGHT = Arrays.asList(
			new PinStateChange("Right", PinState.LOW));
	
	public static Direction match(List<PinStateChange> list) {
		Direction direction = matchClean(list);
		if(direction == null) {
			direction = matchDirty(list);
		}
		return direction;
	}
	
	private static Direction matchClean(List<PinStateChange> list) {
		Direction direction = null;
		if(CLEAN_RIGHT.equals(list)) {
			direction = Direction.RIGHT;
		} else if(CLEAN_LEFT.equals(list)) {
			direction = Direction.LEFT;
		}
		return direction;
	}
	
	private static Direction matchDirty(List<PinStateChange> list) {
		Direction direction = null;
		if(list.size() > 4) {
			if(Collections.indexOfSubList(list, DIRTY_RIGHT) == 0) {
				direction = Direction.RIGHT;
			} else if(Collections.indexOfSubList(list, DIRTY_LEFT) == 0) {
				direction = Direction.LEFT;
			}
		}
		return direction;
	}

}
