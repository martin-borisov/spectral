package mb.spectrum.view;

import static mb.spectrum.UiUtils.createConfigurableBooleanProperty;
import static mb.spectrum.UiUtils.createConfigurableColorProperty;
import static mb.spectrum.UiUtils.createConfigurableIntegerProperty;
import static mb.spectrum.Utils.map;
import static mb.spectrum.Utils.peakLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import mb.spectrum.UiUtils;
import mb.spectrum.Utils;
import mb.spectrum.prop.ConfigurableBooleanProperty;
import mb.spectrum.prop.ConfigurableColorProperty;
import mb.spectrum.prop.ConfigurableDoubleProperty;
import mb.spectrum.prop.ConfigurableIntegerProperty;
import mb.spectrum.prop.ConfigurableProperty;

public class AnalogMeterView extends AbstractMixedChannelView {
	
	private static final int MIN_DB_ANGLE_DEGREES = 130;
	private static final int MAX_DB_ANGLE_DEGREES = 50;
	private static final double MIN_DB_ANGLE_RAD = Math.toRadians(MIN_DB_ANGLE_DEGREES);
	private static final double MAX_DB_ANGLE_RAD = Math.toRadians(MAX_DB_ANGLE_DEGREES);
	private static final double DIV_LENGTH_RATIO_BIG = 0.02;
	private static final double DIV_LENGTH_RATIO_SMALL = DIV_LENGTH_RATIO_BIG / 3;
	private static final double LINGER_STAY_FACTOR = 0.05;
	private static final double LINGER_ACCELARATION_FACTOR = 1.15;
	private static final double BACK_CIRCLE_RADIUS_TO_SCENE_WIDTH_RATIO = 0.05;
	private static final double ELEMENT_WIDTH_TO_CIRCLE_RADIUS_RATIO = 2.5;
	
	/* Configurable properties */
	
	// Requiring reset
	private ConfigurableIntegerProperty propDivCount;
	
	// Not requiring reset
	private ConfigurableIntegerProperty propMinDbValue, propLightXPosition, 
		propLightYPosition, propSensitivity, propMaxDbuValue;
	private ConfigurableColorProperty propBackgroundColor, propLightColor, 
		propIndicatorColor, propNormalLevelDigitsColor, propHighLevelDigitsColor, propPeakColor, propRotorColor, propRotorPlateColor;
	private ConfigurableDoubleProperty propLightSurfaceScale, propLightDiffuseConstant, 
		propLightSpecularConstant, propLightSpecularExponent, propLightZPosition, 
		propIndicatorWidthRatio, propDivisionWidthRatio;
	private ConfigurableBooleanProperty propVisualEnableExtras;
	
	/* Operational and optimization properties */
	private DoubleProperty currentDbRmsProp, currentDbPeakProp, lingerLevelDbProp, 
		radiusProp, centerXProp, centerYProp, currentLevelRadProp;
	private ObjectProperty<Color> darkerPeakColorProp;
	
	
	private double currentDbRms, currentDbPeak;
	private double lingerLevelDb, lingerOpValDb = LINGER_STAY_FACTOR;

	@Override
	public String getName() {
		return "Analog Meter";
	}
	
