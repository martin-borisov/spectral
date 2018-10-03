package mb.spectrum.view;

import java.util.Arrays;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import mb.spectrum.UiUtils;
import mb.spectrum.prop.ConfigurableDoubleProperty;
import mb.spectrum.prop.ConfigurableProperty;
import mb.spectrum.view.AnalogMeterView.Orientation;

public class StereoAnalogMetersView extends AbstractView {
	
	private ConfigurableDoubleProperty propBorderSizeRatio;
	
	private AnalogMeterView leftMeterView, rightMeterView;

	@Override
	public String getName() {
		return "Stereo Analog Meters";
	}
	
	@Override
	protected void initProperties() {
		final String keyPrefix = "stereoAnalogMetersView.";
		
		propBorderSizeRatio = UiUtils.createConfigurableDoubleProperty(
				keyPrefix + "borderSizeRatio", "Border Size Ratio", 0.0, 0.5, 0.01, 0.01);
		
		leftMeterView = new AnalogMeterView("Left Channel", Orientation.VERTICAL);
		rightMeterView = new AnalogMeterView("Right Channel", Orientation.HORIZONTAL);
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Arrays.asList(propBorderSizeRatio);
	}

	@Override
	public void dataAvailable(float[] left, float[] right) {
		leftMeterView.dataAvailable(left);
		rightMeterView.dataAvailable(right);
	}

	@Override
	public void nextFrame() {
		leftMeterView.nextFrame();
		rightMeterView.nextFrame();
	}

	@Override
	protected List<Node> collectNodes() {
		
		// Evenly align the two "analog" meters
		SubScene left = new SubScene(leftMeterView.getRoot(), 0, 0, true, SceneAntialiasing.BALANCED);
		
		/*
		left.widthProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double totalWidth = pane.widthProperty().get() / 2;
					return totalWidth - totalWidth * propBorderSizeRatio.getProp().get() * 1.5;
				}, pane.widthProperty(), propBorderSizeRatio.getProp()));
		left.heightProperty().bind(left.widthProperty().divide(1.8));
		left.layoutXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double totalWidth = pane.widthProperty().get() / 2;
					return totalWidth * propBorderSizeRatio.getProp().get();
				}, pane.widthProperty(), propBorderSizeRatio.getProp()));
		left.layoutYProperty().bind(pane.heightProperty().subtract(left.heightProperty()).divide(2));
		*/
		
		left.widthProperty().bind(pane.heightProperty());
		left.layoutYProperty().bind(pane.heightProperty().divide(2).subtract(left.heightProperty().divide(2)));
		
		left.heightProperty().bind(left.widthProperty().divide(1.8));
		left.layoutXProperty().bind(
				pane.widthProperty().divide(2)
					.subtract(left.widthProperty().divide(2))
					.subtract(left.heightProperty().divide(2)));
		
		left.setRotate(90);
		left.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
		
		SubScene right = new SubScene(rightMeterView.getRoot(), 0, 0, true, SceneAntialiasing.BALANCED);
		right.widthProperty().bind(left.widthProperty());
		right.layoutYProperty().bind(left.layoutYProperty());
		
		right.heightProperty().bind(left.heightProperty());
		right.layoutXProperty().bind(
				pane.widthProperty().divide(2)
					.subtract(right.widthProperty().divide(2))
					.add(right.heightProperty().divide(2)));
		right.setRotate(270);
		
		return Arrays.asList(left, right);
	}

}
