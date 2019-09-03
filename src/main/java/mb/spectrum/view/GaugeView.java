package mb.spectrum.view;

import static mb.spectrum.Utils.peakLevel;

import java.util.Arrays;
import java.util.List;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.KnobType;
import eu.hansolo.medusa.Gauge.NeedleShape;
import eu.hansolo.medusa.Gauge.NeedleSize;
import eu.hansolo.medusa.Gauge.ScaleDirection;
import eu.hansolo.medusa.Gauge.SkinType;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.TickLabelOrientation;
import eu.hansolo.medusa.TickMarkType;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import mb.spectrum.UiUtils;
import mb.spectrum.Utils;
import mb.spectrum.prop.ConfigurableBooleanProperty;
import mb.spectrum.prop.ConfigurableChoiceProperty;
import mb.spectrum.prop.ConfigurableColorProperty;
import mb.spectrum.prop.ConfigurableDoubleProperty;
import mb.spectrum.prop.ConfigurableIntegerProperty;
import mb.spectrum.prop.ConfigurableProperty;

public class GaugeView extends AbstractMixedChannelView {
	
	private ConfigurableIntegerProperty propMinDbValue, propSensitivity;
	private ConfigurableChoiceProperty propType;
	private ConfigurableChoiceProperty propNeedleSize, propNeedleShape, propLabelOrientation, propKnobType, 
		propMajorTickMarkType, propMediumTickMarkType, propMinorTickMarkType;
	private ConfigurableColorProperty propDialColor, propNeedleColor, propKnobColor, propMajorTickMarkColor,
		propMediumTickMarkColor, propMinorTickMarkColor, propMovingAverageColor;
	private ConfigurableDoubleProperty propMajorTickLength, propMediumTickLength, propMinorTickLength;
	private ConfigurableBooleanProperty propShowMovingAverage;
	
	private String name, propKeyPrefix;
	private boolean mirrored;
	private Gauge gauge;
	private double currentDb;
	private Timeline tl;
	
	public GaugeView(String name, String propKeyPrefix, boolean mirrored) {
		super(true);
		this.name = name;
		this.propKeyPrefix = propKeyPrefix;
		this.mirrored = mirrored;
		tl = new Timeline();
		init();
	}

	@Override
	public String getName() {
		return "Simple Analog Meter - Mono";
	}
	
