package mb.spectrum;

import static mb.spectrum.UiUtils.createConfigurableBooleanProperty;
import static mb.spectrum.UiUtils.createConfigurableIntegerProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ddf.minim.AudioInput;
import ddf.minim.AudioListener;
import ddf.minim.Minim;
import ddf.minim.javasound.JSMinim;
import javafx.animation.AnimationTimer;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import mb.spectrum.gpio.StageGpioController;
import mb.spectrum.prop.ConfigurableBooleanProperty;
import mb.spectrum.prop.ConfigurableColorProperty;
import mb.spectrum.prop.ConfigurableDoubleProperty;
import mb.spectrum.prop.ConfigurableIntegerProperty;
import mb.spectrum.prop.ConfigurableProperty;
import mb.spectrum.view.AnalogMeterView;
import mb.spectrum.view.SpectrumAreaView;
import mb.spectrum.view.SpectrumBarView;
import mb.spectrum.view.StereoLevelsLedView;
import mb.spectrum.view.StereoLevelsView;
import mb.spectrum.view.View;

public class Spectrum extends Application {
	
	private static final int SAMPLING_RATE = Integer.valueOf(
			ConfigService.getInstance().getProperty("mb.sampling-rate"));
	private static final int BUFFER_SIZE = Integer.valueOf(
			ConfigService.getInstance().getProperty("mb.buffer-size"));
	private static final boolean ENABLE_GPIO = Boolean.valueOf(
			ConfigService.getInstance().getProperty("mb.enable-gpio"));
	
	private static final int INIT_SCENE_WIDTH = 800;
	private static final int INIT_SCENE_HEIGHT = 480;
	private static final String VIEW_LABEL_COLOR = "#00FFFF";
	private static final double VIEW_LABEL_FADE_IN_MS = 1000;
	private static final double VIEW_LABEL_LINGER_MS = 1000;
	private static final double VIEW_LABEL_FADE_OUT_MS = 1000;
	
	private StageGpioController gpio;
	private Scene scene;
	private Minim minim;
	private AudioInput in;
	private View[] views = new View[] {
			new AnalogMeterView(),
			new StereoLevelsLedView(),
			new SpectrumBarView(),
			new SpectrumAreaView(),
			new StereoLevelsView(),
			};
	private View currentView;
	private int currentViewIdx;
	private Timer viewRotateTimer;
	
	/* Property management */
	private List<ConfigurableProperty<? extends Object>> currentPropertyList;
	private int currentPropIdx;
	private BorderPane currentPropertyNode;
	
	/* Global Properties */
	List<ConfigurableProperty<? extends Object>> globalPropertyList;
	private ConfigurableIntegerProperty propGlobalGain;
	private ConfigurableBooleanProperty propViewAutoRotate;
	private ConfigurableIntegerProperty propViewAutoRotateInterval;
	
	public Spectrum() {
		currentViewIdx = 0;
		currentView = views[currentViewIdx];
		minim = new Minim(new JSMinim(new MinimInitializer()));
	}

	@Override
	public void start(Stage stage) throws Exception {
		checkAndEnableGpio(stage);
		initGlobalProperties();
		startAudio();
        setupStage(stage);
		startFrameListener();
	}

	@Override
	public void stop() throws Exception {
		stopAudio();
		checkAndDisableGpio();
	}
	
	public boolean isPropertiesVisible() {
		return currentPropertyList != null;
	}
	
	private void checkAndEnableGpio(Stage stage) {
		if(ENABLE_GPIO) {
			gpio = new StageGpioController(stage);
		}
	}
	
	private void checkAndDisableGpio() {
		if(ENABLE_GPIO && gpio != null) {
			gpio.close();
		}
	}
	
	private void initGlobalProperties() {
		final String keyPrefix = "global.";
		propGlobalGain = createConfigurableIntegerProperty(
				keyPrefix + "gain", "Global Gain (%)", 10, 400, 100, 10);
		propViewAutoRotate = createConfigurableBooleanProperty(
				keyPrefix + "viewAutoRotate", "Auto Rotate Views", false);
		propViewAutoRotate.getProp().addListener((obs, oldVal, newVal) -> {
			if(newVal != oldVal) {
				if(newVal) {
					scheduleViewRotateTimer();
				} else {
					cancelViewRotateTimer();
				}
			}
		});
		propViewAutoRotateInterval = createConfigurableIntegerProperty(
				keyPrefix + "viewAutoRotateInterval", "View Rotate Int. (S)", 5, 6000, 60, 5);
		globalPropertyList = new ArrayList<>(Arrays.asList(
				propGlobalGain, propViewAutoRotate, propViewAutoRotateInterval));
	}

