package mb.spectrum.prop;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class ConfigurableChoiceProperty extends ConfigurableProperty<String> {
	
	private int idx = 0;
	private List<String> values;
	
	public ConfigurableChoiceProperty(String name, List<String> values, String initValue) {
		super(name, StringUtils.EMPTY, StringUtils.EMPTY, initValue, null);
		this.values = values;
		idx = values.indexOf(initValue);
	}

	@Override
	public String increment() {
		idx++;
		if(idx == values.size()) {
			idx = 0;
		}
		return getPropValue();
	}

	@Override
	public String decrement() {
		idx--;
		if(idx < 0) {
			idx = values.size() - 1;
		}
		return getPropValue();
	}
	
	public int getCurrentIndex() {
	    return idx;
	}
	
	public List<String> getAllValues() {
	    return values;
	}
	
	private String getPropValue() {
		prop.set(values.get(idx));
		return prop.get();
	}

}
