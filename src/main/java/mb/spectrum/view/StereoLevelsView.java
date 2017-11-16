package mb.spectrum.view;

import static mb.spectrum.UiUtils.createConfigurableBooleanProperty;
import static mb.spectrum.UiUtils.createConfigurableColorProperty;
import static mb.spectrum.UiUtils.createConfigurableDoubleProperty;
import static mb.spectrum.UiUtils.createLabel;
import static mb.spectrum.Utils.map;
import static mb.spectrum.Utils.peakLevel;
import static mb.spectrum.Utils.rmsLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import mb.spectrum.UiUtils;
import mb.spectrum.Utils;
import mb.spectrum.prop.ConfigurableProperty;

public class StereoLevelsView extends AbstractView {
	
	// NB: The minimum DB value is empirical and it acts as a threshold in order to avoid
	// flicker of the graph caused by very low audio levels
	// TODO It will be cool to make this value configurable
	private static final int MIN_DB_VALUE = -66;
	
	private static final int DB_LINES_COUNT = 8;
	private static final double BAR_HEIGHT_PROPORTION = 0.35;
	private static final double BAR_MARGIN_PROPORTION = 0.05;
	private static final double DR_BAR_HEIGHT_PROPORTION = 0.37;
	private static final double DR_BAR_MARGIN_PROPORTION = 0.04;
	private static final double GRID_MARGIN_RATIO = 0.07;
	private static final double LINGER_STAY_FACTOR = 0.01;
	private static final double LINGER_ACCELARATION_FACTOR = 1.08;
	
	private ConfigurableProperty<Color> propGridColor;
	private ConfigurableProperty<Color> propBarColorNormal;
	private ConfigurableProperty<Color> propBarColorMid;
	private ConfigurableProperty<Color> propBarColorClip;
	private ConfigurableProperty<Color> propLingerIndicatorColor;
	private ConfigurableProperty<Double> propBarOpacity;
	private ConfigurableProperty<Color> propDrBarColor;
	private ConfigurableProperty<Double> propDrBarOpacity;
	private ConfigurableProperty<Boolean> propShowDr;
	private ConfigurableProperty<Boolean> propRms;
	
	private List<Line> lines;
	private List<Label> labels;
	private Rectangle leftBar, rightBar, leftDrBar, rightDrBar;
	private Line leftMinLevel, leftMaxLevel, rightMinLevel, rightMaxLevel, leftLingerLevel, rightLingerLevel; 
	
	private double currentDbL, currentDbR, 
		minLevelL = 0, minLevelR = 0, 
		maxLevelL = MIN_DB_VALUE, maxLevelR = MIN_DB_VALUE;
	
	private double lingerLevelL = MIN_DB_VALUE, lingerLevelR = MIN_DB_VALUE, 
			lingerOpValL = LINGER_STAY_FACTOR, lingerOpValR = LINGER_STAY_FACTOR;
	
	private DoubleProperty currLevelLProp, currLevelRProp, minLevelLProp, 
		maxLevelLProp, minLevelRProp, maxLevelRProp, leftLingerLevelProp, rightLingerLevelProp;
	
	@Override
	public String getName() {
		return "Stereo Peak/RMS Meters";
	}
	