	@Override
	protected void initProperties() {
		super.initProperties();
		
		final String keyPrefix = "analogMeterView.";
		
		/* Configuration properties */
		
		// Requiring reset
		propDivCount = createConfigurableIntegerProperty(
				keyPrefix + "divisionsCount", "Divisions Count", 4, 41, 21, 1);
		propDivCount.getProp().addListener((obs, oldVal, newVal) -> {
			if(newVal != oldVal) {
				reset();
			}
		});
		
		// Not requiring reset
		propMinDbValue = createConfigurableIntegerProperty(
				keyPrefix + "minDbValue", "Min. DB Value", -100, -10, -24, 1);
		propMaxDbuValue = createConfigurableIntegerProperty(
				keyPrefix + "maxDBuValue", "Max. dBu Value", 0, 24, 6, 1);
		propBackgroundColor = createConfigurableColorProperty(
				keyPrefix + "backgroundColor", "Background Color", Color.ANTIQUEWHITE);
		propLightColor = createConfigurableColorProperty(
				keyPrefix + "lightColor", "Light Color", Color.ANTIQUEWHITE);
		propIndicatorColor = createConfigurableColorProperty(
				keyPrefix + "indicatorColor", "Indicator Color", Color.BLACK);
		propNormalLevelDigitsColor = createConfigurableColorProperty(
				keyPrefix + "normLevelDigitsColor", "Normal Level Digits Color", Color.BLACK);
		propHighLevelDigitsColor = createConfigurableColorProperty(
				keyPrefix + "highLevelDigitsColor", "High Level Digits Color", Color.RED);
		propLightSurfaceScale = UiUtils.createConfigurableDoubleProperty(
				keyPrefix + "lightSurfaceScale", "Light Surface Scale", 0.0, 10.0, 0.5, 0.5);
		propLightDiffuseConstant = UiUtils.createConfigurableDoubleProperty(
				keyPrefix + "lightDiffuseConstant", "Light Diffuse Constant", 0.0, 2.0, 1.8, 0.1);
		propLightSpecularConstant = UiUtils.createConfigurableDoubleProperty(
				keyPrefix + "lightSpecularConstant", "Light Specular Constant", 0.0, 2.0, 0.3, 0.1);
		propLightSpecularExponent = UiUtils.createConfigurableDoubleProperty(
				keyPrefix + "lightSpecularExponent", "Light Specular Exponent", 0.0, 40.0, 20.0, 2.0);
		propLightXPosition = createConfigurableIntegerProperty(
				keyPrefix + "lightXPosition", "Light X Position", 1, 4, 2, 1);
		propLightYPosition = createConfigurableIntegerProperty(
				keyPrefix + "lightYPosition", "Light Y Position", 1, 20, 16, 1);
		propLightZPosition = UiUtils.createConfigurableDoubleProperty(
				keyPrefix + "lightZPosition", "Light Z Position", 1.0, 10.0, 6.0, 0.2);
		propSensitivity = createConfigurableIntegerProperty(
				keyPrefix + "sensitivity", "Sensitivity", 1, 10, 3, 1);
		propVisualEnableExtras = createConfigurableBooleanProperty(
				keyPrefix + "enableExtras", "Enable Visual Extras", false);
		propIndicatorWidthRatio = UiUtils.createConfigurableDoubleProperty(
				keyPrefix + "indicatorWidthRatio", "Indicator Width", 0.001, 0.1, 0.002, 0.001);
		propDivisionWidthRatio = UiUtils.createConfigurableDoubleProperty(
				keyPrefix + "divisionWidthRatio", "Division Width", 0.001, 0.1, 0.003, 0.001);
		propPeakColor = createConfigurableColorProperty(
				keyPrefix + "peakColor", "Peak LED Color", Color.RED);
		propRotorColor = createConfigurableColorProperty(
				keyPrefix + "rotorColor", "Rotor Color", Color.DARKGRAY);
		propRotorPlateColor = createConfigurableColorProperty(
				keyPrefix + "rotorPlateColor", "Rotor Plate Color", Color.SANDYBROWN);
		
		/* Operational properties */
		currentDbRmsProp = new SimpleDoubleProperty(propMinDbValue.getProp().get());
		currentDbPeakProp = new SimpleDoubleProperty(propMinDbValue.getProp().get());
		lingerLevelDbProp = new SimpleDoubleProperty(propMinDbValue.getProp().get());
		
		radiusProp = new SimpleDoubleProperty();
		radiusProp.bind(Bindings.createDoubleBinding(
				() -> {
					return getRoot().widthProperty().get() / 2;
				}, getRoot().widthProperty()));
		
		centerXProp = new SimpleDoubleProperty();
		centerXProp.bind(getRoot().widthProperty().divide(2));
		
		centerYProp = new SimpleDoubleProperty();
		centerYProp.bind(getRoot().heightProperty().divide(2));
		
		currentLevelRadProp = new SimpleDoubleProperty();
		currentLevelRadProp.bind(Bindings.createDoubleBinding(
				() -> {
					return map(lingerLevelDbProp.get(), propMinDbValue.getProp().get(), 0, 
							MIN_DB_ANGLE_RAD, MAX_DB_ANGLE_RAD);
				}, lingerLevelDbProp, propMinDbValue.getProp()));
		
		// This property is used for optimization
		darkerPeakColorProp = new SimpleObjectProperty<>();
		darkerPeakColorProp.bind(Bindings.createObjectBinding(
				() -> {
					return propPeakColor.getProp().get().darker().darker();
				}, propPeakColor.getProp()));
		
		// This value controls the starting position of the indicator
		lingerLevelDb = propMinDbValue.getProp().get();
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Arrays.asList(
				propBackgroundColor, 
				propIndicatorColor, 
				propNormalLevelDigitsColor, 
				propHighLevelDigitsColor,
				propPeakColor, 
				propRotorColor, 
				propRotorPlateColor,
				propDivCount, 
				propMinDbValue, 
				propMaxDbuValue, 
				propSensitivity, 
				propIndicatorWidthRatio, 
				propDivisionWidthRatio,
				propVisualEnableExtras, 
				propLightColor, 
				propLightSurfaceScale, 
				propLightDiffuseConstant, 
				propLightSpecularConstant, 
				propLightSpecularExponent, 
				propLightXPosition, 
				propLightYPosition, 
				propLightZPosition);
	}
	
