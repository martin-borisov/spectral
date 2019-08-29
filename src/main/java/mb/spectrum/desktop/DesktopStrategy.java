package mb.spectrum.desktop;

import ddf.minim.AudioPlayer;
import ddf.minim.AudioSource;
import ddf.minim.Minim;
import ddf.minim.javasound.JSMinim;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import mb.spectrum.AudioListener;
import mb.spectrum.MinimInitializer;
import mb.spectrum.PlatformStrategy;
import mb.spectrum.UiUtils;

public class DesktopStrategy implements PlatformStrategy {
	
	private Minim minim;
	private AudioSource in;
	
	public DesktopStrategy() {
		minim = new Minim(new JSMinim(new MinimInitializer()));
	}
	
	@Override
	public void initialize(Stage stage) {
	    
	    // Used only for troubleshooting purposes, not really needed functionally
	    if(Boolean.getBoolean("spectrumDebug")) {
	        stage.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
	            public void handle(KeyEvent event) {
	                if(KeyCode.D.equals(event.getCode()) && 
	                        event.isControlDown() && 
	                        event.isAltDown()) {
	                    UiUtils.createAndShowShutdownPrompt(stage, false);
	                    event.consume();
	                }
	            }
	        });
	    }
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
