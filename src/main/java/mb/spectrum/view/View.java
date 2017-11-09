package mb.spectrum.view;

import java.util.List;

import javafx.scene.layout.Pane;
import mb.spectrum.prop.ConfigurableProperty;

public interface View {
	String getName();
	Pane getRoot();
	List<ConfigurableProperty<? extends Object>> getProperties();
	void dataAvailable(float[] left, float[] right);
	void nextFrame();
	void onShow();
	void onHide();
}