	@Override
	protected List<Node> collectNodes() {
		List<Node> nodes = new ArrayList<>();
		
		// Load the custom font
		Font.loadFont(AnalogMeterView.class.getResource(
				"/AlexBrush-Regular.ttf").toExternalForm(), 10);
		
		// Lighting
		Lighting lighting = createLighting();
		
		// Background
		nodes.add(createBackground(lighting));
		
		// Divisions and labels
		for (int i = 0; i < propDivCount.getProp().get(); i++) {
		    
			Line line;
		    if(i % 2 == 0) {
				line = createDivisionLine(i, lighting, DIV_LENGTH_RATIO_BIG);
		    	nodes.add(createDbLabel(i, line, lighting));
		    } else {
		    	line = createDivisionLine(i, lighting, DIV_LENGTH_RATIO_SMALL);
		    }
		    nodes.add(line);
		    
		    if(i % 4 == 0) {
		    	nodes.add(createPercentageLabel(i, line, lighting));
		    }
		}
		
		// Decorative elements
		createDecorativeElements(nodes, lighting);
		
		// Peak
		createPeak(nodes, lighting);
		
		// Indicator
		createIndicator(nodes);
	    
		return nodes;
	}
	
	@Override
	public void dataAvailable(float[] data) {
		Integer minDbValue = propMinDbValue.getProp().get();
		currentDbRms = Utils.toDB(peakLevel(data));
		currentDbRms = currentDbRms < minDbValue ? minDbValue : currentDbRms;
		currentDbPeak = Utils.toDB(peakLevel(data));
		currentDbPeak = currentDbPeak < minDbValue ? minDbValue : currentDbPeak;
	}

