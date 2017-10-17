package mb.spectrum.view;

import static mb.spectrum.UiUtils.createGridLine;
import static mb.spectrum.UiUtils.createLabel;
import static mb.spectrum.Utils.peakLevel;
import static mb.spectrum.Utils.rmsLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import mb.spectrum.UiUtils;
import mb.spectrum.Utils;

public class StereoLevelsView extends AbstractView {
	
	// NB: The minimum DB value is empirical and it acts as a threshold in order to avoid
	// flicker of the graph caused by very low audio levels
	private static final int MIN_DB_VALUE = -100;
	
	private static final int DB_LINES_COUNT = 5;
	private static final double BAR_HEIGHT_PROPORTION = 0.4;
	private static final double BAR_MARGIN_PROPORTION = 0.05;
	private static final double DR_BAR_HEIGHT_PROPORTION = 0.42;
	private static final double DR_BAR_MARGIN_PROPORTION = 0.04;
	private static final int LABEL_MARGIN_PX = 5;
	
	private static final SimpleObjectProperty<Color> propBarColorNormal = 
			new SimpleObjectProperty<>(null, "Normal Level Color", Color.LAWNGREEN);
	private static final SimpleObjectProperty<Color> propBarColorClip = 
			new SimpleObjectProperty<>(null, "Clip Level Color", Color.RED);
	private static final SimpleObjectProperty<Double> propBarOpacity = 
			new SimpleObjectProperty<>(null, "Bar Opacity", 0.9);
	private static final SimpleObjectProperty<Color> propDrBarColor = 
			new SimpleObjectProperty<>(null, "DR Range Color", Color.ORANGE);
	private static final SimpleObjectProperty<Double> propDrBarOpacity = 
			new SimpleObjectProperty<>(null, "DR Range Opacity", 0.2);
	private static final SimpleObjectProperty<Color> propGridColor = 
			new SimpleObjectProperty<Color>(null, "Grid Color", Color.web("#fd4a11"));
	private static final SimpleObjectProperty<Boolean> propShowDr = 
			new SimpleObjectProperty<Boolean>(null, "Show Dynamic Range", true);
	private static final SimpleObjectProperty<Boolean> propRms = 
			new SimpleObjectProperty<Boolean>(null, "RMS Mode", false);
	
	private List<Line> lines;
	private List<Text> labels;
	private Rectangle leftBar, rightBar, leftDrBar, rightDrBar;
	private Line leftMinLevel, leftMaxLevel, rightMinLevel, rightMaxLevel;
	
	private double currentDbL, currentDbR, 
		minLevelL = 0, minLevelR = 0, 
		maxLevelL = MIN_DB_VALUE, maxLevelR = MIN_DB_VALUE;
	
	public StereoLevelsView() {
		createPropertyListeners();
	}
	
	@Override
	public String getName() {
		return "Peak/RMS Meter";
	}

	@Override
	public List<ObjectProperty<? extends Object>> getProperties() {
		return Arrays.asList(propGridColor, propBarOpacity, propBarColorNormal, 
				propBarColorClip, propRms, propDrBarColor, propDrBarOpacity, propShowDr);
	}

