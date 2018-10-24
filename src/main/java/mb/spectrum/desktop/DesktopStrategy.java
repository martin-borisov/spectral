package mb.spectrum.desktop;

import ddf.minim.AudioPlayer;
import ddf.minim.AudioSource;
import ddf.minim.Minim;
import ddf.minim.javasound.JSMinim;
import javafx.stage.Stage;
import mb.spectrum.AudioListener;
import mb.spectrum.MinimInitializer;
import mb.spectrum.PlatformStrategy;

public class DesktopStrategy implements PlatformStrategy {
	
	private Minim minim;
	private AudioSource in;
	
	public DesktopStrategy() {
		minim = new Minim(new JSMinim(new MinimInitializer()));
	}
	
	@Override
	public void initialize(Stage stage) {
	}

	@Override
	public void startAudio(boolean stereo, int bufferSize, int samplingRate, int bitRate) {
		in = minim.getLineIn(stereo ? Minim.STEREO : Minim.MONO, bufferSize, samplingRate, bitRate);
		if(in == null) {
			throw new RuntimeException("Audio format not supported");
		}
	}
	
	@Override
	public void startAudio(String audioFilePath, int bufferSize) {
		in = minim.loadFile(audioFilePath, bufferSize);
		((AudioPlayer) in).loop();
	}

	@Override
	public void setListener(AudioListener listener) {
		in.addListener(new ddf.minim.AudioListener() {
			public void samples(float[] left, float[] right) {
				listener.samples(left, right);
			}
			public void samples(float[] paramArrayOfFloat) {
			}
		});
	}

	@Override
	public void stopAudio() {
		if(in != null) {
			in.close();
		}
		if(minim != null) {
			minim.stop();
		}
	}

	@Override
	public void close() {
	}
}
