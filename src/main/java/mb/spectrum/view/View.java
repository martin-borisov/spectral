package mb.spectrum.view;

import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.scene.layout.Pane;

public interface View {
	String getName();
	Pane getRoot();
	List<ObjectProperty<? extends Object>> getProperties();
	void dataAvailable(float[] left, float[] right);
	void nextFrame();
	void onShow();
	void onHide();
}
