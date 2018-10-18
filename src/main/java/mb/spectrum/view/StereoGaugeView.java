package mb.spectrum.view;

import java.util.Arrays;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import mb.spectrum.prop.ConfigurableProperty;

public class StereoGaugeView extends AbstractView {
	
	private GaugeView leftView, rightView;
	
	public StereoGaugeView() {
		super(true);
		leftView = new GaugeView("Left", "stereoGaugeView", true);
		rightView = new GaugeView("Right", "stereoGaugeView", false);
		init();
	}

	@Override
	public String getName() {
		return "Simple Analog Meter - Stereo";
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void initProperties() {
		List<ConfigurableProperty<? extends Object>> rightProps = rightView.getProperties();
		List<ConfigurableProperty<? extends Object>> leftProps = leftView.getProperties();
		for (int i = 0; i < rightProps.size(); i++) {
			rightProps.get(i).getProp().bind((ObjectProperty) leftProps.get(i).getProp());
		}
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return leftView.getProperties();
	}

	@Override
	public void dataAvailable(float[] left, float[] right) {
		leftView.dataAvailable(left);
		rightView.dataAvailable(right);
	}

	@Override
	public void nextFrame() {
	}

	@Override
	protected List<Node> collectNodes() {
		Pane leftPane = leftView.getRoot();
		leftPane.prefWidthProperty().bind(getRoot().widthProperty().divide(2));
		leftPane.prefHeightProperty().bind(getRoot().heightProperty());
		leftPane.setLayoutX(0);
		leftPane.setLayoutY(0);
		
		Pane rightPane = rightView.getRoot();
		rightPane.prefWidthProperty().bind(getRoot().widthProperty().divide(2));
		rightPane.prefHeightProperty().bind(getRoot().heightProperty());
		rightPane.layoutXProperty().bind(rightPane.widthProperty());
		rightPane.setLayoutY(0);
		
		return Arrays.asList(leftPane, rightPane);
	}
}