	private void startAudio() {
		in = minim.getLineIn(Minim.STEREO, BUFFER_SIZE, SAMPLING_RATE, 16);
		if(in == null) {
			throw new RuntimeException("Audio format not supported");
		}
		
		in.addListener(new AudioListener() {
			public void samples(float[] left, float[] right) {
				
				// Gain
				float gain = propGlobalGain.getProp().get() / 100f;
				for (int i = 0; i < left.length; i++) {
					left[i] = left[i] * gain;
				}
				for (int i = 0; i < right.length; i++) {
					right[i] = right[i] * gain;
				}
				
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
		
		// This is necessary for the fade out/in transitions when switching views 
		for (int i = 0; i < views.length; i++) {
			if(i != currentViewIdx) {
				views[i].getRoot().setOpacity(0);
			}
		}
		
		// Create scene
        stage.setScene(scene = new Scene(currentView.getRoot(), 
        		INIT_SCENE_WIDTH, INIT_SCENE_HEIGHT, false, SceneAntialiasing.DISABLED));
        scene.setFill(Color.BLACK);
        currentView.onShow();
        
        stage.setMaximized(true);
        stage.show();
        
        // Event handlers
        stage.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			public void handle(KeyEvent event) {
				onKey(event);
			}
		});
        
        // View rotate timer
        if(propViewAutoRotate.getProp().get()) {
        	scheduleViewRotateTimer();
        }
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
			if(isPropertiesVisible()) {
				if(!isPropertySliderInFocusAndNotLast()) {
					nextProperty();
				}
			} else {
				nextView();
			}
			break;
			
		case LEFT:
			if(isPropertiesVisible()) {
				if(!isPropertySliderInFocusAndNotFirst()) {
					prevProperty();
				}
			} else {
				prevView();
			}
			break;
		
		case SPACE:
			toggleCurrentViewPropertiesOn();
			break;
			
		case ESCAPE:
			togglePropertiesOff();
			break;
			
		case ENTER:
			toggleGlobalPropertiesOn();
			break;
			
		case UP:
			changeCurrentPropertyValue(true);
			break;
			
		case DOWN:
			changeCurrentPropertyValue(false);
			break;
			
		case C:
			if(event.isControlDown()) {
				Platform.exit();
			}
			break;
			
		default:
			break;
		}
	}
	
	private void toggleGlobalPropertiesOn() {
		if(!isPropertiesVisible()) {
			currentPropertyList = globalPropertyList;
			currentPropIdx = 0;
			showProperty(currentPropIdx);
		}
	}
	
	/**
	 * Resets the property index and shows first property of current view
	 */
	private void toggleCurrentViewPropertiesOn() {
		if(!isPropertiesVisible()) {
			currentPropertyList = currentView.getProperties();
			currentPropIdx = 0;
			showProperty(currentPropIdx);
		}
	}
	
