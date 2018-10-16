package mb.spectrum.view;

import static mb.spectrum.Utils.peakLevel;

import java.util.Arrays;
import java.util.List;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.KnobType;
import eu.hansolo.medusa.Gauge.NeedleShape;
import eu.hansolo.medusa.Gauge.NeedleSize;
import eu.hansolo.medusa.Gauge.SkinType;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.Section;
import eu.hansolo.medusa.TickMarkType;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import mb.spectrum.UiUtils;
import mb.spectrum.Utils;
import mb.spectrum.prop.ConfigurableChoiceProperty;
import mb.spectrum.prop.ConfigurableColorProperty;
import mb.spectrum.prop.ConfigurableIntegerProperty;
import mb.spectrum.prop.ConfigurableProperty;

public class GaugeView extends AbstractView {
	
	private ConfigurableIntegerProperty propMinDbValue;
	private ConfigurableChoiceProperty propType, propNeedleSize, propKnobType;
	private ConfigurableColorProperty propDialColor, propNeedleColor, propKnobColor, 
		propMediumTickMarkColor, propMinorTickMarkColor;
	
	private Gauge gauge;
	private double currentDbL, currentDbR;
	private Timeline tl;
	
	public GaugeView() {
		super(true);
		tl = new Timeline();
		init();
	}

	@Override
	public String getName() {
		return "Gauge View";
	}
	
	@Override
	protected void initProperties() {
		super.initProperties();
		
		final String keyPrefix = "gaugeView.";
		
		propMinDbValue = UiUtils.createConfigurableIntegerProperty(
				keyPrefix + "minDbValue", "Min. DB Value", -100, -20, -60, 5);
		propMinDbValue.getProp().addListener((obs, oldVal, newVal) -> {
			reset();
		});
		propType = UiUtils.createConfigurableChoiceProperty(
				keyPrefix + "type", "Type", Arrays.asList(
						"AMP", "HORIZONTAL", "INDICATOR", "KPI", "LINEAR", "MODERN", 
						"QUARTER", "SECTION", "SIMPLE", "SPACE_X", "TILE_KPI", "VERTICAL"), "INDICATOR");
		propType.getProp().addListener((obs, oldVal, newVal) -> {
			reset();
		});
		propNeedleSize = UiUtils.createConfigurableChoiceProperty(
				keyPrefix + "needleSize", "Needle Size", Arrays.asList(
						"STANDARD", "THICK", "THIN"), "STANDARD");
		propDialColor = UiUtils.createConfigurableColorProperty( 
				keyPrefix + "dialColor", "Dial Color", Color.WHITE);
		propNeedleColor = UiUtils.createConfigurableColorProperty( 
				keyPrefix + "needleColor", "Needle Color", Color.LIGHTGRAY);
		propKnobType = UiUtils.createConfigurableChoiceProperty(
				keyPrefix + "knobType", "Knob Type", Arrays.asList(
						"STANDARD", "FLAT", "METAL", "PLAIN"), "METAL");
		propKnobColor = UiUtils.createConfigurableColorProperty( 
				keyPrefix + "knobColor", "Knob Color", Color.LIGHTGRAY);
		propMediumTickMarkColor = UiUtils.createConfigurableColorProperty( 
				keyPrefix + "mediumTickMarkColor", "Med. Tick Mark Color", Color.LIGHTGRAY);
		propMinorTickMarkColor = UiUtils.createConfigurableColorProperty( 
				keyPrefix + "minorTickMarkColor", "Min. Tick Mark Color", Color.LIGHTGRAY);
		
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Arrays.asList(
				propMinDbValue, 
				propType,
				propDialColor,
				propNeedleColor,
				propNeedleSize,
				propKnobType,
				propKnobColor,
				propMediumTickMarkColor,
				propMinorTickMarkColor);
	}

	@Override
	public void dataAvailable(float[] left, float[] right) {
		currentDbL = Utils.toDB(peakLevel(left));
		
		// Update indicator
		Platform.runLater(new Runnable() {
			public void run() {
				tl.stop();
				tl.getKeyFrames().clear();
				tl.getKeyFrames().addAll(
						new KeyFrame(Duration.millis(0), new KeyValue(gauge.valueProperty(), gauge.valueProperty().get())),
						new KeyFrame(Duration.millis(220), 
								new KeyValue(gauge.valueProperty(), currentDbL))
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
                //.skinType(SkinType.INDICATOR) // AMP, HORIZONTAL, KPI, LINEAR, MODERN, QUARTER, SECTION, SIMPLE, SPACE_X, TILE_KPI, VERTICAL
        		.skinType(SkinType.valueOf(propType.get()))
                .decimals(0)
                .sectionsVisible(true)
                .sections(new Section(0, 33, Color.rgb(34, 180, 11)),
                          new Section(33, 66, Color.rgb(255, 146, 0)),
                          new Section(66, 100, Color.rgb(255, 0, 39)))
                // #
                .unit("dB")
                //.unitColor(Color.WHITE)
                .minValue(propMinDbValue.get())
                .maxValue(0)
                // #
                .title("Analog Meter") // Title
                //.titleColor(Color.WHITE)
                .valueVisible(false) // Hide current value
                .maxMeasuredValueVisible(true) // Doesn't have effect
                .minMeasuredValueVisible(true) // Doesn't have effect
                .backgroundPaint(Color.BLACK) // -> 
                //.majorTickMarkColor(Color.WHITE) // ->
                //.mediumTickMarkColor(Color.RED) // ->
                //.minorTickMarkColor(Color.WHITE) // ->
                .minorTickMarkType(TickMarkType.LINE) // ->
                //.needleSize(NeedleSize.THIN) // ->
                .needleShape(NeedleShape.ANGLED) // ->
                //.tickLabelColor(Color.WHITE) // ->
                //.knobType(KnobType.METAL) // ->
                //.thresholdVisible(true)
                //.threshold(-10)
                .customFont(Font.loadFont(AnalogMeterView.class.getResource("/AlexBrush-Regular.ttf").toExternalForm(), 10))
                .customFontEnabled(true)
                // #
                .averageVisible(true)
                .averagingEnabled(true)
                .averagingPeriod(100)
                .angleRange(135) // ->
                // #
                .build();
        
		// Workaround for color properties where the sliders don't get the focus
        gauge.setFocusTraversable(false);
        
        gauge.minWidthProperty().bind(getRoot().widthProperty());
        gauge.minHeightProperty().bind(getRoot().heightProperty());
        
        // Configurable properties
        gauge.unitColorProperty().bind(propDialColor.getProp());
        gauge.titleColorProperty().bind(propDialColor.getProp());
        gauge.majorTickMarkColorProperty().bind(propDialColor.getProp());
        gauge.tickLabelColorProperty().bind(propDialColor.getProp());
        gauge.needleColorProperty().bind(propNeedleColor.getProp());
        gauge.needleSizeProperty().bind(Bindings.createObjectBinding(
        		() -> {
        			return NeedleSize.valueOf(propNeedleSize.get());
        		}, propNeedleSize.getProp()));
        gauge.knobTypeProperty().bind(Bindings.createObjectBinding(
        		() -> {
        			return KnobType.valueOf(propKnobType.get());
        		}, propKnobType.getProp()));
        gauge.knobColorProperty().bind(propKnobColor.getProp());
        gauge.mediumTickMarkColorProperty().bind(propMediumTickMarkColor.getProp());
        gauge.minorTickMarkColorProperty().bind(propMinorTickMarkColor.getProp());
        
		return Arrays.asList(gauge);
	}

}
