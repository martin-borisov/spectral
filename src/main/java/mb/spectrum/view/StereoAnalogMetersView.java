package mb.spectrum.view;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import mb.spectrum.prop.ConfigurableProperty;

public class StereoAnalogMetersView extends AbstractView {
	
	private AnalogMeterView leftMeterView, rightMeterView;

	@Override
	public String getName() {
		return "Stereo Analog Meters";
	}
	
	@Override
	protected void initProperties() {
		leftMeterView = new AnalogMeterView();
		rightMeterView = new AnalogMeterView();
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Collections.emptyList();
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
		
		SubScene left = new SubScene(leftMeterView.getRoot(), 
				pane.getWidth() / 2, pane.getHeight(), true, SceneAntialiasing.BALANCED);
		left.widthProperty().bind(pane.widthProperty().divide(2));
		left.heightProperty().bind(left.widthProperty().divide(1.8));
		left.layoutYProperty().bind(pane.heightProperty().subtract(left.heightProperty()).divide(2));
		
		
		SubScene right = new SubScene(rightMeterView.getRoot(), 
				pane.getWidth() / 2, pane.getHeight(), true, SceneAntialiasing.BALANCED);
		right.widthProperty().bind(pane.widthProperty().divide(2));
		right.heightProperty().bind(right.widthProperty().divide(1.8));
		right.layoutXProperty().bind(pane.widthProperty().divide(2));
		right.layoutYProperty().bind(pane.heightProperty().subtract(right.heightProperty()).divide(2));
		
		return Arrays.asList(left, right);
	}

}
