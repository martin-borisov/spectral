package mb.spectrum;

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
	
	public static String colorToHex(Color color) {
	    String hex1 = Integer.toHexString(color.hashCode()).toUpperCase();
	    String hex2;

	    switch (hex1.length()) {
	    case 2:
	        hex2 = "000000";
	        break;
	    case 3:
	        hex2 = String.format("00000%s", hex1.substring(0,1));
	        break;
	    case 4:
	        hex2 = String.format("0000%s", hex1.substring(0,2));
	        break;
	    case 5:
	        hex2 = String.format("000%s", hex1.substring(0,3));
	        break;
	    case 6:
	        hex2 = String.format("00%s", hex1.substring(0,4));
	        break;
	    case 7:
	        hex2 = String.format("0%s", hex1.substring(0,5));
	        break;
	    default:
	        hex2 = hex1.substring(0, 6);
	    }
	    return "#" + hex2;
	}
	
	public static SimpleObjectProperty<Color> createConfigurableColorProperty(String key, String name, Color defaultValue) {
		ConfigService cs = ConfigService.getInstance();
		SimpleObjectProperty<Color> prop = new SimpleObjectProperty<>(null, name, 
				Color.web(cs.getOrCreateProperty(key, colorToHex(defaultValue))));
		prop.addListener((obs, oldVal, newVal) -> {
			cs.setProperty(key, UiUtils.colorToHex(newVal));
		});
		return prop;
	}
	
}
