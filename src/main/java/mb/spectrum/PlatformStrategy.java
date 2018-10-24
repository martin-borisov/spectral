package mb.spectrum;

import javafx.stage.Stage;

public interface PlatformStrategy {
	
	void initialize(Stage stage);
	void startAudio(boolean stereo, int bufferSize, int samplingRate, int bitRate);
	void startAudio(String audioFilePath, int bufferSize);
	void setListener(AudioListener listener);
	void stopAudio();
	void close();

}