	@Override
	protected void initProperties() {
		super.initProperties();
		
		final String keyPrefix = propKeyPrefix + ".";
		
		propMinDbValue = UiUtils.createConfigurableIntegerProperty(
				keyPrefix + "minDbValue", "Min. DB Value", -100, -20, -60, 5, "dB");
		propMinDbValue.getProp().addListener((obs, oldVal, newVal) -> {
			reset();
		});
		propSensitivity = UiUtils.createConfigurableIntegerProperty(
				keyPrefix + "sensitivity", "Sensitivity" , 100, 1000, 220, 10, "ms");
		propType = UiUtils.createConfigurableChoiceProperty(
				keyPrefix + "type", "Type", 
				Arrays.asList("HORIZONTAL", "VERTICAL", "QUARTER"), "HORIZONTAL");
		propType.getProp().addListener((obs, oldVal, newVal) -> {
			reset();
		});
		propNeedleSize = UiUtils.createConfigurableChoiceProperty(
				keyPrefix + "needleSize", "Needle Size", NeedleSize.class);
		propNeedleShape = UiUtils.createConfigurableChoiceProperty(
				keyPrefix + "needleShape", "Needle Shape", NeedleShape.class);
		propLabelOrientation = UiUtils.createConfigurableChoiceProperty(
				keyPrefix + "labelOrientation", "Label Orientation", TickLabelOrientation.class);
		propDialColor = UiUtils.createConfigurableColorProperty( 
				keyPrefix + "dialColor", "Dial Color", Color.WHITE);
		propNeedleColor = UiUtils.createConfigurableColorProperty( 
				keyPrefix + "needleColor", "Needle Color", Color.LIGHTGRAY);
		propKnobType = UiUtils.createConfigurableChoiceProperty(
				keyPrefix + "knobType", "Knob Type", Arrays.asList(
						"STANDARD", "FLAT", "METAL", "PLAIN"), "METAL");
		propKnobColor = UiUtils.createConfigurableColorProperty( 
				keyPrefix + "knobColor", "Knob Color", Color.LIGHTGRAY);
		propMajorTickMarkType = UiUtils.createConfigurableChoiceProperty(
				keyPrefix + "majorTickMarkType", "Major Tick Type", TickMarkType.class);
		propMediumTickMarkType = UiUtils.createConfigurableChoiceProperty(
				keyPrefix + "mediumTickMarkType", "Medium Tick Type", TickMarkType.class);
		propMinorTickMarkType = UiUtils.createConfigurableChoiceProperty(
				keyPrefix + "minorTickMarkType", "Minor Tick Type", TickMarkType.class);
		propMajorTickMarkColor = UiUtils.createConfigurableColorProperty( 
				keyPrefix + "majorTickMarkColor", "Maj. Tick Mark Color", Color.RED);
		propMediumTickMarkColor = UiUtils.createConfigurableColorProperty( 
				keyPrefix + "mediumTickMarkColor", "Med. Tick Mark Color", Color.LIGHTGRAY);
		propMinorTickMarkColor = UiUtils.createConfigurableColorProperty( 
				keyPrefix + "minorTickMarkColor", "Min. Tick Mark Color", Color.LIGHTGRAY);
		propMajorTickLength = UiUtils.createConfigurableDoubleProperty(
				keyPrefix + "majorTickLength", "Major Tick Length", 0.1, 1.0, 0.4, 0.1);
		propMediumTickLength = UiUtils.createConfigurableDoubleProperty(
				keyPrefix + "mediumTickLength", "Med. Tick Length", 0.1, 1.0, 0.3, 0.1);
		propMinorTickLength = UiUtils.createConfigurableDoubleProperty(
				keyPrefix + "minorTickLength", "Minor Tick Length", 0.1, 1.0, 0.2, 0.1);
		propShowMovingAverage = UiUtils.createConfigurableBooleanProperty(
				keyPrefix + "showMovingAverage", "Show Moving Average", true);
		propMovingAverageColor = UiUtils.createConfigurableColorProperty( 
				keyPrefix + "movingAverageColor", "Moving Average Color", Color.RED);
		
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Arrays.asList(
				propMinDbValue, 
				propSensitivity,
				propType,
				propNeedleSize,
				propNeedleShape,
				propLabelOrientation,
				propKnobType,
				propDialColor,
				propNeedleColor,
				propKnobColor,
				propMajorTickMarkType,
				propMediumTickMarkType,
				propMinorTickMarkType,
				propMajorTickMarkColor,
				propMediumTickMarkColor,
				propMinorTickMarkColor,
				propMajorTickLength,
				propMediumTickLength,
				propMinorTickLength,
				propShowMovingAverage,
				propMovingAverageColor);
	}
	
	@Override
	public void dataAvailable(float[] data) {
		currentDb = Utils.toDB(peakLevel(data));
		
		// Update indicator
		Platform.runLater(new Runnable() {
			public void run() {
				tl.stop();
				tl.getKeyFrames().clear();
				tl.getKeyFrames().addAll(
						new KeyFrame(Duration.millis(0), new KeyValue(gauge.valueProperty(), gauge.valueProperty().get())),
						new KeyFrame(Duration.millis(propSensitivity.get()), 
								new KeyValue(gauge.valueProperty(), currentDb))
						);
				tl.play();
			}
		});
	}

	@Override
	public void nextFrame() {
	}

