package mb.spectrum.view;

import static mb.spectrum.UiUtils.createGridLine;
import static mb.spectrum.UiUtils.createLabel;
import static mb.spectrum.Utils.peakLevel;
import static mb.spectrum.Utils.rmsLevel;

import java.util.ArrayList;
import java.util.List;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import mb.spectrum.Utils;

public class StereoLevelsView extends AbstractView implements EventHandler<Event> {
	
	// NB: The minimum DB value is empirical and it acts as a threshold in order to avoid
	// flicker of the graph caused by very low audio levels
	private static final int MIN_DB_VALUE = -100;
	
	private static final int DB_LINES_COUNT = 5;
	private static final double BAR_HEIGHT_PROPORTION = 0.4;
	private static final double BAR_MARGIN_PROPORTION = 0.05;
	private static final int LABEL_MARGIN_PX = 5;
	
	private static final Color GRID_COLOR = Color.web("#fd4a11");
	private static final Color BAR_COLOR_NORMAL = Color.LAWNGREEN;
	private static final Color BAR_COLOR_CLIP = Color.RED;
	private static final float BAR_OPACITY = 0.9f;
	
	private enum Mode {
		RMS, PEAK;
	}
	
	private Mode mode;
	private List<Line> lines;
	private List<Text> labels;
	private Rectangle leftBar, rightBar;
	
	private double currentDbLeft, currentDbRight;
	
	public StereoLevelsView() {
		mode = Mode.PEAK;
	}
	
	@Override
	public String getName() {
		return "Peak/RMS Meter";
	}



	@Override
	protected List<Node> collectNodes() {
		
		// Create grid and labels
		lines = new ArrayList<>();
		labels = new ArrayList<>();
		for (int i = 0; i < DB_LINES_COUNT; i++) {
			double dbVal = Utils.map(i, 0, DB_LINES_COUNT, MIN_DB_VALUE, 0);
			double x = Utils.map(dbVal, MIN_DB_VALUE, 0, coordX(0), coordX(0) + areaWidth());
			createGridLine(x, coordY(0), x, coordY(0) - areaHeight(), GRID_COLOR, lines);
			Text label = createLabel(x, coordY(0) - areaHeight() - LABEL_MARGIN_PX, 
					Math.round(dbVal) + " db", GRID_COLOR, labels);
			label.setX(label.getX() - label.getLayoutBounds().getWidth() / 2);
		}
		createGridLine(coordX(0) + areaWidth(), coordY(0), 
				coordX(0) + areaWidth(), coordY(0) - areaHeight(), GRID_COLOR, lines);
		Text label = createLabel(coordX(0) + areaWidth(), 
				coordY(0) - areaHeight() - LABEL_MARGIN_PX, "0 db", GRID_COLOR, labels);
		label.setX(label.getX() - label.getLayoutBounds().getWidth() / 2);
		
		// Create bars
		leftBar = new Rectangle(coordX(0), coordY(areaHeight() - barMargin()), 5, barHeight());
		leftBar.setFill(createHorizontalGradient(
				0, areaWidth(), areaHeight() - barMargin() - barHeight() / 2));
		leftBar.setOpacity(BAR_OPACITY);
		
		rightBar = new Rectangle(coordX(0), coordY(0 + barMargin() + barHeight()), 5, barHeight());
		leftBar.setFill(createHorizontalGradient(
				0, areaWidth(), barMargin() + barHeight() / 2));
		rightBar.setOpacity(BAR_OPACITY);
		
		// ###
		/*
		Image image = new Image(getClass().getResourceAsStream("/resources/page-bg-2.jpg"));
		
		BackgroundImage myBI= new BackgroundImage(image,
		        BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT,
		          BackgroundSize.DEFAULT);
		
		Region region = new Region();
		region.setBackground(new Background(myBI));
		region.prefWidthProperty().bind(getRoot().widthProperty());
		region.prefHeightProperty().bind(getRoot().heightProperty());
		*/
		// ###
		
		// Collect and return all shapes
		List<Node> nodes = new ArrayList<>();
		//nodes.add(region);
		nodes.addAll(lines);
		nodes.addAll(labels);
		nodes.add(leftBar);
		nodes.add(rightBar);
		return nodes;
	}
	