	/**
	 * If properties are currently shown, hides the current property
	 */
	private void togglePropertiesOff() {
		if(isPropertiesVisible()) {
			hideProperty(currentPropertyNode);
			currentPropertyNode = null;
			currentPropertyList = null;
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
			
			UiUtils.createFadeOutTransition(currentView.getRoot(), 1000, new EventHandler<ActionEvent>() {
				public void handle(ActionEvent event) {
					
					// Trigger "hide" of current view
					currentView.onHide();
					
					// Set new current view and add to scene
					currentView = views[currentViewIdx];
					scene.setRoot(currentView.getRoot());
					
					UiUtils.createFadeInTransition(currentView.getRoot(), 1000, new EventHandler<ActionEvent>() {
						public void handle(ActionEvent event) {
							
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
					}).play();
					
				}
			}).play();
		}
	}
	
	private void nextProperty() {
		if(currentPropertyNode != null) {
			hideProperty(currentPropertyNode);
			currentPropertyNode = null;
		}
		
		currentPropIdx++;
		if(currentPropIdx > currentPropertyList.size() - 1) {
			currentPropIdx = currentPropertyList.size() - 1;
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
	private void showProperty(int idx) {

		if(!currentPropertyList.isEmpty()) {
			ConfigurableProperty<? extends Object> prop = currentPropertyList.get(idx);
			Region control = null;
			if(prop instanceof ConfigurableColorProperty) {
				ObjectProperty<Color> p = (ObjectProperty<Color>) prop.getProp();
				ColorControl picker = new ColorControl(p.getValue());
				p.bind(picker.colorProperty());
				control = picker;
				
			} else if(prop instanceof ConfigurableDoubleProperty || 
					prop instanceof ConfigurableIntegerProperty) {
				Label label = UiUtils.createNumberPropertyLabel(
						String.valueOf(prop.getProp().getValue()), currentView.getRoot());
				label.textProperty().bind(Bindings.createStringBinding(
						() -> {
							return String.valueOf(prop.getProp().get());
						}, prop.getProp()));
				control = label;
					
			} else if(prop instanceof ConfigurableBooleanProperty) {
				ObjectProperty<Boolean> p = (ObjectProperty<Boolean>) prop.getProp();
				CheckBox box = UiUtils.createBooleanPropertyCheckBox(
						p.getValue(), prop.getName(), currentView.getRoot());
				box.selectedProperty().bind(p);
				control = box;
			}
			
			currentView.getRoot().getChildren().add(
					currentPropertyNode = createPropertyPane(prop.getName(), control));
			UiUtils.createFadeInTransition(currentPropertyNode, 1000, null).play();
		}
	}
	
	private void hideProperty(Pane node) {
		currentPropertyList.get(currentPropIdx).getProp().unbind();
		UiUtils.createFadeOutTransition(node, 1000, new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				currentView.getRoot().getChildren().remove(node);
				node.getChildren().clear();
			}
		}).play();
	}
	
	private BorderPane createPropertyPane(String name, Region control) {
		Pane parent = currentView.getRoot();

		BorderPane pane = new BorderPane();
		pane.setCenter(control);
		BorderPane.setAlignment(control, Pos.CENTER);
		
		// This can be used to identify the control
		pane.setUserData("Property Control");
		
		// Automatically resize pane based on the scene size
		pane.prefWidthProperty().bind(parent.widthProperty().divide(2));
		pane.prefHeightProperty().bind(parent.heightProperty().divide(2));
		pane.layoutXProperty().bind(parent.widthProperty().subtract(pane.widthProperty()).divide(2));
		pane.layoutYProperty().bind(parent.heightProperty().subtract(pane.heightProperty()).divide(2));
		pane.setBackground(new Background(new BackgroundFill(Color.rgb(180, 180, 180), new CornerRadii(5), null)));
		pane.setBorder(new Border(new BorderStroke(Color.DARKGRAY, 
	            BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(2))));
		//pane.setOpacity(0.9);
		
		Label label = new Label(name);
		label.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", parent.widthProperty().divide(40), ";", 
				"-fx-padding: ", parent.widthProperty().divide(50), ";"));
		pane.setTop(label);
		
		// Automatically resize the contained property control based on the pane size
		control.prefWidthProperty().bind(pane.widthProperty().divide(2));
		control.prefHeightProperty().bind(pane.heightProperty().divide(4));
		
		return pane;
	}
	
	private void changeCurrentPropertyValue(boolean increment) {
		if(isPropertiesVisible()) {
			ConfigurableProperty<? extends Object> prop = 
					currentPropertyList.get(currentPropIdx);
			if (increment) {
				prop.increment();
			} else {
				prop.decrement();
			}
		}
	}
	
	private void scheduleViewRotateTimer() {
		int interval = propViewAutoRotateInterval.getProp().get() * 1000;
		viewRotateTimer = new Timer(true);
		viewRotateTimer.schedule(new TimerTask() {
			public void run() {
				
				// Don't switch views if a property is currently visible
				if(currentPropertyNode == null) {
					final int idx;
					if(currentViewIdx + 1 > views.length - 1) {
						idx = 0;
					} else {
						idx = currentViewIdx + 1;
					}
					Platform.runLater(new Runnable() {
						public void run() {
							switchView(idx);
						}
					});
				}
			}
		}, interval, interval);
	}
	
	private void cancelViewRotateTimer() {
		if(viewRotateTimer != null) {
			viewRotateTimer.cancel();
			viewRotateTimer = null;
		}
	}
	
	
	private boolean isPropertySliderInFocusAndNotLast() {
		boolean isVisible = false;
		Node focusOwner = currentView.getRoot().getScene().getFocusOwner();
		if(focusOwner instanceof Slider && isPropertiesVisible()) {
			Node control = currentPropertyNode.getCenter();
			if(control instanceof ColorControl) {
				isVisible = ((ColorControl) control).hasMoreSlidersToTheRight((Slider) focusOwner);
			}
		}
		return isVisible;
	}
	
	private boolean isPropertySliderInFocusAndNotFirst() {
		boolean isVisible = false;
		Node focusOwner = currentView.getRoot().getScene().getFocusOwner();
		if(focusOwner instanceof Slider && isPropertiesVisible()) {
			Node control = currentPropertyNode.getCenter();
			if(control instanceof ColorControl) {
				isVisible = ((ColorControl) control).hasMoreSlidersToTheLeft((Slider) focusOwner);
			}
		}
		return isVisible;
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
