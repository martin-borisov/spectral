package mb.spectrum;

import static java.text.MessageFormat.format;

import java.io.FileInputStream;
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
		synchronized (ConfigService.class) {
			if (properties == null) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream("config.properties");
					properties = new Properties();
					properties.load(fis);
				} catch (Exception e) {
					throw new RuntimeException("Failed to load properties file", e);
				} finally {
					if(fis != null) {
						try {
							fis.close();
						} catch (IOException e) {
						}
					}
				}
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