	@Override
	public void nextFrame() {
		
		// Update operational properties from UI thread
		currentDbRmsProp.set(currentDbRms);
		currentDbPeakProp.set(currentDbPeak);
		
		// Update indicator levels		
		if(lingerLevelDb < currentDbRms) {
			
			// TODO Accelerate, don't increase linearly
			lingerLevelDb += propSensitivity.getProp().get();
			lingerOpValDb = LINGER_STAY_FACTOR;
			
			if(lingerLevelDb > 0) {
				lingerLevelDb = 0;
			}
			
		} else {
			lingerLevelDb = lingerLevelDb - lingerOpValDb;
			lingerOpValDb = lingerOpValDb * LINGER_ACCELARATION_FACTOR;
			
			Integer minDbValue = propMinDbValue.getProp().get();
			if(lingerLevelDb < minDbValue) {
				lingerLevelDb = minDbValue;
			}
		}
		
		lingerLevelDbProp.set(lingerLevelDb);
	}
	
	private void createIndicator(List<Node> nodes) {
		
		Line indicator = new Line();
		
		indicator.startXProperty().bind(Bindings.createDoubleBinding(
				() -> {
	    			return centerXProp.get() + Math.cos(currentLevelRadProp.get()) * radiusProp.get();
				}, currentLevelRadProp, radiusProp, centerXProp));
		
	    indicator.startYProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    			return getRoot().heightProperty().get() - Math.sin(currentLevelRadProp.get()) * radiusProp.get();
	    		}, currentLevelRadProp,  radiusProp, getRoot().heightProperty()));
		
		
		indicator.endXProperty().bind(getRoot().widthProperty().divide(2));
		indicator.endYProperty().bind(getRoot().heightProperty());
		indicator.strokeProperty().bind(propIndicatorColor.getProp());

		// Bind the arrow width to a dedicated property and the width of the parent
		indicator.strokeWidthProperty().bind(Bindings.createDoubleBinding(
				() -> {
					return getRoot().widthProperty().get() * propIndicatorWidthRatio.getProp().get();
				}, propIndicatorWidthRatio.getProp(), getRoot().widthProperty()));
		
		Circle circle = new Circle();
		circle.centerXProperty().bind(getRoot().widthProperty().divide(2));
		circle.centerYProperty().bind(getRoot().heightProperty());
		circle.radiusProperty().bind(getRoot().widthProperty().multiply(BACK_CIRCLE_RADIUS_TO_SCENE_WIDTH_RATIO));
		circle.fillProperty().bind(propRotorColor.getProp());
		circle.strokeProperty().bind(Bindings.createObjectBinding(
				() -> propRotorColor.getProp().get().darker(), 
				propRotorColor.getProp()));
		circle.strokeWidthProperty().bind(indicator.strokeWidthProperty());
		circle.setCache(true);
		
		Rectangle plate = new Rectangle();
		plate.widthProperty().bind(circle.radiusProperty().multiply(ELEMENT_WIDTH_TO_CIRCLE_RADIUS_RATIO));
		plate.heightProperty().bind(plate.widthProperty().divide(4));
		plate.xProperty().bind(circle.centerXProperty().subtract(plate.widthProperty().divide(2)));
		plate.yProperty().bind(circle.centerYProperty().subtract(plate.heightProperty().divide(2)));
		plate.arcHeightProperty().bind(plate.widthProperty().divide(8));
		plate.arcWidthProperty().bind(plate.widthProperty().divide(8));
		plate.rotateProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double angleDeg = map(lingerLevelDbProp.get(), propMinDbValue.getProp().get(), 0, 
							MIN_DB_ANGLE_DEGREES, MAX_DB_ANGLE_DEGREES);
					return angleDeg * -1 - 90;
				
				}, lingerLevelDbProp, propMinDbValue.getProp()));
		
		plate.fillProperty().bind(propRotorPlateColor.getProp());
		plate.strokeProperty().bind(Bindings.createObjectBinding(
				() -> propRotorPlateColor.getProp().get().darker(), 
				propRotorPlateColor.getProp()));
		plate.strokeWidthProperty().bind(circle.strokeWidthProperty());
		
		nodes.addAll(Arrays.asList(circle, plate, indicator));
	}
	
	private void createPeak(List<Node> nodes, Effect effect) {
		InnerShadow peakEffect = new InnerShadow();
		peakEffect.setColor(Color.rgb(0,0,0,0.7));
		peakEffect.setRadius(6);
		peakEffect.setOffsetX(0);
		peakEffect.setOffsetY(2);
		
		Circle peak = new Circle();
		peak.centerXProperty().bind(getRoot().widthProperty().subtract(getRoot().widthProperty().divide(8)));
		peak.centerYProperty().bind(getRoot().heightProperty().divide(8));
		peak.radiusProperty().bind(getRoot().widthProperty().multiply(0.015));
		peak.fillProperty().bind(Bindings.createObjectBinding(
				() -> {
					Color level;
					if(currentDbPeakProp.get() >= 0) {
						level = propPeakColor.getProp().get();
					} else {
						level = darkerPeakColorProp.get();
					}
					return level;
				}, currentDbPeakProp, propPeakColor.getProp(), darkerPeakColorProp));
		
		peak.setEffect(peakEffect);
		
		Label peakLabel = new Label("Peak");
		peakLabel.textFillProperty().bind(propNormalLevelDigitsColor.getProp());
		peakLabel.layoutXProperty().bind(peak.centerXProperty().add(peak.radiusProperty()));
		peakLabel.layoutYProperty().bind(peak.centerYProperty());
		peakLabel.setCache(true);
		peakLabel.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", Bindings.createDoubleBinding(
						() -> (getRoot().widthProperty().get() * SCENE_MARGIN_RATIO) / 1.2,
						getRoot().widthProperty()),
				"; -fx-font-family: 'Alex Brush'"));
