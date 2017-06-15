package mb.spectrum.view;

import javafx.scene.Scene;

public interface View {
	Scene getScene();
	void dataAvailable(float[] left, float[] right);
	void nextFrame();
}
