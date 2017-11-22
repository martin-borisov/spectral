package mb.spectrum.view;

import static mb.spectrum.UiUtils.createConfigurableBooleanProperty;
import static mb.spectrum.UiUtils.createConfigurableColorProperty;
import static mb.spectrum.UiUtils.createConfigurableDoubleProperty;
import static mb.spectrum.UiUtils.createConfigurableIntegerProperty;
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
import mb.spectrum.prop.ConfigurableIntegerProperty;
import mb.spectrum.prop.ConfigurableProperty;

public class StereoLevelsView extends AbstractView {
	
	private static final int INIT_MIN_DB_VALUE = -66;
	private static final int DB_LINES_COUNT = 8;
	private static final double BAR_HEIGHT_PROPORTION = 0.35;
	private static final double BAR_MARGIN_PROPORTION = 0.05;
	private static final double DR_BAR_HEIGHT_PROPORTION = 0.37;
	private static final double DR_BAR_MARGIN_PROPORTION = 0.04;
	private static final double GRID_MARGIN_RATIO = 0.07;
	private static final double LINGER_STAY_FACTOR = 0.01;
	private static final double LINGER_ACCELARATION_FACTOR = 1.08;
	
	// Requiring reset
	private ConfigurableIntegerProperty propMinDbValue;
	
	// Not requiring reset
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
		maxLevelL = INIT_MIN_DB_VALUE, maxLevelR = INIT_MIN_DB_VALUE;
	
	private double lingerLevelL = INIT_MIN_DB_VALUE, lingerLevelR = INIT_MIN_DB_VALUE, 
			lingerOpValL = LINGER_STAY_FACTOR, lingerOpValR = LINGER_STAY_FACTOR;
	
	private DoubleProperty currLevelLProp, currLevelRProp, minLevelLProp, 
		maxLevelLProp, minLevelRProp, maxLevelRProp, leftLingerLevelProp, rightLingerLevelProp;
	
	@Override
	public String getName() {
		return "Stereo Peak/RMS Meters";
	}
	
	@Override
	protected void initProperties() {
		
		final String keyPrefix = "stereoLevelsView.";
		
		/* Configuration Properties */
		
		// Requiring reset
		propMinDbValue = createConfigurableIntegerProperty(
				keyPrefix + "minDbValue", "Min. DB Value", -100, -10, INIT_MIN_DB_VALUE, 1);
		propMinDbValue.getProp().addListener((obs, oldVal, newVal) -> {
			if(newVal != oldVal) {
				reset();
			}
		});
		
		// Not requiring reset
		propGridColor = createConfigurableColorProperty(
				keyPrefix + "gridColor", "Grid Color", Color.web("#fd4a11"));
		propBarColorNormal = createConfigurableColorProperty(
				keyPrefix + "normalLevelColor", "Normal Level Color", Color.DARKGREEN);
		propBarColorMid = createConfigurableColorProperty(
				keyPrefix + "middleLevelColor", "Middle Level Color", Color.LAWNGREEN);
		propBarColorClip = createConfigurableColorProperty(
				keyPrefix + "clipLevelColor", "Clip Level Color", Color.RED);
		propLingerIndicatorColor = createConfigurableColorProperty(
				keyPrefix + "lingerIndicatorColor","Linger Level Color", Color.LIGHTGREEN);
		propBarOpacity = createConfigurableDoubleProperty(
				keyPrefix + "barOpacity", "Bar Opacity", 0.1, 1.0, 0.9, 0.1);
		propDrBarColor = createConfigurableColorProperty(
				keyPrefix + "drBarColor", "D/R Bar Color", Color.ORANGE);
		propDrBarOpacity = createConfigurableDoubleProperty(
				keyPrefix + "drBarOpacity", "D/R Bar Opacity", 0.1, 1.0, 0.2, 0.1);
		propShowDr = createConfigurableBooleanProperty(
				keyPrefix + "showDynamicRange", "Show Dynamic Range", true);
		propRms = createConfigurableBooleanProperty(
				keyPrefix + "enableRmsMode", "RMS Mode", false);
		
		/* Operational properties */
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
				propDrBarColor, propDrBarOpacity, propShowDr, propMinDbValue);
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
		
		double dBVal = map(idx, 0, DB_LINES_COUNT, propMinDbValue.getProp().get(), 0);
		
		// Create line
		Line line = new Line();
		line.startXProperty().bind(
				Bindings.createDoubleBinding(() -> {
					double parentWidth = getRoot().widthProperty().get();
					return map(dBVal, propMinDbValue.getProp().get(), 0, 
						parentWidth * SCENE_MARGIN_RATIO, 
						parentWidth - parentWidth * SCENE_MARGIN_RATIO);
					}, 
					getRoot().widthProperty(), propMinDbValue.getProp()));
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
					return map(lingerLevelProp.get(), propMinDbValue.getProp().get(), 0, 
							parentWidth * SCENE_MARGIN_RATIO, parentWidth - parentWidth * SCENE_MARGIN_RATIO);
				}, 
				lingerLevelProp, getRoot().widthProperty(), propMinDbValue.getProp()));
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
		int minDbValue = propMinDbValue.getProp().get();
		lingerLevelL = lingerLevelL - lingerOpValL;
		lingerOpValL = lingerOpValL * LINGER_ACCELARATION_FACTOR;
		
		if(currentDbL > lingerLevelL) {
			lingerLevelL = currentDbL;
			lingerOpValL = LINGER_STAY_FACTOR;
		}
		if(lingerLevelL < minDbValue) {
			lingerLevelL = minDbValue;
		}
		leftLingerLevelProp.set(lingerLevelL);
		
		lingerLevelR = lingerLevelR - lingerOpValR;
		lingerOpValR = lingerOpValR * LINGER_ACCELARATION_FACTOR;
		
		if(currentDbR > lingerLevelR) {
			lingerLevelR = currentDbR;
			lingerOpValR = LINGER_STAY_FACTOR;
		}
		if(lingerLevelR < minDbValue) {
			lingerLevelR = minDbValue;
		}
		rightLingerLevelProp.set(lingerLevelR);
	}
	
	/* Binding Logic */

	private void createLevelBarExprBinding(Rectangle bar, DoubleProperty prop) {
		
		bar.widthProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					return map(prop.get(), propMinDbValue.getProp().get(), 0, 
							0, parentWidth - parentWidth * SCENE_MARGIN_RATIO * 2);
				}, 
				prop, getRoot().widthProperty(), propMinDbValue.getProp()));
	}
	
	private void createDrBarExprBinding(Rectangle bar, 
			DoubleProperty minLevelProp, DoubleProperty maxLevelProp) {
		
		bar.xProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					return map(minLevelProp.get(), propMinDbValue.getProp().get(), 0,
							parentWidth * SCENE_MARGIN_RATIO, parentWidth - parentWidth * SCENE_MARGIN_RATIO);
				}, 
				minLevelProp, getRoot().widthProperty(), propMinDbValue.getProp()));
		
		bar.widthProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					return map(maxLevelProp.get(), propMinDbValue.getProp().get(), 0,
							parentWidth * SCENE_MARGIN_RATIO, parentWidth - parentWidth * SCENE_MARGIN_RATIO) - bar.xProperty().get();
				}, 
				maxLevelProp, getRoot().widthProperty(), bar.xProperty(), propMinDbValue.getProp()));
	}

}
