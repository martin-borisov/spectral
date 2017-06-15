package mb.spectrum;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MinimInitializer {

	public String sketchPath(String fileName) {
		return "";
	}

	public InputStream createInput(String fileName) {
		try {
			return new FileInputStream(new File(fileName));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
