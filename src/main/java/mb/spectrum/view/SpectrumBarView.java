package mb.spectrum.view;

import static mb.spectrum.Utils.map;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import mb.spectrum.UiUtils;

public class SpectrumBarView extends AbstractSpectrumView {
	
	private SimpleObjectProperty<Color> propBarColor;
	private SimpleObjectProperty<Color> propTrailColor;
	private SimpleObjectProperty<Double> propGapBarRatio;
	
	private List<Rectangle> bars;
	private List<Line> trails;

	@Override
	public String getName() {
		return "Spectrum Analizer - Bars";
	}
	
	@Override
	protected void initProperties() {
		super.initProperties();
		propBarColor = UiUtils.createConfigurableColorProperty(
				"spectrumBarView.barColor", "Bar Color", Color.web("#7CFC00", 0.5));
		propTrailColor = UiUtils.createConfigurableColorProperty(
				"spectrumBarView.trailColor", "Trail Color", Color.LAWNGREEN);
		propGapBarRatio = UiUtils.createConfigurableDoubleProperty(
				"spectrumBarView.gapBarRatio", "Gap/Bar Ratio", 0.01);
	}
	
	@Override
	public List<ObjectProperty<? extends Object>> getProperties() {
		List<ObjectProperty<? extends Object>> props = 
				new ArrayList<>(super.getProperties());
		props.add(propBarColor);
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
							idx * bandWidth + bandWidth * propGapBarRatio.get() / 2;
				}, getRoot().widthProperty(), propGapBarRatio));
		rect.widthProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					double bandWidth = (parentWidth - parentWidth * SCENE_MARGIN_RATIO * 2) / bandCount;
					return bandWidth - bandWidth * propGapBarRatio.get();
				}, getRoot().widthProperty(), propGapBarRatio));
		rect.yProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentHeight = getRoot().heightProperty().get();
					return map(bandValues.get(idx).get(), MIN_DB_VALUE, 0, 
							parentHeight - parentHeight * SCENE_MARGIN_RATIO, 0);
				}, getRoot().heightProperty(), bandValues.get(idx)));
		rect.heightProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentHeight = getRoot().heightProperty().get();
					return (parentHeight - parentHeight * SCENE_MARGIN_RATIO) - rect.yProperty().get();
				}, getRoot().heightProperty(), rect.yProperty()));
		rect.fillProperty().bind(propBarColor);
		bars.add(rect);
	}
	
	private void createTrail(int idx) {
		Line line = new Line();
		line.startXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					double bandWidth = (parentWidth - parentWidth * SCENE_MARGIN_RATIO * 2) / bandCount;
					return getRoot().widthProperty().get() * SCENE_MARGIN_RATIO + 
							idx * bandWidth + bandWidth * propGapBarRatio.get() / 2;
				}, getRoot().widthProperty(), propGapBarRatio));
		line.endXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					double bandWidth = (parentWidth - parentWidth * SCENE_MARGIN_RATIO * 2) / bandCount;
					return line.startXProperty().get() + bandWidth - bandWidth * propGapBarRatio.get();
				}, getRoot().widthProperty(), line.startXProperty()));
		
		line.startYProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentHeight = getRoot().heightProperty().get();
					return map(trailValues.get(idx).get(), MIN_DB_VALUE, 0, 
							parentHeight - parentHeight * SCENE_MARGIN_RATIO, 0);
				}, getRoot().heightProperty(), trailValues.get(idx)));
		line.endYProperty().bind(line.startYProperty());
		line.visibleProperty().bind(trailValues.get(idx).greaterThan(MIN_DB_VALUE));
		line.strokeProperty().bind(propTrailColor);
		line.setStrokeWidth(2);
		trails.add(line);
	}
}