	@Override
	protected void initProperties() {
		
		// Configuration properties
		propGridColor = createConfigurableColorProperty(
				"stereoLevelsView.gridColor", "Grid Color", Color.web("#fd4a11"));
		propBarColorNormal = createConfigurableColorProperty(
				"stereoLevelsView.normalLevelColor", "Normal Level Color", Color.DARKGREEN);
		propBarColorMid = createConfigurableColorProperty(
				"stereoLevelsView.middleLevelColor", "Middle Level Color", Color.LAWNGREEN);
		propBarColorClip = createConfigurableColorProperty(
				"stereoLevelsView.clipLevelColor", "Clip Level Color", Color.RED);
		propLingerIndicatorColor = createConfigurableColorProperty(
				"stereoLevelsView.lingerIndicatorColor","Linger Level Color", Color.LIGHTGREEN);
		propBarOpacity = createConfigurableDoubleProperty(
				"stereoLevelsView.barOpacity", "Bar Opacity", 0.1, 1.0, 0.9, 0.1);
		propDrBarColor = createConfigurableColorProperty(
				"stereoLevelsView.drBarColor", "D/R Bar Color", Color.ORANGE);
		propDrBarOpacity = createConfigurableDoubleProperty(
				"stereoLevelsView.drBarOpacity", "D/R Bar Opacity", 0.1, 1.0, 0.2, 0.1);
		propShowDr = createConfigurableBooleanProperty(
				"stereoLevelsView.showDynamicRange", "Show Dynamic Range", true);
		propRms = createConfigurableBooleanProperty(
				"stereoLevelsView.enableRmsMode", "RMS Mode", false);
		
		// Operational properties
		currLevelLProp = new SimpleDoubleProperty();
		currLevelRProp = new SimpleDoubleProperty();
		minLevelLProp = new SimpleDoubleProperty();
		maxLevelLProp = new SimpleDoubleProperty();
		minLevelRProp = new SimpleDoubleProperty();
		maxLevelRProp = new SimpleDoubleProperty();
		leftLingerLevelProp = new SimpleDoubleProperty();
		rightLingerLevelProp = new SimpleDoubleProperty();
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Arrays.asList(propGridColor, propBarOpacity, propBarColorNormal, 
				propBarColorMid, propLingerIndicatorColor, propBarColorClip, propRms, 
				propDrBarColor, propDrBarOpacity, propShowDr);
	}

	@Override
	protected List<Node> collectNodes() {
		
		// Create grid and labels
		lines = new ArrayList<>();
		labels = new ArrayList<>();
		for (int i = 0; i <= DB_LINES_COUNT; i++) {
			createGridLineAndLabel(i);
		}
		
		// Level bars
		leftBar = createLevelBar(
				getRoot().heightProperty().multiply(SCENE_MARGIN_RATIO + BAR_MARGIN_PROPORTION), 
				currLevelLProp);
		
		rightBar = createLevelBar(
				getRoot().heightProperty().subtract(
						getRoot().heightProperty().multiply(
								SCENE_MARGIN_RATIO + BAR_MARGIN_PROPORTION + BAR_HEIGHT_PROPORTION)), 
				currLevelRProp);
		
		// RMS indicators
		leftLingerLevel = createLingerIndicator(leftLingerLevelProp, leftBar);
		rightLingerLevel = createLingerIndicator(rightLingerLevelProp, rightBar);
		
		// DR bars
		leftDrBar = createDrBar(
				getRoot().heightProperty().multiply(SCENE_MARGIN_RATIO + DR_BAR_MARGIN_PROPORTION), 
				minLevelLProp, maxLevelLProp);
		
		rightDrBar = createDrBar(getRoot().heightProperty().subtract(
				getRoot().heightProperty().multiply(
						SCENE_MARGIN_RATIO + DR_BAR_MARGIN_PROPORTION + DR_BAR_HEIGHT_PROPORTION)), 
				minLevelRProp, maxLevelRProp);
		
		// DR indicators
		leftMinLevel = createMinDrLine(leftDrBar);
		leftMaxLevel = createMaxDrLine(leftDrBar);
		rightMinLevel = createMinDrLine(rightDrBar);
		rightMaxLevel = createMaxDrLine(rightDrBar);
		
		// Collect and return all nodes
		List<Node> nodes = new ArrayList<>();
		nodes.addAll(lines);
		nodes.addAll(labels);
		nodes.addAll(Arrays.asList(
				leftDrBar, rightDrBar, leftBar, rightBar, leftLingerLevel, rightLingerLevel, 
				leftMinLevel, leftMaxLevel, rightMinLevel, rightMaxLevel));
		
		/*
		Image image = new Image(getClass().getResourceAsStream("/resources/page-bg-2.jpg"));
		
		BackgroundImage myBI= new BackgroundImage(image,
		        BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT,
		          BackgroundSize.DEFAULT);
		
		Region region = new Region();
		region.setBackground(new Background(myBI));
		region.prefWidthProperty().bind(getRoot().widthProperty());
		region.prefHeightProperty().bind(getRoot().heightProperty());
		nodes.add(region);
		*/
		
		return nodes;
	}
	