//		peakLabel.effectProperty().bind(Bindings.createObjectBinding(
//				() -> {
//					return propVisualEnableExtras.getProp().get() ? effect : null;
//				}, propVisualEnableExtras.getProp()));
		
		nodes.add(peakLabel);
		nodes.add(peak);

	}
	
	private void createDecorativeElements(List<Node> nodes, Effect effect) {		
		Label centerLabel = new Label("Analog Meter");
		centerLabel.textFillProperty().bind(propNormalLevelDigitsColor.getProp());
		centerLabel.layoutXProperty().bind(getRoot().widthProperty().divide(2).subtract(
				centerLabel.widthProperty().divide(2)));
		centerLabel.layoutYProperty().bind(getRoot().heightProperty().divide(2).subtract(centerLabel.heightProperty().divide(2)));
		centerLabel.setCache(true);
		centerLabel.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", Bindings.createDoubleBinding(
						() -> (getRoot().widthProperty().get() * SCENE_MARGIN_RATIO) / 1.2,
						getRoot().widthProperty()),
				"; -fx-font-family: 'Alex Brush'"));
//		centerLabel.effectProperty().bind(Bindings.createObjectBinding(
//				() -> {
//					return propVisualEnableExtras.getProp().get() ? effect : null;
//				}, propVisualEnableExtras.getProp()));
		nodes.add(centerLabel);
	}
	
	private Lighting createLighting() {
		Light.Point light = new Light.Point();
		light.xProperty().bind(getRoot().widthProperty().divide(
				Bindings.createDoubleBinding(() -> Double.valueOf(propLightXPosition.getProp().get()), 
						propLightXPosition.getProp())));
		light.yProperty().bind(getRoot().heightProperty().subtract(getRoot().heightProperty().divide(
				Bindings.createDoubleBinding(() -> Double.valueOf(propLightYPosition.getProp().get()), 
						propLightYPosition.getProp()))));
		light.zProperty().bind(getRoot().widthProperty().divide(
				Bindings.createDoubleBinding(() -> Double.valueOf(propLightZPosition.getProp().get()), 
						propLightZPosition.getProp())));
		light.colorProperty().bind(propLightColor.getProp());

		Lighting lighting = new Lighting();
		lighting.setLight(light);
		lighting.surfaceScaleProperty().bind(propLightSurfaceScale.getProp());
		lighting.diffuseConstantProperty().bind(propLightDiffuseConstant.getProp());
		lighting.specularConstantProperty().bind(propLightSpecularConstant.getProp());
		lighting.specularExponentProperty().bind(propLightSpecularExponent.getProp());
		return lighting;
	}
	
	private Rectangle createBackground(Lighting lighting) {
		Rectangle background = new Rectangle();
		background.setX(0);
		background.setY(0);
		background.widthProperty().bind(getRoot().widthProperty());
		background.heightProperty().bind(getRoot().heightProperty());
		background.fillProperty().bind(propBackgroundColor.getProp());
		background.setCache(true); // If this is not set there are visible artifacts from the lighting effect and moving objects on top of it
		background.setEffect(lighting);
		background.effectProperty().bind(Bindings.createObjectBinding(
				() -> {
					return propVisualEnableExtras.getProp().get() ? lighting : null;
				}, propVisualEnableExtras.getProp()));
		return background;
	}
	
	private Line createDivisionLine(int idx, Effect effect, double divLengthRatio) {
		
		double angleRad = map(idx, 0, propDivCount.getProp().get() - 1, MIN_DB_ANGLE_RAD, MAX_DB_ANGLE_RAD);

	    Line line = new Line();
	    
	    line.startXProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    			return centerXProp.get() + Math.cos(angleRad) * radiusProp.get();
	    		}, radiusProp, centerXProp));
	    line.startYProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    	        return getRoot().heightProperty().get() - Math.sin(angleRad) * radiusProp.get();
	    		}, radiusProp, getRoot().heightProperty()));
	    line.endXProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    			double len = getRoot().heightProperty().get() * divLengthRatio;
	    	        return centerXProp.get() + Math.cos(angleRad) * (radiusProp.get() - len);
	    		}, radiusProp, centerXProp, getRoot().heightProperty()));
	    line.endYProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    	        double len = getRoot().heightProperty().get() * divLengthRatio;
	    	        return getRoot().heightProperty().get() - Math.sin(angleRad) * (radiusProp.get() - len);
	    		}, radiusProp, getRoot().heightProperty()));
	    line.setCache(true);
	    
	    // Bind the line color to the minimum dB value and the max dBu value,
	    // as well as normal level and high level colors
	    line.strokeProperty().bind(Bindings.createObjectBinding(
	    		() -> {
	    			long db = Math.round(map(idx, 0, propDivCount.getProp().get() - 1,
	    					propMinDbValue.getProp().get() + propMaxDbuValue.getProp().get() , 
	    					propMaxDbuValue.getProp().get()));
	    			
	    			Color color;
	    			if(db < 0) {
	    				color = propNormalLevelDigitsColor.getProp().get();
	    			} else {
	    				color = propHighLevelDigitsColor.getProp().get();
	    			}
	    			
	    			return color;
 	    		}, propMinDbValue.getProp(), propMaxDbuValue.getProp(), 
	    			propNormalLevelDigitsColor.getProp(), propHighLevelDigitsColor.getProp()));
	    
		// Bind the line width to a dedicated property and the width of the parent
	    line.strokeWidthProperty().bind(Bindings.createDoubleBinding(
				() -> {
					return getRoot().widthProperty().get() * propDivisionWidthRatio.getProp().get();
				}, propDivisionWidthRatio.getProp(), getRoot().widthProperty()));
		
	    // Enable or disable the effect based on a boolean property
