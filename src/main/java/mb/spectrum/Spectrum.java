package mb.spectrum;

import ddf.minim.AudioInput;
import ddf.minim.AudioListener;
import ddf.minim.Minim;
import ddf.minim.javasound.JSMinim;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.stage.Stage;
import mb.spectrum.view.SpectrumAreaGridView;
import mb.spectrum.view.View;

public class Spectrum extends Application {
	
	private static final int SAMPLING_RATE = 44100;
	private static final int BUFFER_SIZE = 2048;
	
	private Minim minim;
	private AudioInput in;
	private View[] views = new View[] {
			new SpectrumAreaGridView(SAMPLING_RATE, BUFFER_SIZE),
			//new SpectrumBarGridView(SAMPLING_RATE, BUFFER_SIZE),
			//new StereoLevelsView()
			};
	private View currentView;
	
	
	public Spectrum() {
		minim = new Minim(new JSMinim(new MinimInitializer()));
		currentView = views[0];
	}

	@Override
	public void start(Stage stage) throws Exception {
		startAudio();
        setupScene(stage);
		startFrameListener();
	}
	
	@Override
	public void stop() throws Exception {
		stopAudio();
	}

	private void startAudio() {
		in = minim.getLineIn(Minim.STEREO, BUFFER_SIZE, SAMPLING_RATE, 16);
		if(in == null) {
			throw new RuntimeException("Audio format not supported");
		}
		
		in.addListener(new AudioListener() {
			public void samples(float[] left, float[] right) {
				currentView.dataAvailable(left, right);
			}
			public void samples(float[] paramArrayOfFloat) {
			}
		});
	}
	
	private void stopAudio() {
		if(in != null) {
			in.close();
		}
		if(minim != null) {
			minim.stop();
		}
	}
	
	private void setupScene(Stage stage) {
        stage.setScene(currentView.getScene());
        stage.setMaximized(true);
        stage.show();
	}
	
	private void startFrameListener() {
		new AnimationTimer() {
			public void handle(long now) {
				currentView.nextFrame();
			}
		}.start();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
