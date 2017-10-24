package mb.spectrum;

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
}