package mb.spectrum.view;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import mb.spectrum.Utils;

public class StereoLevelsView extends AbstractView {
	
	private static final double BAR_HEIGHT_PROPORTION = 0.4;
	private static final int DB_LINES_COUNT = 5;
	
	private Rectangle leftBar, rightBar;
	private List<Line> lines;
	
	@Override
	protected List<Node> collectNodes() {
		
		// Create grid
		lines = new ArrayList<>();
		for (int i = 0; i < DB_LINES_COUNT; i++) {
			double dbVal = Utils.map(i, 0, DB_LINES_COUNT, 0, -165);
			double x = Utils.map(dbVal, 0, -165, coordX(0), coordX(0) + sceneWidth());
			createLine(x, coordY(0), x, coordY(0) - sceneHeight(), lines);
		}
		createLine(coordX(0) + sceneWidth(), coordY(0), 
				coordX(0) + sceneWidth(), coordY(0) - sceneHeight(), lines);
		
		// Create bars
		leftBar = new Rectangle(coordX(0), coordY(0) - barHeight(), 5, barHeight());
		leftBar.setFill(Color.LAWNGREEN);
		rightBar = new Rectangle(coordX(0), coordY(sceneHeight() - barHeight()) - barHeight(), 5, barHeight());
		rightBar.setFill(Color.LAWNGREEN);
		
		// Collect and return all shapes
		List<Node> nodes = new ArrayList<>(lines);
		nodes.add(leftBar);
		nodes.add(rightBar);
		return nodes;
	}

	@Override
	public void dataAvailable(float[] left, float[] right) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void nextFrame() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void onSceneWidthChange(Number oldValue, Number newValue) {
		
		// Update grid
		for (int i = 0; i < DB_LINES_COUNT; i++) {
			double dbVal = Utils.map(i, 0, DB_LINES_COUNT, 0, -165);
			double x = Utils.map(dbVal, 0, -165, coordX(0), coordX(0) + sceneWidth());
			lines.get(i).setStartX(x);
			lines.get(i).setEndX(x);
		}
		lines.get(lines.size() - 1).setStartX(coordX(0) + sceneWidth());
		lines.get(lines.size() - 1).setEndX(coordX(0) + sceneWidth());
		
		// TODO Update bars
	}

	@Override
	protected void onSceneHeightChange(Number oldValue, Number newValue) {
		
		// Update grid
		for (Line line : lines) {
			line.setStartY(coordY(0));
		}
		
		// Update bars
		leftBar.setY(coordY(0) - barHeight());
		leftBar.setHeight(barHeight());
		rightBar.setY(coordY(sceneHeight() - barHeight()) - barHeight());
		rightBar.setHeight(barHeight());
	}

	private double barHeight() {
		return sceneHeight() * BAR_HEIGHT_PROPORTION;
	}
	
	private Line createLine(double startX, double startY, double endX, double endY, List<Line> list) {
		Line line = new Line(startX, startY, endX, endY);
		line.setStroke(Color.web("#fd4a11"));
		line.getStrokeDashArray().addAll(2d);
		line.setCache(true);
		list.add(line);
		return line;
	}

}
