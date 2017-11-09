package mb.spectrum.prop;

public class ConfigurableBooleanProperty extends ConfigurableProperty<Boolean> {

	public ConfigurableBooleanProperty(String name, Boolean minValue, Boolean maxValue, Boolean initValue,
			Boolean increment) {
		super(name, minValue, maxValue, initValue, increment);
	}

	@Override
	public Boolean increment() {
		prop.set(!prop.get());
		return prop.get();
	}

	@Override
	public Boolean decrement() {
		prop.set(!prop.get());
		return prop.get();
	}

}
