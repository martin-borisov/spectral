package mb.spectrum.view;

import static mb.spectrum.Utils.map;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import mb.spectrum.UiUtils;
import mb.spectrum.prop.ConfigurableProperty;

public class SpectrumBarView extends AbstractSpectrumView {
	
	private ConfigurableProperty<Color> propBarColor1;
	private ConfigurableProperty<Color> propBarColor2;
	private ConfigurableProperty<Color> propTrailColor;
	private ConfigurableProperty<Double> propGapBarRatio;
	
	private List<Rectangle> bars;
	private List<Line> trails;

	@Override
	public String getName() {
		return "Spectrum Analizer - Bars";
	}
	
	@Override
	protected String getBasePropertyKey() {
		return "spectrumBarView";
	}
	
	@Override
	protected void initProperties() {
		super.initProperties();
		propBarColor1 = UiUtils.createConfigurableColorProperty(
				getBasePropertyKey() + ".barColor1", "Bar Color 1", Color.web("#7CFC00", 0.75));
		propBarColor2 = UiUtils.createConfigurableColorProperty(
				getBasePropertyKey() + ".barColor2", "Bar Color 2", Color.web("#7CFC00", 0.25));
		propTrailColor = UiUtils.createConfigurableColorProperty(
				getBasePropertyKey() + ".trailColor", "Trail Color", Color.LAWNGREEN);
		propGapBarRatio = UiUtils.createConfigurableDoubleProperty(
				getBasePropertyKey() + ".gapBarRatio", "Gap/Bar Ratio", 0.01, 0.9, 0.01, 0.01);
	}
	
	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		List<ConfigurableProperty<? extends Object>> props = 
				new ArrayList<>(super.getProperties());
		props.add(propBarColor1);
		props.add(propBarColor2);
		props.add(propTrailColor);
		props.add(propGapBarRatio);
		return props;
	}

	@Override
	protected List<Node> collectNodes() {
		List<Node> parentShapes = super.collectNodes();
		
		bars = new ArrayList<>();
		trails = new ArrayList<>();
		for (int i = 0; i < bandCount; i++) {
			createBar(i);
			createTrail(i);
		}
		
		ArrayList<Node> shapes = new ArrayList<>(parentShapes);
		shapes.addAll(bars);
		shapes.addAll(trails);
		return shapes;
	}
	
	private void createBar(int idx) {
		Rectangle rect = new Rectangle();
		rect.xProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					double bandWidth = (parentWidth - parentWidth * SCENE_MARGIN_RATIO * 2) / bandCount;
					return getRoot().widthProperty().get() * SCENE_MARGIN_RATIO + 
							idx * bandWidth + bandWidth * propGapBarRatio.getProp().get() / 2;
				}, getRoot().widthProperty(), propGapBarRatio.getProp()));
		rect.widthProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					double bandWidth = (parentWidth - parentWidth * SCENE_MARGIN_RATIO * 2) / bandCount;
					return bandWidth - bandWidth * propGapBarRatio.getProp().get();
				}, getRoot().widthProperty(), propGapBarRatio.getProp()));
		rect.yProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentHeight = getRoot().heightProperty().get();
					return map(bandValues.get(idx).get(), propMinDbValue.getProp().get(), 0, 
							parentHeight - parentHeight * SCENE_MARGIN_RATIO, 0);
				}, getRoot().heightProperty(), propMinDbValue.getProp(), bandValues.get(idx)));
		rect.heightProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentHeight = getRoot().heightProperty().get();
					return (parentHeight - parentHeight * SCENE_MARGIN_RATIO) - rect.yProperty().get();
				}, getRoot().heightProperty(), rect.yProperty()));
		
		ReadOnlyDoubleProperty rootHeightProp = getRoot().heightProperty();
		rect.styleProperty().bind(
				Bindings.concat(
						"-fx-fill: ", 
							"linear-gradient(from ", rect.xProperty(), "px ", rootHeightProp.multiply(SCENE_MARGIN_RATIO), "px to ", 
								rect.xProperty(), "px ", rootHeightProp.subtract(rootHeightProp.multiply(SCENE_MARGIN_RATIO)), ",", 
								Bindings.createStringBinding(() -> (UiUtils.colorToWeb(propBarColor2.getProp().get())), propBarColor2.getProp()), " 50%, ", 
								Bindings.createStringBinding(() -> (UiUtils.colorToWeb(propBarColor1.getProp().get())), propBarColor1.getProp()), " 100%)"
					));
		
		
		bars.add(rect);
	}
	
	private void createTrail(int idx) {
		Line line = new Line();
		line.startXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					double bandWidth = (parentWidth - parentWidth * SCENE_MARGIN_RATIO * 2) / bandCount;
					return getRoot().widthProperty().get() * SCENE_MARGIN_RATIO + 
							idx * bandWidth + bandWidth * propGapBarRatio.getProp().get() / 2;
				}, getRoot().widthProperty(), propGapBarRatio.getProp()));
		line.endXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					double bandWidth = (parentWidth - parentWidth * SCENE_MARGIN_RATIO * 2) / bandCount;
					return line.startXProperty().get() + bandWidth - bandWidth * propGapBarRatio.getProp().get();
				}, getRoot().widthProperty(), line.startXProperty(), propGapBarRatio.getProp()));
		
		line.startYProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentHeight = getRoot().heightProperty().get();
					return map(trailValues.get(idx).get(), propMinDbValue.getProp().get(), 0, 
							parentHeight - parentHeight * SCENE_MARGIN_RATIO, 0);
				}, getRoot().heightProperty(), propMinDbValue.getProp(), trailValues.get(idx)));
		line.endYProperty().bind(line.startYProperty());
		line.visibleProperty().bind(trailValues.get(idx).greaterThan(propMinDbValue.getProp().get()));
		line.strokeProperty().bind(propTrailColor.getProp());
		line.setStrokeWidth(2);
		trails.add(line);
	}
}