	@Override
	protected List<Node> collectNodes() {
		
		// Create grid and labels
		lines = new ArrayList<>();
		labels = new ArrayList<>();
		for (int i = 0; i < DB_LINES_COUNT; i++) {
			double dbVal = Utils.map(i, 0, DB_LINES_COUNT, MIN_DB_VALUE, 0);
			double x = Utils.map(dbVal, MIN_DB_VALUE, 0, coordX(0), coordX(0) + areaWidth());
			createGridLine(x, coordY(0), x, coordY(0) - areaHeight(), propGridColor.getValue(), lines)
				.strokeProperty().bind(propGridColor);
			Text label = createLabel(x, coordY(0) - areaHeight() - LABEL_MARGIN_PX, 
					Math.round(dbVal) + " db", propGridColor.getValue(), labels);
			label.setX(label.getX() - label.getLayoutBounds().getWidth() / 2);
			label.strokeProperty().bind(propGridColor);
		}
		createGridLine(coordX(0) + areaWidth(), coordY(0), 
				coordX(0) + areaWidth(), coordY(0) - areaHeight(), propGridColor.getValue(), lines)
			.strokeProperty().bind(propGridColor);
		Text label = createLabel(coordX(0) + areaWidth(), 
				coordY(0) - areaHeight() - LABEL_MARGIN_PX, "0 db", propGridColor.getValue(), labels);
		label.setX(label.getX() - label.getLayoutBounds().getWidth() / 2);
		label.strokeProperty().bind(propGridColor);
		
		// Level bars
		leftBar = new Rectangle(coordX(0), coordY(areaHeight() - barMargin()), 5, barHeight());
		leftBar.opacityProperty().bind(propBarOpacity);
		
		rightBar = new Rectangle(coordX(0), coordY(0 + barMargin() + barHeight()), 5, barHeight());
		rightBar.opacityProperty().bind(propBarOpacity);
		
		updateBarColors();
		
		// DR bars
		leftDrBar = new Rectangle(coordX(0), coordY(areaHeight() - drBarMargin()), 5, drBarHeight());
		leftDrBar.fillProperty().bind(propDrBarColor);
		leftDrBar.opacityProperty().bind(propDrBarOpacity);
		
		rightDrBar = new Rectangle(coordX(0), coordY(0 + drBarMargin() + drBarHeight()), 5, drBarHeight());
		rightDrBar.fillProperty().bind(propDrBarColor);
		rightDrBar.opacityProperty().bind(propDrBarOpacity);
		
		// DR indicators
		leftMinLevel = UiUtils.createThickRoundedLine(Color.BLUE);
		leftMinLevel.startXProperty().bind(leftDrBar.xProperty());
		leftMinLevel.endXProperty().bind(leftDrBar.xProperty());
		leftMinLevel.startYProperty().bind(leftDrBar.yProperty());
		leftMinLevel.endYProperty().bind(leftDrBar.yProperty().add(leftDrBar.heightProperty()));
		
		leftMaxLevel = UiUtils.createThickRoundedLine(Color.ORANGE);
		leftMaxLevel.startXProperty().bind(leftDrBar.xProperty().add(leftDrBar.widthProperty()));
		leftMaxLevel.endXProperty().bind(leftDrBar.xProperty().add(leftDrBar.widthProperty()));
		leftMaxLevel.startYProperty().bind(leftDrBar.yProperty());
		leftMaxLevel.endYProperty().bind(leftDrBar.yProperty().add(leftDrBar.heightProperty()));
		
		rightMinLevel = UiUtils.createThickRoundedLine(Color.BLUE);
		rightMinLevel.startXProperty().bind(rightDrBar.xProperty());
		rightMinLevel.endXProperty().bind(rightDrBar.xProperty());
		rightMinLevel.startYProperty().bind(rightDrBar.yProperty());
		rightMinLevel.endYProperty().bind(rightDrBar.yProperty().add(rightDrBar.heightProperty()));
		
		rightMaxLevel = UiUtils.createThickRoundedLine(Color.ORANGE);
		rightMaxLevel.startXProperty().bind(rightDrBar.xProperty().add(rightDrBar.widthProperty()));
		rightMaxLevel.endXProperty().bind(rightDrBar.xProperty().add(rightDrBar.widthProperty()));
		rightMaxLevel.startYProperty().bind(rightDrBar.yProperty());
		rightMaxLevel.endYProperty().bind(rightDrBar.yProperty().add(rightDrBar.heightProperty()));
		
		
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
		nodes.addAll(Arrays.asList(
				leftBar, rightBar, leftDrBar, rightDrBar, 
				leftMinLevel, leftMaxLevel, rightMinLevel, rightMaxLevel));
		return nodes;
	}
	
