package mb.spectrum.prop;

import javafx.scene.paint.Color;

public class ConfigurableColorProperty extends ConfigurableProperty<Color> {

	public ConfigurableColorProperty(String name, Color minValue, Color maxValue, Color initValue, Color increment) {
		super(name, minValue, maxValue, initValue, increment);
	}

	@Override
	public Color increment() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Color decrement() {
		// TODO Auto-generated method stub
		return null;
	}

}
