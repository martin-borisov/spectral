package mb.spectrum.view;

import javafx.scene.layout.Pane;

public interface View {
	String getName();
	Pane getRoot();
	void dataAvailable(float[] left, float[] right);
	void nextFrame();
	void onShow();
	void onHide();
}