	private void createGridLineAndLabel(int idx) {
		
		double dBVal = map(idx, 0, DB_LINES_COUNT, MIN_DB_VALUE, 0);
		
		// Create line
		Line line = new Line();
		line.startXProperty().bind(
				Bindings.createDoubleBinding(() -> {
					double parentWidth = getRoot().widthProperty().get();
					return map(dBVal, MIN_DB_VALUE, 0, 
						parentWidth * SCENE_MARGIN_RATIO, 
						parentWidth - parentWidth * SCENE_MARGIN_RATIO);
					}, 
					getRoot().widthProperty()));
		line.endXProperty().bind(line.startXProperty());
		line.startYProperty().bind(getRoot().heightProperty().multiply(GRID_MARGIN_RATIO));
		line.endYProperty().bind(getRoot().heightProperty().subtract(
				getRoot().heightProperty().multiply(GRID_MARGIN_RATIO)));
		line.strokeProperty().bind(propGridColor.getProp());
		line.getStrokeDashArray().addAll(2d);
		line.setCache(true);
		lines.add(line);
		
		// Create label
		Label label = createLabel(Math.round(dBVal) + "dB", labels);
		label.layoutXProperty().bind(line.startXProperty().subtract(label.widthProperty().divide(2)));
		label.layoutYProperty().bind(line.startYProperty().subtract(label.heightProperty()));
		label.textFillProperty().bind(propGridColor.getProp());
		label.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", Bindings.createDoubleBinding(
						() -> Math.sqrt(getRoot().widthProperty().get() / 3), 
						getRoot().widthProperty())));
	}
	
	private Rectangle createLevelBar(DoubleBinding yBinding, DoubleProperty levelProp) {
		Rectangle bar = new Rectangle();
		bar.xProperty().bind(lines.get(0).startXProperty());
		bar.yProperty().bind(yBinding);
		bar.heightProperty().bind(getRoot().heightProperty().multiply(BAR_HEIGHT_PROPORTION));
		bar.opacityProperty().bind(propBarOpacity.getProp());
		createLevelBarExprBinding(bar, levelProp);
		
		ReadOnlyDoubleProperty rootWidthProp = getRoot().widthProperty();
		bar.styleProperty().bind(
				Bindings.concat(
						"-fx-fill: ", 
							"linear-gradient(from ", bar.xProperty(), "px ", bar.yProperty(), "px to ", 
								Bindings.createDoubleBinding(
										() -> (rootWidthProp.get() - rootWidthProp.get() * SCENE_MARGIN_RATIO), rootWidthProp), 
								"px ", bar.yProperty(), ",", 
								Bindings.createStringBinding(() -> (UiUtils.colorToWeb(propBarColorNormal.getProp().get())), propBarColorNormal.getProp()), " 0%, ", 
								Bindings.createStringBinding(() -> (UiUtils.colorToWeb(propBarColorMid.getProp().get())), propBarColorMid.getProp()), " 80%, ", 
								Bindings.createStringBinding(() -> (UiUtils.colorToWeb(propBarColorClip.getProp().get())), propBarColorClip.getProp()), " 90%)"
				));
		return bar;
	}
	
	private Line createLingerIndicator(DoubleProperty lingerLevelProp, Rectangle levelBar) {
		Line line = new Line();
		line.startXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					return map(lingerLevelProp.get(), MIN_DB_VALUE, 0, 
							parentWidth * SCENE_MARGIN_RATIO, parentWidth - parentWidth * SCENE_MARGIN_RATIO);
				}, 
				lingerLevelProp, getRoot().widthProperty()));
		line.endXProperty().bind(line.startXProperty());
		line.startYProperty().bind(levelBar.yProperty());
		line.endYProperty().bind(levelBar.yProperty().add(levelBar.heightProperty()));
		
		line.strokeProperty().bind(propLingerIndicatorColor.getProp());
		line.setStrokeLineCap(StrokeLineCap.ROUND);
		line.setStrokeWidth(4);
		line.opacityProperty().bind(propBarOpacity.getProp());
		return line;
	}
	
	private Rectangle createDrBar(DoubleBinding yBinding, DoubleProperty minLevelProp, DoubleProperty maxLevelProp) {
		Rectangle bar = new Rectangle();
		bar.yProperty().bind(yBinding);
		bar.heightProperty().bind(getRoot().heightProperty().multiply(DR_BAR_HEIGHT_PROPORTION));
		bar.fillProperty().bind(propDrBarColor.getProp());
		bar.opacityProperty().bind(propDrBarOpacity.getProp());
		bar.visibleProperty().bind(propShowDr.getProp());
		createDrBarExprBinding(bar, minLevelProp, maxLevelProp);
		return bar;
	}
	
	private Line createMinDrLine(Rectangle drBar) {
		Line line = UiUtils.createThickRoundedLine(Color.BLUE);
		line.startXProperty().bind(drBar.xProperty());
		line.endXProperty().bind(drBar.xProperty());
		line.startYProperty().bind(drBar.yProperty());
		line.endYProperty().bind(drBar.yProperty().add(drBar.heightProperty()));
		line.visibleProperty().bind(propShowDr.getProp());
		return line;
	}
	
	private Line createMaxDrLine(Rectangle drBar) {
		Line line = UiUtils.createThickRoundedLine(Color.ORANGE);
		line.startXProperty().bind(drBar.xProperty().add(drBar.widthProperty()));
		line.endXProperty().bind(drBar.xProperty().add(drBar.widthProperty()));
		line.startYProperty().bind(drBar.yProperty());
		line.endYProperty().bind(drBar.yProperty().add(drBar.heightProperty()));
		line.visibleProperty().bind(propShowDr.getProp());
		return line;
	}
	
	/* Handlers */

	@Override
	public void dataAvailable(float[] left, float[] right) {
		
		float levelLeft = 0, levelRight = 0;
		if(propRms.getProp().get()) {
			levelLeft = rmsLevel(left);
			levelRight = rmsLevel(right);
		} else {
			levelLeft = peakLevel(left);
			levelRight = peakLevel(right);
		}
		
		currentDbL = Utils.toDB(levelLeft);
		currentDbR = Utils.toDB(levelRight);
		
		minLevelL = Math.min(minLevelL, currentDbL);
		maxLevelL = Math.max(maxLevelL, currentDbL);
		minLevelR = Math.min(minLevelR, currentDbR);
		maxLevelR = Math.max(maxLevelR, currentDbR);
	}

	@Override
	public void nextFrame() {
		
		// Update operational properties from UI thread
		currLevelLProp.set(currentDbL);
		currLevelRProp.set(currentDbR);
		minLevelLProp.set(minLevelL);
		maxLevelLProp.set(maxLevelL);
		minLevelRProp.set(minLevelR);
		maxLevelRProp.set(maxLevelR);
		
		// Update linger levels
		lingerLevelL = lingerLevelL - lingerOpValL;
		lingerOpValL = lingerOpValL * LINGER_ACCELARATION_FACTOR;
		
		if(currentDbL > lingerLevelL) {
			lingerLevelL = currentDbL;
			lingerOpValL = LINGER_STAY_FACTOR;
		}
		if(lingerLevelL < MIN_DB_VALUE) {
			lingerLevelL = MIN_DB_VALUE;
		}
		leftLingerLevelProp.set(lingerLevelL);
		
		lingerLevelR = lingerLevelR - lingerOpValR;
		lingerOpValR = lingerOpValR * LINGER_ACCELARATION_FACTOR;
		
		if(currentDbR > lingerLevelR) {
			lingerLevelR = currentDbR;
			lingerOpValR = LINGER_STAY_FACTOR;
		}
		if(lingerLevelR < MIN_DB_VALUE) {
			lingerLevelR = MIN_DB_VALUE;
		}
		rightLingerLevelProp.set(lingerLevelR);
	}

	@Override
	public void onShow() {
	}

	@Override
	public void onHide() {
	}
	
	/* Binding Logic */

	private void createLevelBarExprBinding(Rectangle bar, DoubleProperty prop) {
		
		bar.widthProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					return map(prop.get(), MIN_DB_VALUE, 0, 
							0, parentWidth - parentWidth * SCENE_MARGIN_RATIO * 2);
				}, 
				prop, getRoot().widthProperty()));
	}
	
	private void createDrBarExprBinding(Rectangle bar, 
			DoubleProperty minLevelProp, DoubleProperty maxLevelProp) {
		
		bar.xProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					return map(minLevelProp.get(), MIN_DB_VALUE, 0,
							parentWidth * SCENE_MARGIN_RATIO, parentWidth - parentWidth * SCENE_MARGIN_RATIO);
				}, 
				minLevelProp, getRoot().widthProperty()));
		
		bar.widthProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					return map(maxLevelProp.get(), MIN_DB_VALUE, 0,
							parentWidth * SCENE_MARGIN_RATIO, parentWidth - parentWidth * SCENE_MARGIN_RATIO) - bar.xProperty().get();
				}, 
				maxLevelProp, getRoot().widthProperty(), bar.xProperty()));
	}

}
