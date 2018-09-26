package mb.spectrum.view;

import static mb.spectrum.UiUtils.createConfigurableBooleanProperty;
import static mb.spectrum.UiUtils.createConfigurableColorProperty;
import static mb.spectrum.UiUtils.createConfigurableDoubleProperty;
import static mb.spectrum.UiUtils.createConfigurableIntegerProperty;
import static mb.spectrum.Utils.peakLevel;
import static mb.spectrum.Utils.rmsLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import mb.spectrum.Utils;
import mb.spectrum.prop.ConfigurableBooleanProperty;
import mb.spectrum.prop.ConfigurableColorProperty;
import mb.spectrum.prop.ConfigurableDoubleProperty;
import mb.spectrum.prop.ConfigurableIntegerProperty;
import mb.spectrum.prop.ConfigurableProperty;

public class StereoLevelsLedView extends AbstractView {
	
	public enum Channel {
		LEFT, RIGHT
	}
	
	/* Configuration Properties */
	
	// Requiring reset
	private ConfigurableProperty<Integer> propLedCount;
	
	// Not requiring reset
	private ConfigurableIntegerProperty propMinDbValue;
	private ConfigurableIntegerProperty propClipDbValue;
	private ConfigurableIntegerProperty propMidDbValue;
	private ConfigurableColorProperty propLedColorNormal;
	private ConfigurableColorProperty propLedColorMid;
	private ConfigurableColorProperty propLedColorClip;
	private ConfigurableDoubleProperty propHGapLedRatio;
	private ConfigurableDoubleProperty propVGapLedRatio;
	private ConfigurableDoubleProperty propArcWidthWeight;
	private ConfigurableDoubleProperty propArcHeightWeight;
	private ConfigurableBooleanProperty propRms;
	
	/* Operational Properties */
	private DoubleProperty currentDbLProp, currentDbRProp;
	
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
		propMinDbValue = createConfigurableIntegerProperty(
				keyPrefix + "minDbValue", "Min. dB Value", -100, -10, -24, 1);
		propClipDbValue = createConfigurableIntegerProperty(
				keyPrefix + "clipDbValue", "Clip dB Value", -100, 0, -10, 1);
		propMidDbValue = createConfigurableIntegerProperty(
				keyPrefix + "midDbValue", "Middle dB Value", -100, 0, -20, 1);
		propLedColorNormal = createConfigurableColorProperty(
				keyPrefix + "normalLevelColor", "Normal Level Color", Color.LAWNGREEN);
		propLedColorMid = createConfigurableColorProperty(
				keyPrefix + "middleLevelColor", "Middle Level Color", Color.YELLOW);
		propLedColorClip = createConfigurableColorProperty(
				keyPrefix + "clipLevelColor", "Clip Level Color", Color.RED);
		propHGapLedRatio = createConfigurableDoubleProperty(
				keyPrefix + "horizontalGapLedRatio", "LED Horizontal Gap Ratio", 0.01, 0.5, 0.05, 0.01);
		propVGapLedRatio = createConfigurableDoubleProperty(
				keyPrefix + "verticalGapLedRatio", "LED Vertical Gap Ratio", 0.01, 0.5, 0.05, 0.01);
		propArcWidthWeight = createConfigurableDoubleProperty(
				keyPrefix + "arcWidthWeight", "Arc Width Weight", 0.0, 1.0, 0.1, 0.05);
		propArcHeightWeight = createConfigurableDoubleProperty(
				keyPrefix + "arcHeightWeight", "Arc Height Weight", 0.0, 1.0, 0.1, 0.05);
		propRms = createConfigurableBooleanProperty(
				keyPrefix + "enableRmsMode", "RMS Mode", false);
		
