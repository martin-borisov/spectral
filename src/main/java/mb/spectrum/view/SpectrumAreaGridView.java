package mb.spectrum.view;

import static mb.spectrum.Utils.map;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

/**
 * TODO: Check if the DB lines show real values
 */
public class SpectrumAreaGridView extends AbstractSpectrumView {
	
	private SimpleObjectProperty<Color> propAreaColor;
	private SimpleObjectProperty<Color> propAreaStrokeColor;
	private SimpleObjectProperty<Color> propTrailColor;
	
	private Path curvePath, trailPath;

	@Override
	public String getName() {
		return "Spectrum Analizer - Area";
	}
	
	@Override
	protected void initProperties() {
		super.initProperties();
		propAreaColor = new SimpleObjectProperty<>(null, "Spectrum Area Color", Color.web("#7CFC00", 0.5));
		propAreaStrokeColor = new SimpleObjectProperty<>(null, "Spectrum Area Stroke Color", Color.LAWNGREEN);
		propTrailColor = new SimpleObjectProperty<>(null, "Spectrum Trail Color", Color.DARKGREEN);
	}
	
	@Override
	public List<ObjectProperty<? extends Object>> getProperties() {
		List<ObjectProperty<? extends Object>> props = 
				new ArrayList<>(super.getProperties());
		props.add(propAreaColor);
		props.add(propAreaStrokeColor);
		props.add(propTrailColor);
		return props;
	}

	@Override
	protected List<Node> collectNodes() {
		List<Node> parentShapes = super.collectNodes();
		
		// Area
		curvePath = new Path();
		curvePath.strokeProperty().bind(propAreaStrokeColor);
		curvePath.fillProperty().bind(propAreaColor);
		createStartingPoint(curvePath);
		
		// Trail
		trailPath = new Path();
		trailPath.strokeProperty().bind(propTrailColor);
		trailPath.setStrokeWidth(2);
		createStartingPoint(trailPath);
		
		// Paths
		for (int i = 0; i < bandCount; i++) {
			
			// Create line segments
			createFreqLineToSegment(i);
			createTrailLineToSegment(i);
		}
		createLastLine(curvePath);
		createLastLine(trailPath);
		
		ArrayList<Node> shapes = new ArrayList<>(parentShapes);
		shapes.add(curvePath);
		shapes.add(trailPath);
		return shapes;
	}
	
	private void createStartingPoint(Path path) {
		MoveTo moveTo = new MoveTo();
		moveTo.xProperty().bind(getRoot().widthProperty().multiply(SCENE_MARGIN_RATIO));
		moveTo.yProperty().bind(getRoot().heightProperty().subtract(
				getRoot().heightProperty().multiply(SCENE_MARGIN_RATIO)));
		path.getElements().add(moveTo);
	}
	
	private void createLastLine(Path path) {
		LineTo lineTo = new LineTo();
		lineTo.xProperty().bind(getRoot().widthProperty().subtract(
				getRoot().widthProperty().multiply(SCENE_MARGIN_RATIO)));
		lineTo.yProperty().bind(getRoot().heightProperty().subtract(
				getRoot().heightProperty().multiply(SCENE_MARGIN_RATIO)));
		path.getElements().add(lineTo);
	}
	
	private void createTrailLineToSegment(int idx) {
		LineTo lineTo = new LineTo();
		lineTo.xProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					double barW = (parentWidth - parentWidth * SCENE_MARGIN_RATIO * 2) / bandCount;
					return parentWidth * SCENE_MARGIN_RATIO + idx * barW + barW / 2;
				}, 
				getRoot().widthProperty()));
		
		lineTo.yProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentHeight = getRoot().heightProperty().get();
					return map(trailValues.get(idx).get(), MIN_DB_VALUE, 0, 
							parentHeight - parentHeight * SCENE_MARGIN_RATIO, 0);
				}, 
				getRoot().heightProperty(), trailValues.get(idx))
				);
		
		trailPath.getElements().add(lineTo);
	}
	
	private void createFreqLineToSegment(int idx) {
		LineTo lineTo = new LineTo();
		lineTo.xProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					double barW = (parentWidth - parentWidth * SCENE_MARGIN_RATIO * 2) / bandCount;
					return parentWidth * SCENE_MARGIN_RATIO + idx * barW + barW / 2;
				}, 
				getRoot().widthProperty()));
		
		lineTo.yProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentHeight = getRoot().heightProperty().get();
					return map(bandValues.get(idx).get(), MIN_DB_VALUE, 0, 
							parentHeight - parentHeight * SCENE_MARGIN_RATIO, 0);
				}, 
				getRoot().heightProperty(), bandValues.get(idx))
				);
		
		curvePath.getElements().add(lineTo);
	}
}
