package mb.spectrum.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
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
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void initProperties() {
		final String keyPrefix = "stereoAnalogMetersView.";
		
		propBorderSizeRatio = UiUtils.createConfigurableDoubleProperty(
				keyPrefix + "borderSizeRatio", "Border Size Ratio", 0.0, 0.5, 0.01, 0.01);
		
		leftMeterView = new AnalogMeterView("Left Channel", "stereoAnalogMetersView", "Peak L.", Orientation.VERTICAL);
		rightMeterView = new AnalogMeterView("Right Channel", "stereoAnalogMetersView", "Peak R.",  Orientation.VERTICAL);
		
		// Bind the properties of the two sub views. The left set of properties is exposed for user input.
		List<ConfigurableProperty<? extends Object>> leftProps = leftMeterView.getProperties();
		List<ConfigurableProperty<? extends Object>> rightProps = rightMeterView.getProperties();
		for (int i = 0; i < rightProps.size(); i++) {
			rightProps.get(i).getProp().bind((ObjectProperty) leftProps.get(i).getProp());
		}
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		List<ConfigurableProperty<? extends Object>> props = new ArrayList<>();
		props.add(propBorderSizeRatio);
		props.addAll(leftMeterView.getProperties());
		return props;
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
		left.widthProperty().bind(pane.heightProperty());
		left.layoutYProperty().bind(pane.heightProperty().divide(2).subtract(left.heightProperty().divide(2)));
		left.heightProperty().bind(left.widthProperty().divide(1.8));
		left.layoutXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double width = pane.widthProperty().get() / 2;
					return width - left.widthProperty().get() / 2 
							- left.heightProperty().get() / 2 
							- width * propBorderSizeRatio.getProp().get() / 2;
				}, pane.widthProperty(), left.widthProperty(), left.heightProperty(), propBorderSizeRatio.getProp()));
		
		left.setRotate(90);
		left.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
		
		SubScene right = new SubScene(rightMeterView.getRoot(), 0, 0, true, SceneAntialiasing.BALANCED);
		right.widthProperty().bind(left.widthProperty());
		right.layoutYProperty().bind(left.layoutYProperty());
		right.heightProperty().bind(left.heightProperty());
		right.layoutXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double width = pane.widthProperty().get() / 2;
					return width - right.widthProperty().get() / 2 
							+ right.heightProperty().get() / 2 
							+ width * propBorderSizeRatio.getProp().get() / 2;
				}, pane.widthProperty(), right.widthProperty(), right.heightProperty(), propBorderSizeRatio.getProp()));
		right.setRotate(270);
		
		return Arrays.asList(left, right);
	}

}
