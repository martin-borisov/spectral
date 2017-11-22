package mb.spectrum.view;

import static mb.spectrum.UiUtils.createConfigurableBooleanProperty;
import static mb.spectrum.UiUtils.createConfigurableDoubleProperty;
import static mb.spectrum.UiUtils.createConfigurableIntegerProperty;
import static mb.spectrum.Utils.peakLevel;
import static mb.spectrum.Utils.rmsLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.scene.Node;
import mb.spectrum.Utils;
import mb.spectrum.prop.ConfigurableProperty;

public class StereoLevelsLedView extends AbstractView {
	
	private enum Channel {
		LEFT, RIGHT
	}
	
	// Requiring reset
	private ConfigurableProperty<Integer> propLedCount;
	
	// Not requiring reset
	private ConfigurableProperty<Double> propGapLedRatio;
	private ConfigurableProperty<Boolean> propRms;
	
	private double currentDbL, currentDbR;

	@Override
	public String getName() {
		return "Stereo LED Meters";
	}
	
	@Override
	protected void initProperties() {
		
		final String keyPrefix = "stereoLevelsLedView.";
		
		/* Configuration Properties */
		
		// Requiring reset
		propLedCount = createConfigurableIntegerProperty(
				keyPrefix + "ledCount", "Led Count", 2, 1000, 10, 1);
		propLedCount.getProp().addListener((obs, oldVal, newVal) -> {
			if(newVal != oldVal) {
				reset();
			}
		});
		
		// Not requiring reset
		propGapLedRatio = createConfigurableDoubleProperty(
				keyPrefix, "LED Gap Ratio", 0.01, 0.5, 0.02, 0.01);
		propRms = createConfigurableBooleanProperty(
				keyPrefix + "enableRmsMode", "RMS Mode", false);
		
		/* Operational properties */
	}
	
	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Arrays.asList(propLedCount, propRms);
	}
	
	@Override
	public void dataAvailable(float[] left, float[] right) {
		
		float levelLeft = 0, levelRight = 0;
		if(propRms.getProp().get()) {
			levelLeft = rmsLevel(left);
			levelRight = rmsLevel(right);
		} else {
			levelLeft = peakLevel(left);
			levelRight = peakLevel(right);
		}
		
		currentDbL = Utils.toDB(levelLeft);
		currentDbR = Utils.toDB(levelRight);
	}

	@Override
	public void nextFrame() {
		// TODO Auto-generated method stub
	}
	
	@Override
	protected List<Node> collectNodes() {
		
		List<Node> nodes = new ArrayList<>();
		return nodes;
	}
	
	private void createLedRectangle(Channel channel, int col) {
		
		int ledCount = propLedCount.getProp().get();
		
	}
}
