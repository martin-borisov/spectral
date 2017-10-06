package mb.spectrum.view;

import static mb.spectrum.Utils.peakLevel;
import static mb.spectrum.Utils.rmsLevel;

import java.util.ArrayList;
import java.util.List;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import mb.spectrum.UiUtils;
import mb.spectrum.Utils;

public class StereoLevelsView extends AbstractView {
	
	// NB: The minimum DB value is empirical and it acts as a threshold in order to avoid
	// flicker of the graph caused by very low audio levels
	private static final int MIN_DB_VALUE = -100;
	
	private static final double BAR_HEIGHT_PROPORTION = 0.4;
	private static final double BAR_MARGIN_PROPORTION = 0.05;
	private static final int DB_LINES_COUNT = 5;
	private static final int LABEL_MARGIN_PX = 5;
	
	private static final Color GRID_COLOR = Color.web("#fd4a11");
	private static final Color BAR_COLOR_NORMAL = Color.LAWNGREEN;
	private static final Color BAR_COLOR_CLIP = Color.RED;
	
	private enum Mode {
		RMS, PEAK;
	}
	
	private Rectangle leftBar, rightBar;
	private List<Line> lines;
	private List<Text> labels;
	private Mode mode;
	
	public StereoLevelsView() {
		createListeners();
		mode = Mode.PEAK;
	}
	
	@Override
	protected List<Node> collectNodes() {
		
		// Create grid and labels
		lines = new ArrayList<>();
		labels = new ArrayList<>();
		for (int i = 0; i < DB_LINES_COUNT; i++) {
			double dbVal = Utils.map(i, 0, DB_LINES_COUNT, MIN_DB_VALUE, 0);
			double x = Utils.map(dbVal, MIN_DB_VALUE, 0, coordX(0), coordX(0) + sceneWidth());
			UiUtils.createGridLine(x, coordY(0), x, coordY(0) - sceneHeight(), GRID_COLOR, lines);
			Text label = UiUtils.createLabel(x, coordY(0) - sceneHeight() - LABEL_MARGIN_PX, 
					Math.round(dbVal) + " db", GRID_COLOR, labels);
			label.setX(label.getX() - label.getLayoutBounds().getWidth() / 2);
		}
		UiUtils.createGridLine(coordX(0) + sceneWidth(), coordY(0), 
				coordX(0) + sceneWidth(), coordY(0) - sceneHeight(), GRID_COLOR, lines);
		Text label = UiUtils.createLabel(coordX(0) + sceneWidth(), 
				coordY(0) - sceneHeight() - LABEL_MARGIN_PX, "0 db", GRID_COLOR, labels);
		label.setX(label.getX() - label.getLayoutBounds().getWidth() / 2);
		
		// Create bars
		leftBar = new Rectangle(coordX(0), coordY(sceneHeight() - barMargin()), 5, barHeight());
		leftBar.setFill(BAR_COLOR_NORMAL);
		rightBar = new Rectangle(coordX(0), coordY(0 + barMargin() + barHeight()), 5, barHeight());
		rightBar.setFill(BAR_COLOR_NORMAL);
		
		// Collect and return all shapes
		List<Node> nodes = new ArrayList<>();
		nodes.addAll(lines);
		nodes.addAll(labels);
		nodes.add(leftBar);
		nodes.add(rightBar);
		return nodes;
	}

	@Override
	public void dataAvailable(float[] left, float[] right) {
		
		float levelLeft = 0, levelRight = 0;
		if(Mode.RMS == mode) {
			levelLeft = rmsLevel(left);
			levelRight = rmsLevel(right);
		} else {
			levelLeft = peakLevel(left);
			levelRight = peakLevel(right);
		}
		
		double dbLeft = Utils.toDB(levelLeft);
		leftBar.setWidth(Utils.map(dbLeft, MIN_DB_VALUE, 0, 0, sceneWidth()));
		if(dbLeft > -10) {
			leftBar.setFill(BAR_COLOR_CLIP);
		} else {
			leftBar.setFill(BAR_COLOR_NORMAL);
		}
		
		double dbRight = Utils.toDB(levelRight);
		rightBar.setWidth(Utils.map(dbRight, MIN_DB_VALUE, 0, 0, sceneWidth()));
		if(dbRight > -10) {
			rightBar.setFill(BAR_COLOR_CLIP);
		} else {
			rightBar.setFill(BAR_COLOR_NORMAL);
		}
	}

	@Override
	public void nextFrame() {
	}
	
	@Override
	protected void onSceneWidthChange(Number oldValue, Number newValue) {
		
		// Update grid
		for (int i = 0; i < DB_LINES_COUNT; i++) {
			double dbVal = Utils.map(i, 0, DB_LINES_COUNT, MIN_DB_VALUE, 0);
			double x = Utils.map(dbVal, MIN_DB_VALUE, 0, coordX(0), coordX(0) + sceneWidth());
			lines.get(i).setStartX(x);
			lines.get(i).setEndX(x);
			
			Text label = labels.get(i);
			label.setX(x - label.getLayoutBounds().getWidth() / 2);
		}
		lines.get(lines.size() - 1).setStartX(coordX(0) + sceneWidth());
		lines.get(lines.size() - 1).setEndX(coordX(0) + sceneWidth());
		
		Text label = labels.get(labels.size() - 1);
		label.setX(coordX(0) + sceneWidth() - label.getLayoutBounds().getWidth() / 2);
	}

	@Override
	protected void onSceneHeightChange(Number oldValue, Number newValue) {
		
		// Update grid
		for (Line line : lines) {
			line.setStartY(coordY(0));
		}
		
		// Update bars
		leftBar.setY(coordY(sceneHeight() - barMargin()));
		leftBar.setHeight(barHeight());
		rightBar.setY(coordY(0 + barMargin() + barHeight()));
		rightBar.setHeight(barHeight());
	}

	private double barHeight() {
		return sceneHeight() * BAR_HEIGHT_PROPORTION;
	}
	
	private double barMargin() {
		return sceneHeight() * BAR_MARGIN_PROPORTION;
	}
	
	private void createListeners() {
		getScene().addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			public void handle(KeyEvent event) {
				if(KeyCode.UP == event.getCode() || KeyCode.DOWN == event.getCode()) {
					onChangeMode();
				}
			}
		});
	}
	
	private void onChangeMode() {
		if(Mode.RMS == mode) {
			mode = Mode.PEAK;
		} else {
			mode = Mode.RMS;
		}
	}

}
