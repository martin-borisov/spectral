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
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
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
	
	private static final double LABEL_WIDTH_RATIO = 0.05;
	private static final Color OFF_COLOR = Color.rgb(15, 15, 20);
	
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
		propLedCount.addUpdateFinishedListener((obs, oldVal, newVal) -> {
			if(newVal != oldVal) {
				reset();
			}
		});
		
		// Not requiring reset
		propMinDbValue = createConfigurableIntegerProperty(
				keyPrefix + "minDbValue", "Min. dB Value", -100, -10, -24, 1, "dB");
		propClipDbValue = createConfigurableIntegerProperty(
				keyPrefix + "clipDbValue", "Clip dB Value", -100, 0, -10, 1, "dB");
		propMidDbValue = createConfigurableIntegerProperty(
				keyPrefix + "midDbValue", "Middle dB Value", -100, 0, -20, 1, "dB");
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
		nodes.add(createLabel(Channel.LEFT));
		nodes.add(createLabel(Channel.RIGHT));
		
		for (int i = 0; i < propLedCount.getProp().get(); i++) {
			nodes.add(createLed(Channel.LEFT, i));
			nodes.add(createLed(Channel.RIGHT, i));
		}
		return nodes;
	}
	
	private Node createLabel(Channel channel) {
		Label label = new Label(channel == Channel.LEFT ? "L" : "R");
		
		label.textFillProperty().bind(propLedColorNormal.getProp());
		
		label.layoutXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					double labelAreaWidth = parentWidth * LABEL_WIDTH_RATIO;
					return labelAreaWidth / 2 - label.widthProperty().get() / 2;
				}, getRoot().widthProperty(), label.widthProperty()));
		
		label.layoutYProperty().bind(
				getRoot().heightProperty().multiply(channel == Channel.LEFT ? 0.25 : 0.75)
					.subtract(label.heightProperty().divide(2)));
		
		label.layoutYProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentHeight = getRoot().heightProperty().get();
					double ledGap = (parentHeight / 2) * propVGapLedRatio.getProp().get();
					double ledHalfHeight = (parentHeight / 2 - ledGap - ledGap / 2) / 2;
					double labelHalfHeight = label.heightProperty().get() / 2;
					return channel == Channel.LEFT ? (ledGap + ledHalfHeight - labelHalfHeight) : 
						(parentHeight - ledGap - ledHalfHeight - labelHalfHeight);
				}, getRoot().heightProperty(), propVGapLedRatio.getProp(), label.heightProperty()));
		
		bindFontSizeToParentWidth(label, LABEL_WIDTH_RATIO, "Helvetica");
		label.setCache(true);
		return label;
	}
	
	private Rectangle createLed(Channel channel, int col) {
		
		Rectangle led = new Rectangle();
		
		led.widthProperty().bind(Bindings.createDoubleBinding(
				() -> {
					int ledCount = propLedCount.getProp().get();
					double parentWidth = getRoot().widthProperty().get();
					double ledAreaWidth = parentWidth - parentWidth * LABEL_WIDTH_RATIO * 2;
					double ledGap = (ledAreaWidth / ledCount) * propHGapLedRatio.getProp().get();
					return ledAreaWidth / ledCount - ledGap - ledGap / ledCount;
				}, propLedCount.getProp(), getRoot().widthProperty(), propHGapLedRatio.getProp()));
		
		led.xProperty().bind(Bindings.createDoubleBinding(
				() -> {
					int ledCount = propLedCount.getProp().get();
					double parentWidth = getRoot().widthProperty().get();
					double labelAreaWidth = parentWidth * LABEL_WIDTH_RATIO;
					double ledAreaWidth = parentWidth - labelAreaWidth * 2;
					double ledGap = (ledAreaWidth / ledCount) * propHGapLedRatio.getProp().get();
					return labelAreaWidth + ledGap + col * (ledGap + led.widthProperty().get());
				}, propLedCount.getProp(), getRoot().widthProperty(), propHGapLedRatio.getProp(), led.widthProperty()));
		
		led.heightProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentHeight = getRoot().heightProperty().get();
					double ledGap = (parentHeight / 2) * propVGapLedRatio.getProp().get();
					return parentHeight / 2 - ledGap - ledGap / 2 ;
				}, getRoot().heightProperty(), propVGapLedRatio.getProp()));
		
		led.yProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentHeight = getRoot().heightProperty().get();
					double ledGap = (parentHeight / 2) * propVGapLedRatio.getProp().get();
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
					
					return currentDbProp.get() > min ? color : OFF_COLOR;
				}, propLedColorNormal.getProp(), propLedColorMid.getProp(), 
				propLedColorClip.getProp(), propMinDbValue.getProp(), 
				propLedCount.getProp(), propClipDbValue.getProp(), propMidDbValue.getProp(), currentDbProp));
		return led;
	}
	
	/* Utilities */
	private void bindFontSizeToParentWidth(Label label, double ratio, String family) {
		label.fontProperty().bind(Bindings.createObjectBinding(
				() -> {
					return Font.font(family, getRoot().widthProperty().get() * ratio);
				}, getRoot().widthProperty()));
	}
}
