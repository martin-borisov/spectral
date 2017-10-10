package mb.spectrum;

import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Transition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
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
	
	public static Text createLabel(double x, double y, String text, Color color, List<Text> list) {
		Text label = new Text(x, y, text);
		label.setStroke(color);
		label.setCache(true);
		list.add(label);
		return label;
	}
	
	public static Transition createTextFadeInOutTransition(Node node, double fadeInMs, 
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
}
