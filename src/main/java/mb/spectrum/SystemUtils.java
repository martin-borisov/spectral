package mb.spectrum;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SystemUtils {
	
	public static void shutdown() {
		try {
			ProcessBuilder builder = new ProcessBuilder("shutdown", "now");
			builder.redirectErrorStream(true);
			Process p = builder.start();
			
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(p.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
			    System.out.println(line);
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
