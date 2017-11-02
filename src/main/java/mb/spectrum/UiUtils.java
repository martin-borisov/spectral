package mb.spectrum;

import static mb.spectrum.Utils.map;

import java.text.MessageFormat;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Transition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

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
		fadeIn.setToValue(node.getOpacity());
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
	
	public static Spinner<Double> createDoubleSpinner(double min, double max, double initValue, double step) {
		return new Spinner<Double>(new DoubleSpinnerValueFactory(min, max, initValue, step));
	}
	
	public static Spinner<Integer> createIntegerSpinner(int min, int max, int initValue, int step) {
		return new Spinner<Integer>(new IntegerSpinnerValueFactory(min, max, initValue, step));
	}
	
	public static ColorPicker createColorPicker(Color color) {
		return new ColorPicker(color);
	}
	
	public static CheckBox createCheckBox(Boolean value, String label) {
		CheckBox box = new CheckBox(label);
		box.setSelected(value);
		return box;
	}
	
	public static Line createThickRoundedLine(Color color) {
		Line line = new Line();
		line.setStroke(color);
		line.setStrokeWidth(4);
		line.setStrokeLineCap(StrokeLineCap.ROUND);
		return line;
		
	}
	
	public static String colorToWeb(Color color) {
		return MessageFormat.format("rgba({0}, {1}, {2}, {3})", 
				map(color.getRed(), 0, 1, 0, 255),
				map(color.getGreen(), 0, 1, 0, 255),
				map(color.getBlue(), 0, 1, 0, 255),
				color.getOpacity());
	}
	
	public static SimpleObjectProperty<Color> createConfigurableColorProperty(String key, String name, Color defaultValue) {
		ConfigService cs = ConfigService.getInstance();
		SimpleObjectProperty<Color> prop = new SimpleObjectProperty<>(null, name, 
				Color.web(cs.getOrCreateProperty(key, colorToWeb(defaultValue))));
		prop.addListener((obs, oldVal, newVal) -> {
			cs.setProperty(key, colorToWeb(newVal));
		});
		return prop;
	}
	
	public static SimpleObjectProperty<Double> createConfigurableDoubleProperty(String key, String name, Double defaultValue) {
		ConfigService cs = ConfigService.getInstance();
		SimpleObjectProperty<Double> prop = new SimpleObjectProperty<>(null, name, 
				Double.valueOf(cs.getOrCreateProperty(key, String.valueOf(defaultValue))));
		prop.addListener((obs, oldVal, newVal) -> {
			cs.setProperty(key, String.valueOf(newVal));
		});
		return prop;
	}
	
	public static SimpleObjectProperty<Boolean> createConfigurableBooleanProperty(String key, String name, Boolean defaultValue) {
		ConfigService cs = ConfigService.getInstance();
		SimpleObjectProperty<Boolean> prop = new SimpleObjectProperty<>(null, name, 
				Boolean.valueOf(cs.getOrCreateProperty(key, String.valueOf(defaultValue))));
		prop.addListener((obs, oldVal, newVal) -> {
			cs.setProperty(key, String.valueOf(newVal));
		});
		return prop;
	}
	
	public static SimpleObjectProperty<Integer> createConfigurableIntegerProperty(String key, String name, Integer defaultValue) {
		ConfigService cs = ConfigService.getInstance();
		SimpleObjectProperty<Integer> prop = new SimpleObjectProperty<>(null, name, 
				Integer.valueOf(cs.getOrCreateProperty(key, String.valueOf(defaultValue))));
		prop.addListener((obs, oldVal, newVal) -> {
			cs.setProperty(key, String.valueOf(newVal));
		});
		return prop;
	}
}
