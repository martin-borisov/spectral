package mb.spectrum;

import static java.text.MessageFormat.format;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigService {

	private static ConfigService ref;
	private Properties properties;

	public static ConfigService getInstance() {
		synchronized (ConfigService.class) {
			if (ref == null) {
				ref = new ConfigService();
			}
		}
		return ref;
	}

	private ConfigService() {
	}

	public String getProperty(String key) {
		return getProperties().getProperty(key);
	}
	
	public String getOrCreateProperty(String key, String defaultValue) {
		String val = null;
		synchronized (ConfigService.class) {
			val = getProperties().getProperty(key);
			if(val == null) {
				val = defaultValue;
				getProperties().setProperty(key, val);
				storeProperties();
			}
		}
		return val;
	}
	
	public void setProperty(String key, String value) {
		if (getProperties().containsKey(key)) {
			getProperties().setProperty(key, value);
			synchronized (ConfigService.class) {
				storeProperties();
			}
		} else {
			throw new IllegalArgumentException(format("Property \"{0}\" does not exist", key));
		}
	}

	private Properties getProperties() {
		if (properties == null) {
			properties = new Properties();
			try {
				properties.load(this.getClass().getClassLoader()
						.getResourceAsStream("config.properties"));
			} catch (IOException e) {
				throw new RuntimeException("Failed to load properties file", e);
			}
		}
		return properties;
	}
	
	private void storeProperties() {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("config.properties");
			getProperties().store(fos, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}
	}
}