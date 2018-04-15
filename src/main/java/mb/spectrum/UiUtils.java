package mb.spectrum;

import static mb.spectrum.Utils.map;
import static org.apache.commons.lang3.math.NumberUtils.isCreatable;

import java.text.MessageFormat;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Transition;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;
import mb.spectrum.prop.ConfigurableBooleanProperty;
import mb.spectrum.prop.ConfigurableColorProperty;
import mb.spectrum.prop.ConfigurableDoubleProperty;
import mb.spectrum.prop.ConfigurableIntegerProperty;

public class UiUtils {
	
	public static Line createGridLine(double startX, double startY, double endX, double endY, Color color, List<Line> list) {
		Line line = new Line(startX, startY, endX, endY);
		line.setStroke(color);
		line.getStrokeDashArray().addAll(2d);
		line.setCache(true);
		list.add(line);
		return line;
	}
	
	public static Label createLabel(String text, List<Label> list) {
		Label label = new Label(text);
		label.setCache(true);
		list.add(label);
		return label;
	}
	
	public static Transition createFadeInOutTransition(Node node, double fadeInMs, 
			double lingerMs, double fadeOutMs, EventHandler<ActionEvent> handler) {
		
		FadeTransition fadeIn = new FadeTransition(Duration.millis(fadeInMs), node);
		fadeIn.setFromValue(0.0f);
		fadeIn.setToValue(1.0f);
		fadeIn.setCycleCount(1);
		fadeIn.setAutoReverse(false);
		
		FadeTransition fadeOut = new FadeTransition(Duration.millis(fadeOutMs), node);
		fadeOut.setFromValue(1.0f);
		fadeOut.setToValue(0.0f);
		fadeOut.setCycleCount(1);
		fadeOut.setAutoReverse(false);
		fadeOut.setDelay(Duration.millis(lingerMs));
		
		SequentialTransition trans = new SequentialTransition(fadeIn, fadeOut);
		trans.setOnFinished(handler);
		return trans;
	}
	
	public static Transition createFadeInTransition(Node node, double fadeInMs, 
			EventHandler<ActionEvent> handler) {
		FadeTransition fadeIn = new FadeTransition(Duration.millis(fadeInMs), node);
		fadeIn.setFromValue(0.0f);
		fadeIn.setToValue(1.0f);
		fadeIn.setCycleCount(1);
		fadeIn.setAutoReverse(false);
		fadeIn.setOnFinished(handler);
		return fadeIn;
	}
	
	public static Transition createFadeOutTransition(Node node, double fadeInMs, 
			EventHandler<ActionEvent> handler) {
		FadeTransition fadeOut = new FadeTransition(Duration.millis(fadeInMs), node);
		fadeOut.setFromValue(node.getOpacity());
		fadeOut.setToValue(0.0f);
		fadeOut.setCycleCount(1);
		fadeOut.setAutoReverse(false);
		fadeOut.setOnFinished(handler);
		return fadeOut;
	}
	
//	public static Spinner<Double> createDoubleSpinner(double min, double max, double initValue, double step) {
//		return new Spinner<Double>(new DoubleSpinnerValueFactory(min, max, initValue, step));
//	}
//	
//	public static Spinner<Integer> createIntegerSpinner(int min, int max, int initValue, int step) {
//		return new Spinner<Integer>(new IntegerSpinnerValueFactory(min, max, initValue, step));
//	}
	
	public static ColorPicker createColorPropertyColorPicker(Color color, Pane parent) {
		ColorPicker picker = new ColorPicker(color);
		picker.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", parent.widthProperty().divide(40), ";"));
		return picker;
	}
	
	public static Button createActionPropertyButton(String label) {
		Button button = new Button(label);
		return button;
	}
	
	public static CheckBox createBooleanPropertyCheckBox(Boolean value, String label, Pane parent) {
		CheckBox box = new CheckBox(label);
		box.setSelected(value);
		box.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", parent.widthProperty().divide(40), ";"));
		return box;
	}
	
	public static Label createNumberPropertyLabel(String initValue, Pane parent) {
		Label label = new Label(initValue);
		label.setAlignment(Pos.CENTER);
		
		// TODO Play a bit with the values below to find the best fit
		label.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", parent.widthProperty().divide(20), ";", 
				"-fx-padding: ", parent.widthProperty().divide(50), ";"));
		return label;
	}
	
	public static Line createThickRoundedLine(Color color) {
		Line line = new Line();
		line.setStroke(color);
		line.setStrokeWidth(4);
		line.setStrokeLineCap(StrokeLineCap.ROUND);
		return line;
	}
	
	public static String colorToWeb(Color color) {
		return MessageFormat.format("rgba({0,number,#}, {1,number,#}, {2,number,#}, {3})", 
				map(color.getRed(), 0, 1, 0, 255),
				map(color.getGreen(), 0, 1, 0, 255),
				map(color.getBlue(), 0, 1, 0, 255),
				color.getOpacity());
	}
	
	public static ConfigurableColorProperty createConfigurableColorProperty(String key, String name, Color defaultValue) {
		ConfigService cs = ConfigService.getInstance();
		ConfigurableColorProperty prop = 
				new ConfigurableColorProperty(name, null, null, 
						Color.web(cs.getOrCreateProperty(key, colorToWeb(defaultValue))), null);
		prop.getProp().addListener((obs, oldVal, newVal) -> {
			cs.setProperty(key, colorToWeb(newVal));
		});
		return prop;
	}
	
	public static ConfigurableDoubleProperty createConfigurableDoubleProperty(String key, String name, 
			Double minValue, Double maxValue, Double defaultValue, Double increment) {
		ConfigService cs = ConfigService.getInstance();
		ConfigurableDoubleProperty prop = 
				new ConfigurableDoubleProperty(name, minValue, maxValue, 
						Double.valueOf(cs.getOrCreateProperty(key, String.valueOf(defaultValue))), increment);
		prop.getProp().addListener((obs, oldVal, newVal) -> {
			cs.setProperty(key, String.valueOf(newVal));
		});
		return prop;
	}
	
	public static ConfigurableBooleanProperty createConfigurableBooleanProperty(String key, String name, Boolean defaultValue) {
		ConfigService cs = ConfigService.getInstance();
		ConfigurableBooleanProperty prop = 
				new ConfigurableBooleanProperty(name, null, null, 
						Boolean.valueOf(cs.getOrCreateProperty(key, String.valueOf(defaultValue))), null);
		prop.getProp().addListener((obs, oldVal, newVal) -> {
			cs.setProperty(key, String.valueOf(newVal));
		});
		return prop;
	}
	
	public static ConfigurableIntegerProperty createConfigurableIntegerProperty(String key, String name, 
			Integer minValue, Integer maxValue, Integer defaultValue, Integer increment) {
		ConfigService cs = ConfigService.getInstance();
		ConfigurableIntegerProperty prop = 
				new ConfigurableIntegerProperty(name, minValue, maxValue, 
						Integer.valueOf(cs.getOrCreateProperty(key, String.valueOf(defaultValue))), increment);
		prop.getProp().addListener((obs, oldVal, newVal) -> {
			cs.setProperty(key, String.valueOf(newVal));
		});
		return prop;
	}
	
	public static boolean isDouble(String string) {
		return isCreatable(string) && string.contains(".");
	}
}