		/* Operational properties */
		currentDbLProp = new SimpleDoubleProperty(propMinDbValue.getProp().get());
		currentDbRProp = new SimpleDoubleProperty(propMinDbValue.getProp().get());
	}
	
	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Arrays.asList(propMinDbValue,
				propClipDbValue,
				propMidDbValue,
				propLedCount,
				propLedColorNormal,
				propLedColorMid,
				propLedColorClip,
				propHGapLedRatio, 
				propVGapLedRatio, 
				propArcHeightWeight, 
				propArcWidthWeight, 
				propRms);
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
		
		// Update operational properties from UI thread
		currentDbLProp.set(currentDbL);
		currentDbRProp.set(currentDbR);
	}
	
	@Override
	protected List<Node> collectNodes() {
		
		List<Node> nodes = new ArrayList<>();
		for (int i = 0; i < propLedCount.getProp().get(); i++) {
			nodes.add(createLed(Channel.LEFT, i));
			nodes.add(createLed(Channel.RIGHT, i));
		}
		return nodes;
	}
	
	private Rectangle createLed(Channel channel, int col) {
		
		Rectangle led = new Rectangle();
		
		led.widthProperty().bind(Bindings.createDoubleBinding(
				() -> {
					int ledCount = propLedCount.getProp().get();
					double parentWidth = getRoot().widthProperty().get();
					double ledGapRatio = propHGapLedRatio.getProp().get();
					double ledGap = (parentWidth / ledCount) * ledGapRatio;
					return parentWidth / ledCount - ledGap - ledGap / ledCount;
				}, propLedCount.getProp(), getRoot().widthProperty(), propHGapLedRatio.getProp()));
		
		led.xProperty().bind(Bindings.createDoubleBinding(
				() -> {
					int ledCount = propLedCount.getProp().get();
					double parentWidth = getRoot().widthProperty().get();
					double ledGapRatio = propHGapLedRatio.getProp().get();
					double ledGap = (parentWidth / ledCount) * ledGapRatio;
					return ledGap + col * (ledGap + led.widthProperty().get());
				}, propLedCount.getProp(), getRoot().widthProperty(), propHGapLedRatio.getProp(), led.widthProperty()));
		
		led.heightProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentHeight = getRoot().heightProperty().get();
					double ledGapRatio = propVGapLedRatio.getProp().get();
					double ledGap = (parentHeight / 2) * ledGapRatio;
					return parentHeight / 2 - ledGap - ledGap / 2 ;
				}, getRoot().heightProperty(), propVGapLedRatio.getProp()));
		
		led.yProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentHeight = getRoot().heightProperty().get();
					double ledGapRatio = propVGapLedRatio.getProp().get();
					double ledGap = (parentHeight / 2) * ledGapRatio;
					return ledGap + channel.ordinal() * (ledGap + led.heightProperty().get());
				}, getRoot().heightProperty(), propVGapLedRatio.getProp(), led.heightProperty()));
		
		led.arcWidthProperty().bind(Bindings.createDoubleBinding(
				() -> led.widthProperty().get() * propArcWidthWeight.getProp().get(), 
				led.widthProperty(), propArcWidthWeight.getProp()));
		
		led.arcHeightProperty().bind(Bindings.createDoubleBinding(
				() -> led.heightProperty().get() * propArcHeightWeight.getProp().get(), 
				led.heightProperty(), propArcHeightWeight.getProp()));
		
		DoubleProperty currentDbProp = 
				Channel.LEFT.equals(channel) ? currentDbLProp : currentDbRProp;
		led.visibleProperty().bind(Bindings.createBooleanBinding(
				() -> {
					double range = Math.abs(propMinDbValue.getProp().get() / propLedCount.getProp().get());
					double min = propMinDbValue.getProp().get() + range * col;
					return currentDbProp.get() > min;
				}, propMinDbValue.getProp(), propLedCount.getProp(), currentDbProp));
		
		
		led.fillProperty().bind(Bindings.createObjectBinding(
				() -> {
					double range = Math.abs(propMinDbValue.getProp().get() / propLedCount.getProp().get());
					double min = propMinDbValue.getProp().get() + range * col;
					
					Color color;
					if(min > propClipDbValue.getProp().get()) {
						color = propLedColorClip.getProp().get();
					} else if(min > propMidDbValue.getProp().get()) {
						color = propLedColorMid.getProp().get();
					} else {
						color = propLedColorNormal.getProp().get();
					}
					
					return color;
				}, propLedColorNormal.getProp(), propLedColorMid.getProp(), 
				propLedColorClip.getProp(), propMinDbValue.getProp(), 
				propLedCount.getProp(), propClipDbValue.getProp(), propMidDbValue.getProp()));
		return led;
	}
}
