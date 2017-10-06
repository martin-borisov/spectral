package mb.spectrum;

import ddf.minim.AudioInput;
import ddf.minim.AudioListener;
import ddf.minim.Minim;
import ddf.minim.javasound.JSMinim;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import mb.spectrum.view.SpectrumAreaGridView;
import mb.spectrum.view.SpectrumBarGridView;
import mb.spectrum.view.StereoLevelsView;
import mb.spectrum.view.View;

public class Spectrum extends Application {
	
	private static final int SAMPLING_RATE = 44100;
	private static final int BUFFER_SIZE = 2048;
	
	private Stage stage;
	private Minim minim;
	private AudioInput in;
	private View[] views = new View[] {
			new StereoLevelsView(),
			new SpectrumAreaGridView(SAMPLING_RATE, BUFFER_SIZE),
			new SpectrumBarGridView(SAMPLING_RATE, BUFFER_SIZE)
			};
	private View currentView;
	private int currentViewIdx;
	
	
	public Spectrum() {
		currentViewIdx = 0;
		currentView = views[currentViewIdx];
		minim = new Minim(new JSMinim(new MinimInitializer()));
	}

	@Override
	public void start(Stage stage) throws Exception {
		this.stage = stage;
		startAudio();
        setupStage(stage);
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
	
	private void setupStage(Stage stage) {
        stage.setScene(currentView.getScene());
        //stage.setMaximized(true);
        stage.show();
        
        // Event handlers
        stage.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			public void handle(KeyEvent event) {
				onKey(event);
			}
		});
	}
	
	private void startFrameListener() {
		new AnimationTimer() {
			public void handle(long now) {
				currentView.nextFrame();
			}
		}.start();
	}
	
	private void onKey(KeyEvent event) {
		if(KeyCode.RIGHT == event.getCode()) {
			nextScene();
		}
		if(KeyCode.LEFT == event.getCode()) {
			prevScene();
		}
	}
	
	private void nextScene() {
		currentViewIdx += 1;
		if(currentViewIdx > views.length - 1) {
			currentViewIdx = views.length - 1;
		}
		switchScene(currentViewIdx);
	}
	
	private void prevScene() {
		currentViewIdx -= 1;
		if(currentViewIdx < 0) {
			currentViewIdx = 0;
		}
		switchScene(currentViewIdx);
	}
	
	private void switchScene(int idx) {
		currentView = views[currentViewIdx];
		stage.setScene(currentView.getScene());
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
