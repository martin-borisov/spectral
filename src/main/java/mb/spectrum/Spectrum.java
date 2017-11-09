package mb.spectrum;

import java.util.List;

import ddf.minim.AudioInput;
import ddf.minim.AudioListener;
import ddf.minim.Minim;
import ddf.minim.javasound.JSMinim;
import javafx.animation.AnimationTimer;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import mb.spectrum.prop.ConfigurableProperty;
import mb.spectrum.view.AnalogMeterView;
import mb.spectrum.view.SpectrumAreaView;
import mb.spectrum.view.SpectrumBarView;
import mb.spectrum.view.StereoLevelsView;
import mb.spectrum.view.View;

public class Spectrum extends Application {
	
	private static final int SAMPLING_RATE = 44100;
	private static final int BUFFER_SIZE = 2048;
	
	private static final int INIT_SCENE_WIDTH = 800;
	private static final int INIT_SCENE_HEIGHT = 600;
	private static final String VIEW_LABEL_COLOR = "#00FFFF";
	private static final double VIEW_LABEL_FADE_IN_MS = 1000;
	private static final double VIEW_LABEL_LINGER_MS = 1000;
	private static final double VIEW_LABEL_FADE_OUT_MS = 1000;
	
	private Scene scene;
	private Minim minim;
	private AudioInput in;
	private View[] views = new View[] {
			new AnalogMeterView(),
			new SpectrumBarView(),
			new SpectrumAreaView(),
			new StereoLevelsView(),
			};
	private View currentView;
	private int currentViewIdx;
	
	/* Property management */
	private boolean propertiesVisible;
	private int currentPropIdx;
	private TitledPane currentPropertyNode;
	
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
		
