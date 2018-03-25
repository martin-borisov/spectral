package mb.spectrum.view;

import static mb.spectrum.Utils.peakLevel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import mb.spectrum.Utils;
import mb.spectrum.prop.ConfigurableProperty;

public class CubeView extends AbstractMixedChannelView {
	
	private Box box;
	
	/* Operational Properties */
	private DoubleProperty rotateXProp, rotateYProp, rotateZProp, scaleProp;

	@Override
	public String getName() {
		return "3D Cube";
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Collections.emptyList();
	}

	@Override
	protected void initProperties() {
		super.initProperties();
		
		rotateXProp = new SimpleDoubleProperty();
		rotateYProp = new SimpleDoubleProperty();
		rotateZProp = new SimpleDoubleProperty();
		scaleProp = new SimpleDoubleProperty(100);
	}

	@Override
	public void nextFrame() {
		rotateXProp.set(rotateXProp.get() + 1);
		rotateYProp.set(rotateYProp.get() + 1);
		//rotateZProp.set(rotateZProp.get() + 1);
	}

	@Override
	public void dataAvailable(float[] data) {
		//scaleProp.set(400 - Math.abs(Utils.toDB(peakLevel(data))));
	}

	@Override
	protected List<Node> collectNodes() {
		return Arrays.asList(box = createBox());
	}
	
	private Box createBox() {
		Box box = new Box();
		box.layoutXProperty().bind(getRoot().widthProperty().divide(2));
		box.layoutYProperty().bind(getRoot().heightProperty().divide(2));
		box.widthProperty().bind(scaleProp);
		box.heightProperty().bind(scaleProp);
		box.depthProperty().bind(scaleProp);
		box.setMaterial(new PhongMaterial(Color.LIMEGREEN));
		
		Rotate rotateX = new Rotate();
		rotateX.setAxis(Rotate.X_AXIS);
		rotateX.angleProperty().bind(rotateXProp);
		
		Rotate rotateY = new Rotate();
		rotateY.setAxis(Rotate.Y_AXIS);
		rotateY.angleProperty().bind(rotateYProp);
		
		Rotate rotateZ = new Rotate();
		rotateZ.setAxis(Rotate.Z_AXIS);
		rotateZ.angleProperty().bind(rotateZProp);
		
		box.getTransforms().addAll(rotateX, rotateY, rotateZ);
		
		// This doesn't seem to work
		//new ParallelTransition(box, createRotateTransition(box, Rotate.Y_AXIS), createRotateTransition(box, Rotate.X_AXIS)).play();
		
		return box;
	}
	
	private RotateTransition createRotateTransition(Node node, Point3D axis) {
		RotateTransition rotate = new RotateTransition(Duration.seconds(10), node);
		rotate.setAxis(axis);
		rotate.setFromAngle(360);
		rotate.setToAngle(0);
		rotate.setInterpolator(Interpolator.LINEAR);
		rotate.setCycleCount(RotateTransition.INDEFINITE);
		return rotate;
	}
	
	private RotateTransition createRotateTransition(Point3D axis) {
		RotateTransition rotate = new RotateTransition(Duration.seconds(30));
		rotate.setAxis(axis);
		rotate.setFromAngle(360);
		rotate.setToAngle(0);
		rotate.setInterpolator(Interpolator.LINEAR);
		rotate.setCycleCount(RotateTransition.INDEFINITE);
		return rotate;
	}

}