//	    line.effectProperty().bind(Bindings.createObjectBinding(
//				() -> {
//					return propVisualEnableExtras.getProp().get() ? effect : null;
//				}, propVisualEnableExtras.getProp()));
	    
	    return line;
	}
	
	private Label createDbLabel(int idx, Line line, Effect effect) {

		Label label = new Label();
	    label.layoutXProperty().bind(line.startXProperty().subtract(label.widthProperty().divide(2)));
	    label.layoutYProperty().bind(line.startYProperty().subtract(label.heightProperty().multiply(1.5)));
	    
	    // Bind the text to the minimum dB value and the max dBu value
	    label.textProperty().bind(Bindings.createStringBinding(
	    		() -> {
	    			long db = Math.round(map(idx, 0, propDivCount.getProp().get() - 1,
	    					propMinDbValue.getProp().get() + propMaxDbuValue.getProp().get() , 
	    					propMaxDbuValue.getProp().get()));
	    			return String.valueOf(db) + "dB";
	    		}, propMinDbValue.getProp(), propMaxDbuValue.getProp()));
	    
	    // Bind the text color to the minimum dB value and the max dBu value,
	    // as well as normal level and high level colors
	    label.textFillProperty().bind(Bindings.createObjectBinding(
	    		() -> {
	    			long db = Math.round(map(idx, 0, propDivCount.getProp().get() - 1,
	    					propMinDbValue.getProp().get() + propMaxDbuValue.getProp().get() , 
	    					propMaxDbuValue.getProp().get()));
	    			
	    			Color color;
	    			if(db < 0) {
	    				color = propNormalLevelDigitsColor.getProp().get();
	    			} else {
	    				color = propHighLevelDigitsColor.getProp().get();
	    			}
	    			
	    			return color;
 	    		}, propMinDbValue.getProp(), propMaxDbuValue.getProp(), 
	    			propNormalLevelDigitsColor.getProp(), propHighLevelDigitsColor.getProp()));
	    
		label.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", Bindings.createDoubleBinding(
						() -> (getRoot().widthProperty().get() * SCENE_MARGIN_RATIO) / 2.5,
						getRoot().widthProperty())));
		label.setCache(true);
		
