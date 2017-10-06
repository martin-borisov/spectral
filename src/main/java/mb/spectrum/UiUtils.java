package mb.spectrum;

import java.util.List;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

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

}
