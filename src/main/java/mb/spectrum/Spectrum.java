package mb.spectrum;

import ddf.minim.AudioInput;
import ddf.minim.AudioListener;
import ddf.minim.Minim;
import ddf.minim.javasound.JSMinim;
import javafx.animation.AnimationTimer;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import mb.spectrum.view.SpectrumAreaGridView;
import mb.spectrum.view.StereoLevelsView;
import mb.spectrum.view.View;

public class Spectrum extends Application {
	
	private static final int SAMPLING_RATE = 44100;
	private static final int BUFFER_SIZE = 2048;
	
	private static final int INIT_SCENE_WIDTH = 800;
	private static final int INIT_SCENE_HEIGHT = 600;
	private static final Color VIEW_LABEL_COLOR = Color.CYAN;
	private static final int SCENE_WIDTH_TO_VIEW_LABEL_FONT_SIZE_RATIO = 20;
	private static final double VIEW_LABEL_FADE_IN_MS = 1000;
	private static final double VIEW_LABEL_LINGER_MS = 1000;
	private static final double VIEW_LABEL_FADE_OUT_MS = 1000;
	
	
	private Scene scene;
	private Minim minim;
	private AudioInput in;
	private View[] views = new View[] {
			new StereoLevelsView(),
			new SpectrumAreaGridView(SAMPLING_RATE, BUFFER_SIZE),
			//new SpectrumBarGridView(SAMPLING_RATE, BUFFER_SIZE)
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
		
        stage.setScene(scene = new Scene(currentView.getRoot(), 
        		INIT_SCENE_WIDTH, INIT_SCENE_HEIGHT, false, SceneAntialiasing.DISABLED));
        currentView.onShow();
        
        stage.setMaximized(true);
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
			nextView();
		}
		if(KeyCode.LEFT == event.getCode()) {
			prevView();
		}
	}
	
	private void nextView() {
		int idx = currentViewIdx + 1;
		if(idx > views.length - 1) {
			idx = views.length - 1;
		}
		switchView(idx);
	}
	
	private void prevView() {
		int idx = currentViewIdx - 1;
		if(idx < 0) {
			idx = 0;
		}
		switchView(idx);
	}
	
	private void switchView(int idx) {
		if(idx != currentViewIdx) {
			currentViewIdx = idx;
			
			// Trigger "hide" of current view
			currentView.onHide();
			
			currentView = views[currentViewIdx];
			scene.setRoot(currentView.getRoot());
			
			// Trigger "show" of new view
			currentView.onShow();
			
			// Show view label with animation
			Text name = new Text(currentView.getName());
			name.setFill(VIEW_LABEL_COLOR);
			name.setFont(Font.font(scene.getWidth() / SCENE_WIDTH_TO_VIEW_LABEL_FONT_SIZE_RATIO));
			name.setX(scene.getWidth() / 2 - name.getLayoutBounds().getWidth() / 2);
			name.setY(scene.getHeight() / 2 + name.getLayoutBounds().getHeight() / 2);
			currentView.getRoot().getChildren().add(name);
			
			Transition trans = UiUtils.createTextFadeInOutTransition(
					name, VIEW_LABEL_FADE_IN_MS, VIEW_LABEL_LINGER_MS, VIEW_LABEL_FADE_OUT_MS, 
					new EventHandler<ActionEvent>() {
				public void handle(ActionEvent event) {
					currentView.getRoot().getChildren().remove(name);
				}
			});
			trans.play();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