	@Override
	protected List<Node> collectNodes() {
		
        gauge = GaugeBuilder.create()
        		.skinType(SkinType.valueOf(propType.get())) // AMP, HORIZONTAL, KPI, LINEAR, MODERN, QUARTER, SECTION, SIMPLE, SPACE_X, TILE_KPI, VERTICAL
        		
        		// Mandatory
                .unit("dB")
                .decimals(0)
                .minValue(propMinDbValue.get())
                .maxValue(0)
                .backgroundPaint(Color.BLACK)
                .valueVisible(false) // Hide current value label
                .angleRange(135) // TODO
                .averagingPeriod(100) // Applicable when averaging is enabled
                
                // TODO Try to set a custom font, which doesn't work for some reason
                .title(name)
                .customFont(Font.loadFont(GaugeView.class.getResource(
                		"/AlexBrush-Regular.ttf").toExternalForm(), 10))
                .customFontEnabled(true)
                
                // Covered
                //.unitColor(Color.WHITE)
                //.titleColor(Color.WHITE)
                //.majorTickMarkColor(Color.WHITE)
                //.mediumTickMarkColor(Color.RED)
                //.minorTickMarkColor(Color.WHITE)
                //.minorTickMarkType(TickMarkType.LINE)
                //.needleSize(NeedleSize.THIN)
                //.needleShape(NeedleShape.ANGLED)
                //.tickLabelColor(Color.WHITE)
                //.knobType(KnobType.METAL)
                //.tickLabelOrientation(TickLabelOrientation.HORIZONTAL)
 
                
                // Applicable to only some gauge types
                //.maxMeasuredValueVisible(true) // Doesn't have effect
                //.minMeasuredValueVisible(true) // Doesn't have effect
                //.sectionsVisible(true)
                //.sections(new Section(0, 33, Color.rgb(34, 180, 11)),
                //          new Section(33, 66, Color.rgb(255, 146, 0)),
                //          new Section(66, 100, Color.rgb(255, 0, 39)))
                
                // TODO To be explored
                //.thresholdVisible(true)
                //.threshold(-10)
                
                .build();
        
		// Workaround for color properties where the sliders don't get the focus
        gauge.setFocusTraversable(false);
        
        gauge.minWidthProperty().bind(getRoot().widthProperty());
        gauge.minHeightProperty().bind(getRoot().heightProperty());
        
        // Configurable properties
        gauge.unitColorProperty().bind(propDialColor.getProp());
        gauge.titleColorProperty().bind(propDialColor.getProp());
        gauge.tickLabelColorProperty().bind(propDialColor.getProp());
        gauge.needleColorProperty().bind(propNeedleColor.getProp());
        gauge.needleSizeProperty().bind(Bindings.createObjectBinding(
        		() -> {
        			return NeedleSize.valueOf(propNeedleSize.get());
        		}, propNeedleSize.getProp()));
        gauge.needleShapeProperty().bind(Bindings.createObjectBinding(
        		() -> {
        			return NeedleShape.valueOf(propNeedleShape.get());
        		}, propNeedleShape.getProp()));
        gauge.tickLabelOrientationProperty().bind(Bindings.createObjectBinding(
        		() -> {
        			return TickLabelOrientation.valueOf(propLabelOrientation.get());
        		}, propLabelOrientation.getProp()));
        gauge.knobTypeProperty().bind(Bindings.createObjectBinding(
        		() -> {
        			return KnobType.valueOf(propKnobType.get());
        		}, propKnobType.getProp()));
        gauge.knobColorProperty().bind(propKnobColor.getProp());
        gauge.majorTickMarkTypeProperty().bind(Bindings.createObjectBinding(
        		() -> {
        			return TickMarkType.valueOf(propMajorTickMarkType.get());
        		}, propMajorTickMarkType.getProp()));
        gauge.mediumTickMarkTypeProperty().bind(Bindings.createObjectBinding(
        		() -> {
        			return TickMarkType.valueOf(propMediumTickMarkType.get());
        		}, propMediumTickMarkType.getProp()));
        gauge.minorTickMarkTypeProperty().bind(Bindings.createObjectBinding(
        		() -> {
        			return TickMarkType.valueOf(propMinorTickMarkType.get());
        		}, propMinorTickMarkType.getProp()));
        gauge.majorTickMarkColorProperty().bind(propMajorTickMarkColor.getProp());
        gauge.mediumTickMarkColorProperty().bind(propMediumTickMarkColor.getProp());
        gauge.minorTickMarkColorProperty().bind(propMinorTickMarkColor.getProp());
        gauge.majorTickMarkLengthFactorProperty().bind(propMajorTickLength.getProp());
        gauge.mediumTickMarkLengthFactorProperty().bind(propMediumTickLength.getProp());
        gauge.minorTickMarkLengthFactorProperty().bind(propMinorTickLength.getProp());
        gauge.averageVisibleProperty().bind(propShowMovingAverage.getProp());
        gauge.averagingEnabledProperty().bind(propShowMovingAverage.getProp());
        gauge.averageColorProperty().bind(propMovingAverageColor.getProp());
        
		if(mirrored) {
			if("VERTICAL".equals(propType.get())) {
				gauge.setKnobPosition(Pos.CENTER_LEFT);
				gauge.setScaleDirection(ScaleDirection.COUNTER_CLOCKWISE);
			} else if("QUARTER".equals(propType.get())) {
				gauge.setKnobPosition(Pos.BOTTOM_LEFT);
				gauge.setScaleDirection(ScaleDirection.COUNTER_CLOCKWISE);
			}
		}
		
		// #
		gauge.setMinorTickMarksVisible(false);
		// #
		
        
		return Arrays.asList(gauge);
	}
}
