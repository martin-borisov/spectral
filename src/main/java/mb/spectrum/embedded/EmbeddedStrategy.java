package mb.spectrum.embedded;

import javafx.stage.Stage;
import mb.spectrum.desktop.DesktopStrategy;
import mb.spectrum.gpio.StageGpioController;

public class EmbeddedStrategy extends DesktopStrategy {
	
	private StageGpioController gpio;

	@Override
	public void initialize(Stage stage) {
		super.initialize(stage);
		gpio = new StageGpioController(stage);
	}

	@Override
	public void close() {
		super.close();
		if(gpio != null) {
			gpio.close();
		}
	}
}
