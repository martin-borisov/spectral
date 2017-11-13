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
import javafx.beans.property.SimpleDoubleProperty;
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
	
	// TODO It will be cool to make these values configurable
	private static final int MIN_DB_VALUE = -24; // = MIN_DBU_VALUE - MAX_DBU_VALUE = -66dBu
	private static final int MAX_DBU_VALUE = 6; // dBu
	private static final int MIN_DBU_VALUE = MIN_DB_VALUE + MAX_DBU_VALUE; // dBu
	private static final double MIN_DB_ANGLE_RAD = 2.22; // Range is 3.14 to 0
	private static final double MAX_DB_ANGLE_RAD = 0.92; // Range is 3.14 to 0
	private static final double RADIUS_TO_WIDTH_RATIO = 1.5;
	private static final double TOP_MARGIN_RATIO = 0.15;
	private static final double DIV_LENGTH_RATIO = 0.02;
	private static final double LINGER_STAY_FACTOR = 0.05;
	private static final double LINGER_ACCELARATION_FACTOR = 1.15;
	private static final double BACK_CIRCLE_RADIUS_TO_SCENE_WIDTH_RATIO = 0.05;
	private static final double ELEMENT_WIDTH_TO_CIRCLE_RADIUS_RATIO = 2.5;
	
	private ConfigurableIntegerProperty propDivCount, propLightXPosition, propLightYPosition, propSensitivity;
	private ConfigurableColorProperty propBackgroundColor, propLightColor, 
		propIndicatorColor, propNormalLevelDigitsColor, propHighLevelDigitsColor;
	private ConfigurableDoubleProperty propLightSurfaceScale, propLightDiffuseConstant, 
		propLightSpecularConstant, propLightSpecularExponent, propLightZPosition;
	private ConfigurableBooleanProperty propVisualEnableExtras;
	
	private DoubleProperty currentDbRmsProp, currentDbPeakProp, lingerLevelDbProp;
	
	private double currentDbRms, currentDbPeak;
	private double lingerLevelDb = MIN_DB_VALUE, lingerOpValDb = LINGER_STAY_FACTOR;

	@Override
	public String getName() {
		return "Analog Meter";
	}
	
	@Override
	protected void initProperties() {
		super.initProperties();
		
		final String keyPrefix = "analogMeterView.";
		
		// Configuration properties
		propDivCount = createConfigurableIntegerProperty(
				keyPrefix + "divisionsCount", "Divisions Count", 4, 20, 11, 1);
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
		
		
		// Operational properties
		currentDbRmsProp = new SimpleDoubleProperty(MIN_DB_VALUE);
		currentDbPeakProp = new SimpleDoubleProperty(MIN_DB_VALUE);
		lingerLevelDbProp = new SimpleDoubleProperty(MIN_DB_VALUE);
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Arrays.asList(propDivCount, propBackgroundColor, 
				propIndicatorColor, propNormalLevelDigitsColor, propHighLevelDigitsColor, 
				propVisualEnableExtras, propLightColor, propLightSurfaceScale, propLightDiffuseConstant, 
				propLightSpecularConstant, propLightSpecularExponent, propLightXPosition, propLightYPosition, 
				propLightZPosition, propSensitivity);
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
			Line line = createDivisionLine(i, lighting);
		    nodes.add(line);
		    nodes.add(createDbLabel(i, line, lighting));
		    
		    if(i % 2 == 0) {
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
		currentDbRms = Utils.toDB(peakLevel(data));
		currentDbRms = currentDbRms < MIN_DB_VALUE ? MIN_DB_VALUE : currentDbRms;
		currentDbPeak = Utils.toDB(peakLevel(data));
		currentDbPeak = currentDbPeak < MIN_DB_VALUE ? MIN_DB_VALUE : currentDbPeak;
	}

	@Override
	public void nextFrame() {
		
		// Update operational properties from UI thread
		currentDbRmsProp.set(currentDbRms);
		currentDbPeakProp.set(currentDbPeak);
		
		// Update linger levels
		/*
		lingerLevelDb = lingerLevelDb - lingerOpValDb;
		lingerOpValDb = lingerOpValDb * LINGER_ACCELARATION_FACTOR;
		 */
		
		if(currentDbRms > lingerLevelDb) {
			
			/*
			lingerLevelDb = currentDbRms;
			*/
			
			// TODO Accelarate, don't increase constantly
			lingerLevelDb += propSensitivity.getProp().get();
			lingerOpValDb = LINGER_STAY_FACTOR;
		} else {
			lingerLevelDb = lingerLevelDb - lingerOpValDb;
			lingerOpValDb = lingerOpValDb * LINGER_ACCELARATION_FACTOR;
		}
		
		if(lingerLevelDb < MIN_DB_VALUE) {
			lingerLevelDb = MIN_DB_VALUE;
		}
		lingerLevelDbProp.set(lingerLevelDb);
	}
	
	private void createIndicator(List<Node> nodes) {
		
		Line indicator = new Line();
		indicator.startXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double angleRad = map(lingerLevelDbProp.get(), MIN_DB_VALUE, 0, 
							MIN_DB_ANGLE_RAD, MAX_DB_ANGLE_RAD);
					double radius = getRoot().widthProperty().get() / RADIUS_TO_WIDTH_RATIO;
					double sceneCenterX = getRoot().widthProperty().get() / 2;
	    			return sceneCenterX + Math.cos(angleRad) * radius;
				}, lingerLevelDbProp, getRoot().widthProperty()));
	    indicator.startYProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    			double angleRad = map(lingerLevelDbProp.get(), MIN_DB_VALUE, 0, 
							MIN_DB_ANGLE_RAD, MAX_DB_ANGLE_RAD);
	    			double radius = getRoot().widthProperty().get() / RADIUS_TO_WIDTH_RATIO;
	    	        double topMagin = getRoot().heightProperty().get() * TOP_MARGIN_RATIO;
	    	        return radius + topMagin - Math.sin(angleRad) * radius;
	    		}, lingerLevelDbProp, getRoot().widthProperty(), getRoot().heightProperty()));
		indicator.endXProperty().bind(getRoot().widthProperty().divide(2));
		indicator.endYProperty().bind(getRoot().heightProperty());
		indicator.strokeProperty().bind(propIndicatorColor.getProp());
		indicator.setStrokeWidth(2); // TODO Prop / Should scale
		
		Circle backCircle = new Circle();
		backCircle.centerXProperty().bind(getRoot().widthProperty().divide(2));
		backCircle.centerYProperty().bind(getRoot().heightProperty());
		backCircle.radiusProperty().bind(getRoot().widthProperty().multiply(BACK_CIRCLE_RADIUS_TO_SCENE_WIDTH_RATIO));
		backCircle.setFill(Color.DARKGRAY); // TODO Prop
		backCircle.setStroke(Color.DIMGRAY); // TODO Should be derived from the fill
		backCircle.setStrokeWidth(3); // TODO Should scale
		backCircle.setCache(true);
		
		Rectangle frontElement = new Rectangle();
		frontElement.widthProperty().bind(backCircle.radiusProperty().multiply(ELEMENT_WIDTH_TO_CIRCLE_RADIUS_RATIO));
		frontElement.heightProperty().bind(frontElement.widthProperty().divide(4));
		frontElement.xProperty().bind(backCircle.centerXProperty().subtract(frontElement.widthProperty().divide(2)));
		frontElement.yProperty().bind(backCircle.centerYProperty().subtract(frontElement.heightProperty().divide(2)));
		frontElement.arcHeightProperty().bind(frontElement.widthProperty().divide(8));
		frontElement.arcWidthProperty().bind(frontElement.widthProperty().divide(8));
		frontElement.rotateProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double angleRad = map(lingerLevelDbProp.get(), MIN_DB_VALUE, 0, 
							MIN_DB_ANGLE_RAD, MAX_DB_ANGLE_RAD);
					return angleRad * -57.295779513 - 90;
				}, lingerLevelDbProp));
		
		frontElement.setFill(Color.SANDYBROWN); // TODO Prop
		frontElement.setStroke(Color.SADDLEBROWN); // TODO Should be derived from the fill
		frontElement.strokeWidthProperty().bind(backCircle.strokeWidthProperty());
		
		nodes.addAll(Arrays.asList(backCircle, frontElement, indicator));
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
						level = Color.RED;
					} else {
						level = Color.rgb(100, 0, 0);
					}
					return level;
				}, currentDbPeakProp));		
		peak.setEffect(peakEffect);
		
		Label peakLabel = new Label("Peak");
		peakLabel.textFillProperty().bind(propNormalLevelDigitsColor.getProp());
		peakLabel.layoutXProperty().bind(peak.centerXProperty().add(peak.radiusProperty()));
		peakLabel.layoutYProperty().bind(peak.centerYProperty().subtract(peak.radiusProperty()));
		peakLabel.setCache(true);
		peakLabel.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", Bindings.createDoubleBinding(
						() -> (getRoot().widthProperty().get() * SCENE_MARGIN_RATIO) / 1.2,
						getRoot().widthProperty()),
				"; -fx-font-family: 'Alex Brush'"));
		peakLabel.effectProperty().bind(Bindings.createObjectBinding(
				() -> {
					return propVisualEnableExtras.getProp().get() ? effect : null;
				}, propVisualEnableExtras.getProp()));
		
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
		centerLabel.effectProperty().bind(Bindings.createObjectBinding(
				() -> {
					return propVisualEnableExtras.getProp().get() ? effect : null;
				}, propVisualEnableExtras.getProp()));
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
	
	private Line createDivisionLine(int idx, Effect effect) {
		
		double angleRad = map(idx, 0, propDivCount.getProp().get() - 1, MIN_DB_ANGLE_RAD, MAX_DB_ANGLE_RAD);
		
	    Line line = new Line();
	    line.startXProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    			double radius = getRoot().widthProperty().get() / RADIUS_TO_WIDTH_RATIO;
	    			double sceneCenterX = getRoot().widthProperty().get() / 2;
	    			return sceneCenterX + Math.cos(angleRad) * radius;
	    		}, getRoot().widthProperty()));
	    line.startYProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    			double radius = getRoot().widthProperty().get() / RADIUS_TO_WIDTH_RATIO;
	    	        double topMagin = getRoot().heightProperty().get() * TOP_MARGIN_RATIO;
	    	        return radius + topMagin - Math.sin(angleRad) * radius;
	    		}, getRoot().widthProperty(), getRoot().heightProperty()));
	    line.endXProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    			double radius = getRoot().widthProperty().get() / RADIUS_TO_WIDTH_RATIO;
	    			double len = getRoot().heightProperty().get() * DIV_LENGTH_RATIO;
	    			double sceneCenterX = getRoot().widthProperty().get() / 2;
	    	        return sceneCenterX + Math.cos(angleRad) * (radius - len);
	    		}, getRoot().widthProperty()));
	    line.endYProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    			double radius = getRoot().widthProperty().get() / RADIUS_TO_WIDTH_RATIO;
	    	        double topMagin = getRoot().heightProperty().get() * TOP_MARGIN_RATIO;
	    	        double len = getRoot().heightProperty().get() * DIV_LENGTH_RATIO;
	    	        return radius + topMagin - Math.sin(angleRad) * (radius - len);
	    		}, getRoot().widthProperty(), getRoot().heightProperty()));
	    line.setCache(true);
	    
		long dbValue = Math.round(map(idx, 0, propDivCount.getProp().get() - 1, MIN_DBU_VALUE, MAX_DBU_VALUE));
	    if(dbValue < 0) {
	    	line.strokeProperty().bind(propNormalLevelDigitsColor.getProp());
	    } else {
	    	line.strokeProperty().bind(propHighLevelDigitsColor.getProp());
	    }
	    
	    line.setStrokeWidth(2); // TODO Should scale
	   
	    line.effectProperty().bind(Bindings.createObjectBinding(
				() -> {
					return propVisualEnableExtras.getProp().get() ? effect : null;
				}, propVisualEnableExtras.getProp()));
	    
	    return line;
	}
	
	private Label createDbLabel(int idx, Line line, Effect effect) {
		long dbValue = Math.round(map(idx, 0, propDivCount.getProp().get() - 1, MIN_DBU_VALUE, MAX_DBU_VALUE));
		
	    Label label = new Label(String.valueOf(dbValue) + "dB");
	    label.layoutXProperty().bind(line.startXProperty().subtract(label.widthProperty().divide(2)));
	    label.layoutYProperty().bind(line.startYProperty().subtract(label.heightProperty().multiply(1.5)));
	    
	    if(dbValue < 0) {
	    	label.textFillProperty().bind(propNormalLevelDigitsColor.getProp());
	    } else {
	    	label.textFillProperty().bind(propHighLevelDigitsColor.getProp());
	    }
	    
		label.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", Bindings.createDoubleBinding(
						() -> (getRoot().widthProperty().get() * SCENE_MARGIN_RATIO) / 2.5,
						getRoot().widthProperty())));
		label.setCache(true);
		
		label.effectProperty().bind(Bindings.createObjectBinding(
				() -> {
					return propVisualEnableExtras.getProp().get() ? effect : null;
				}, propVisualEnableExtras.getProp()));
	    
	    return label;
	}
	
	private Label createPercentageLabel(int idx, Line line, Effect effect) {
		long percentageValue = Math.round(map(idx, 0, propDivCount.getProp().get() - 1, 0, 100));
		long dbValue = Math.round(map(idx, 0, propDivCount.getProp().get() - 1, MIN_DBU_VALUE, MAX_DBU_VALUE));
		
	    Label label = new Label(String.valueOf(percentageValue) + "%");
	    label.layoutXProperty().bind(line.endXProperty().subtract(label.widthProperty().divide(2)));
	    label.layoutYProperty().bind(line.endYProperty().add(label.heightProperty().divide(3)));
	    
	    if(dbValue < 0) {
	    	label.textFillProperty().bind(propNormalLevelDigitsColor.getProp());
	    } else {
	    	label.textFillProperty().bind(propHighLevelDigitsColor.getProp());
	    }
	    
		label.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", Bindings.createDoubleBinding(
						() -> (getRoot().widthProperty().get() * SCENE_MARGIN_RATIO) / 3.5,
						getRoot().widthProperty())));
		label.setCache(true);
		
		label.effectProperty().bind(Bindings.createObjectBinding(
				() -> {
					return propVisualEnableExtras.getProp().get() ? effect : null;
				}, propVisualEnableExtras.getProp()));
		
	    return label;
	}

}