//		label.effectProperty().bind(Bindings.createObjectBinding(
//				() -> {
//					return propVisualEnableExtras.getProp().get() ? effect : null;
//				}, propVisualEnableExtras.getProp()));
	    
	    return label;
	}
	
	private Label createPercentageLabel(int idx, Line line, Effect effect) {
		long percentageValue = Math.round(map(idx, 0, propDivCount.getProp().get() - 1, 0, 100));
		
	    Label label = new Label(String.valueOf(percentageValue) + "%");
	    label.layoutXProperty().bind(line.endXProperty().subtract(label.widthProperty().divide(2)));
	    label.layoutYProperty().bind(line.endYProperty().add(label.heightProperty().divide(3)));
	    
	    // Bind the text color to the minimum dB value and the max dBu value,
	    // as well as normal level and high level colors
	    label.textFillProperty().bind(Bindings.createObjectBinding(
	    		() -> {
	    			long db = Math.round(map(idx, 0, propDivCount.getProp().get() - 1,
	    					propMinDbValue.getProp().get() + propMaxDbuValue.getProp().get() , 
	    					propMaxDbuValue.getProp().get()));
	    			
	    			Color color;
	    			if(db < 0) {
	    				color = propNormalLevelDigitsColor.getProp().get();
	    			} else {
	    				color = propHighLevelDigitsColor.getProp().get();
	    			}
	    			
	    			return color;
 	    		}, propMinDbValue.getProp(), propMaxDbuValue.getProp(), 
	    			propNormalLevelDigitsColor.getProp(), propHighLevelDigitsColor.getProp()));
	    
		label.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", Bindings.createDoubleBinding(
						() -> (getRoot().widthProperty().get() * SCENE_MARGIN_RATIO) / 3.5,
						getRoot().widthProperty())));
		label.setCache(true);
		
//		label.effectProperty().bind(Bindings.createObjectBinding(
//				() -> {
//					return propVisualEnableExtras.getProp().get() ? effect : null;
//				}, propVisualEnableExtras.getProp()));
		
	    return label;
	}

}