	private void createPropertyListeners() {
		propBarColorNormal.addListener(new ChangeListener<Color>() {
			public void changed(ObservableValue<? extends Color> observable, Color oldValue, Color newValue) {
				updateBarColors();
			}
		});
		propBarColorClip.addListener(new ChangeListener<Color>() {
			public void changed(ObservableValue<? extends Color> observable, Color oldValue, Color newValue) {
				updateBarColors();
			}
		});
		propShowDr.addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				toggleDrVisibility(newValue);
			}
		});
	}
	
	private void updateBarColors() {
		leftBar.setFill(createHorizontalGradient(
				0, areaWidth(), areaHeight() - barMargin() - barHeight() / 2));
		rightBar.setFill(createHorizontalGradient(
				0, areaWidth(), barMargin() + barHeight() / 2));
	}
	
	/* Handlers */

	@Override
	public void dataAvailable(float[] left, float[] right) {
		
		float levelLeft = 0, levelRight = 0;
		if(propRms.getValue()) {
			levelLeft = rmsLevel(left);
			levelRight = rmsLevel(right);
		} else {
			levelLeft = peakLevel(left);
			levelRight = peakLevel(right);
		}
		
		currentDbL = Utils.toDB(levelLeft);
		currentDbR = Utils.toDB(levelRight);
		
		minLevelL = Math.min(minLevelL, currentDbL);
		minLevelR = Math.min(minLevelR, currentDbR);
		maxLevelL = Math.max(maxLevelL, currentDbL);
		maxLevelR = Math.max(maxLevelR, currentDbR);
	}

	@Override
	public void nextFrame() {
		leftBar.setWidth(Utils.map(currentDbL, MIN_DB_VALUE, 0, 0, areaWidth()));
		rightBar.setWidth(Utils.map(currentDbR, MIN_DB_VALUE, 0, 0, areaWidth()));
		leftDrBar.setX(coordX(Utils.map(minLevelL, MIN_DB_VALUE, 0, 0, areaWidth())));
		leftDrBar.setWidth(coordX(Utils.map(maxLevelL, MIN_DB_VALUE, 0, 0, areaWidth())) - leftDrBar.getX());
		rightDrBar.setX(coordX(Utils.map(minLevelR, MIN_DB_VALUE, 0, 0, areaWidth())));
		rightDrBar.setWidth(coordX(Utils.map(maxLevelR, MIN_DB_VALUE, 0, 0, areaWidth())) - rightDrBar.getX());
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
		
		leftDrBar.setY(coordY(areaHeight() - drBarMargin()));
		leftDrBar.setHeight(drBarHeight());
		rightDrBar.setY(coordY(0 + drBarMargin() + drBarHeight()));
		rightDrBar.setHeight(drBarHeight());
	}

	@Override
	public void onShow() {
	}

	@Override
	public void onHide() {
	}

	private double barHeight() {
		return areaHeight() * BAR_HEIGHT_PROPORTION;
	}
	
	private double barMargin() {
		return areaHeight() * BAR_MARGIN_PROPORTION;
	}
	
	private double drBarHeight() {
		return areaHeight() * DR_BAR_HEIGHT_PROPORTION;
	}
	
	private double drBarMargin() {
		return areaHeight() * DR_BAR_MARGIN_PROPORTION;
	}
	
	private LinearGradient createHorizontalGradient(double fromX, double toX, double y) {
		LinearGradient gr = new LinearGradient(
				coordX(fromX), coordY(y), 
				coordX(toX), coordY(y), 
				false, CycleMethod.NO_CYCLE, 
				new Stop[]{
						new Stop(0, propBarColorNormal.getValue()), 
						new Stop(0.8, propBarColorNormal.getValue()), 
						new Stop(0.9, propBarColorClip.getValue())});
		return gr;
	}
	
	private void toggleDrVisibility(boolean visible) {
		leftDrBar.setVisible(visible);
		rightDrBar.setVisible(visible);
		leftMinLevel.setVisible(visible);
		leftMaxLevel.setVisible(visible);
		rightMinLevel.setVisible(visible);
		rightMaxLevel.setVisible(visible);
	}

}