		switch (event.getCode()) {
		case RIGHT:
			if(propertiesVisible) {
				nextProperty();
			} else {
				nextView();
			}
			break;
			
		case LEFT:
			if(propertiesVisible) {
				prevProperty();
			} else {
				prevView();
			}
			break;
		
		case SPACE:
			togglePropertiesOn();
			break;
			
		case ESCAPE:
			togglePropertiesOff();
			break;
			
		case UP:
			changeCurrentPropertyValue(true);
			break;
			
		case DOWN:
			changeCurrentPropertyValue(false);
			break;
			
		default:
			break;
		}
	}
	
	/**
	 * Resets the property index and shows first property of current view
	 */
	private void togglePropertiesOn() {
		if(!propertiesVisible) {
			currentPropIdx = 0;
			propertiesVisible = showProperty(currentPropIdx);
		}
	}
	
	/**
	 * If properties are currently shown, hides the current property
	 */
	private void togglePropertiesOff() {
		if(propertiesVisible) {
			hideProperty(currentPropertyNode);
			propertiesVisible = false;
		}
	}
	
	/**
	 * Switch to the next view from the list.
	 */
	private void nextView() {
		int idx = currentViewIdx + 1;
		if(idx > views.length - 1) {
			idx = views.length - 1;
		}
		switchView(idx);
	}
	
	/**
	 * Switch to the previous view from the list.
	 */
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
			
			// Reset properties
			togglePropertiesOff();
			
			// Trigger "hide" of current view
			currentView.onHide();
			
			currentView = views[currentViewIdx];
			scene.setRoot(currentView.getRoot());
			
			// Trigger "show" of new view
			currentView.onShow();
			
			// Show view label with animation
			Pane parent = currentView.getRoot();
			Label name = new Label(currentView.getName());
			name.styleProperty().bind(Bindings.concat(
					"-fx-font-size: ", parent.widthProperty().divide(20), ";", 
					"-fx-text-fill: ", VIEW_LABEL_COLOR, ";"));
			name.layoutXProperty().bind(parent.widthProperty().subtract(name.widthProperty()).divide(2));
			name.layoutYProperty().bind(parent.heightProperty().subtract(name.heightProperty()).divide(2));
			parent.getChildren().add(name);
			
			Transition trans = UiUtils.createFadeInOutTransition(
					name, VIEW_LABEL_FADE_IN_MS, VIEW_LABEL_LINGER_MS, VIEW_LABEL_FADE_OUT_MS, 
					new EventHandler<ActionEvent>() {
				public void handle(ActionEvent event) {
					parent.getChildren().remove(name);
				}
			});
			trans.play();
		}
	}
	
	private void nextProperty() {
		if(currentPropertyNode != null) {
			hideProperty(currentPropertyNode);
			currentPropertyNode = null;
		}
		
		currentPropIdx++;
		if(currentPropIdx > currentView.getProperties().size() - 1) {
			currentPropIdx = currentView.getProperties().size() - 1;
		}
		showProperty(currentPropIdx);
	}
	
	private void prevProperty() {
		if(currentPropertyNode != null) {
			hideProperty(currentPropertyNode);
			currentPropertyNode = null;
		}
		
		currentPropIdx--;
		if(currentPropIdx < 0) {
			currentPropIdx = 0;
		}
		showProperty(currentPropIdx);
	}
	
	@SuppressWarnings("unchecked")
	private boolean showProperty(int idx) {

		// TODO Invert the binding or utilize double binding for components that cannot be yet controlled with just keys
		List<ConfigurableProperty<? extends Object>> props = currentView.getProperties();
		if(!props.isEmpty()) {
			ObjectProperty<? extends Object> prop = props.get(idx).getProp();
			Control control = null;
			if(prop.getValue() instanceof Color) {
				ObjectProperty<Color> p = (ObjectProperty<Color>) prop;
				ColorPicker picker = UiUtils.createColorPropertyColorPicker(
						p.getValue(), currentView.getRoot());
				p.bind(picker.valueProperty());
				control = picker;
				
			} else if(prop.getValue() instanceof Double) {
				ObjectProperty<Double> p = (ObjectProperty<Double>) prop;
				Label label = UiUtils.createNumberPropertyLabel(
						String.valueOf(p.getValue()), currentView.getRoot());
				label.textProperty().addListener((obs, oldVal, newVal) -> {
					p.set(Double.valueOf(newVal));
				});
				control = label;
				
			} else if(prop.getValue() instanceof Integer) {
				ObjectProperty<Integer> p = (ObjectProperty<Integer>) prop;
				Label label = UiUtils.createNumberPropertyLabel(
						String.valueOf(p.getValue()), currentView.getRoot());
				label.textProperty().addListener((obs, oldVal, newVal) -> {
					p.set(Integer.valueOf(newVal));
				});
				control = label;
				
			} else if(prop.getValue() instanceof Boolean) {
				ObjectProperty<Boolean> p = (ObjectProperty<Boolean>) prop;
				CheckBox box = UiUtils.createBooleanPropertyCheckBox(
						p.getValue(), prop.getName(), currentView.getRoot());
				p.bind(box.selectedProperty());
				control = box;
				
			}
			
			currentView.getRoot().getChildren().add(
					currentPropertyNode = createPropertyPane(prop.getName(), control));
			UiUtils.createFadeInTransition(currentPropertyNode, 1000, null).play();
		}
		return !props.isEmpty();
	}
	
	private void hideProperty(TitledPane node) {
		currentView.getProperties().get(currentPropIdx).getProp().unbind();
		UiUtils.createFadeOutTransition(node, 1000, new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				currentView.getRoot().getChildren().remove(node);
				node.setContent(null);
			}
		}).play();
	}
	
	private TitledPane createPropertyPane(String name, Control control) {
		Pane parent = currentView.getRoot();

		TitledPane pane = new TitledPane(null, control);
		
		// Automatically resize pane based on the scene size
		pane.prefWidthProperty().bind(parent.widthProperty().divide(2));
		pane.prefHeightProperty().bind(parent.heightProperty().divide(2));
		pane.layoutXProperty().bind(parent.widthProperty().subtract(pane.widthProperty()).divide(2));
		pane.layoutYProperty().bind(parent.heightProperty().subtract(pane.heightProperty()).divide(2));
		
		// TODO This is just a sample. Remove later.
		//pane.setStyle("-fx-color: -fx-focus-color;");
		
		pane.setCollapsible(false);
		
		// TODO This does not work. Revisit.
		pane.setBackground(new Background(new BackgroundFill(Color.BLUE, new CornerRadii(5), null)));
		pane.setOpacity(0.8);
		
		Label label = new Label(name);
		label.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", parent.widthProperty().divide(40), ";", 
				"-fx-padding: ", parent.widthProperty().divide(50), ";"));
		pane.setGraphic(label);
		
		// Automatically resize the contained property control based on the pane size
		control.prefWidthProperty().bind(pane.widthProperty().divide(2));
		control.prefHeightProperty().bind(pane.heightProperty().divide(4));
		
		return pane;
	}
	
	
	// TODO The logic for changing the property values must be handled within the property classes, not here.
	// When this is done, take care of proper unbinding when property controls are hidden/removed - hideProperty()
	private void changeCurrentPropertyValue(boolean increment) {
		if(propertiesVisible) {
			ConfigurableProperty<? extends Object> prop = 
					currentView.getProperties().get(currentPropIdx);
			Object inc = prop.getIncrement();
			if(inc instanceof Integer) {
				
				Label label = (Label) currentPropertyNode.getContent();
				String text = label.getText();
				if(increment) {
					int val = Integer.valueOf(text) + (Integer) inc;
					label.setText(String.valueOf(val));
				} else {
					int val = Integer.valueOf(text) - (Integer) inc;
					label.setText(String.valueOf(val));
				}
				
			} else if(inc instanceof Double) {
				
				Label label = (Label) currentPropertyNode.getContent();
				String text = label.getText();
				if(increment) {
					double val = Double.valueOf(text) + (Double) inc;
					label.setText(String.valueOf(val));
				} else {
					double val = Double.valueOf(text) - (Double) inc;
					label.setText(String.valueOf(val));
				}
			}
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