	/* Handlers */

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
		
		currentDbLeft = Utils.toDB(levelLeft);
		currentDbRight = Utils.toDB(levelRight);
	}

	@Override
	public void nextFrame() {
		leftBar.setWidth(Utils.map(currentDbLeft, MIN_DB_VALUE, 0, 0, areaWidth()));
		rightBar.setWidth(Utils.map(currentDbRight, MIN_DB_VALUE, 0, 0, areaWidth()));
	}
	
	@Override
	protected void onSceneWidthChange(Number oldValue, Number newValue) {
		
		// Update grid
		for (int i = 0; i < DB_LINES_COUNT; i++) {
			double dbVal = Utils.map(i, 0, DB_LINES_COUNT, MIN_DB_VALUE, 0);
			double x = Utils.map(dbVal, MIN_DB_VALUE, 0, coordX(0), coordX(0) + areaWidth());
			lines.get(i).setStartX(x);
			lines.get(i).setEndX(x);
			
			Text label = labels.get(i);
			label.setX(x - label.getLayoutBounds().getWidth() / 2);
		}
		lines.get(lines.size() - 1).setStartX(coordX(0) + areaWidth());
		lines.get(lines.size() - 1).setEndX(coordX(0) + areaWidth());
		
		Text label = labels.get(labels.size() - 1);
		label.setX(coordX(0) + areaWidth() - label.getLayoutBounds().getWidth() / 2);
		
		// Update bars
		leftBar.setFill(createHorizontalGradient(
				0, areaWidth(), areaHeight() - barMargin() - barHeight() / 2));
		rightBar.setFill(createHorizontalGradient(
				0, areaWidth(), barMargin() + barHeight() / 2));
	}

	@Override
	protected void onSceneHeightChange(Number oldValue, Number newValue) {
		
		// Update grid
		for (Line line : lines) {
			line.setStartY(coordY(0));
		}
		
		// Update bars
		leftBar.setY(coordY(areaHeight() - barMargin()));
		leftBar.setHeight(barHeight());
		leftBar.setFill(createHorizontalGradient(
				0, areaWidth(), areaHeight() - barMargin() - barHeight() / 2));
		
		rightBar.setY(coordY(0 + barMargin() + barHeight()));
		rightBar.setHeight(barHeight());
		rightBar.setFill(createHorizontalGradient(
				0, areaWidth(), barMargin() + barHeight() / 2));
	}

	@Override
	public void onShow() {
		getRoot().getScene().addEventHandler(KeyEvent.KEY_RELEASED, this);
	}

	@Override
	public void onHide() {
		getRoot().getScene().removeEventHandler(KeyEvent.KEY_RELEASED, this);
	}
	
	@Override
	public void handle(Event event) {
		if(KeyEvent.KEY_RELEASED == event.getEventType()) {
			KeyEvent evt = (KeyEvent) event;
			if(KeyCode.UP == evt.getCode() || KeyCode.DOWN == evt.getCode()) {
				onChangeMode();
			}
		}
	}

	private void onChangeMode() {
		if(Mode.RMS == mode) {
			mode = Mode.PEAK;
		} else {
			mode = Mode.RMS;
		}
	}

	private double barHeight() {
		return areaHeight() * BAR_HEIGHT_PROPORTION;
	}
	
	private double barMargin() {
		return areaHeight() * BAR_MARGIN_PROPORTION;
	}
	
	private LinearGradient createHorizontalGradient(double fromX, double toX, double y) {
		LinearGradient gr = new LinearGradient(
				coordX(fromX), coordY(y), 
				coordX(toX), coordY(y), 
				false, CycleMethod.NO_CYCLE, 
				new Stop[]{
						new Stop(0, BAR_COLOR_NORMAL), 
						new Stop(0.8, BAR_COLOR_NORMAL), 
						new Stop(0.9, BAR_COLOR_CLIP)});
		return gr;
	}

}
