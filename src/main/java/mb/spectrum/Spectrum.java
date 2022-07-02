package mb.spectrum;

import static mb.spectrum.UiUtils.createConfigurableBooleanProperty;
import static mb.spectrum.UiUtils.createConfigurableIntegerProperty;
import static mb.spectrum.UiUtils.createUtilityPane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import mb.spectrum.embedded.EmbeddedStrategy;
import mb.spectrum.prop.ConfigurableBooleanProperty;
import mb.spectrum.prop.ConfigurableChoiceProperty;
import mb.spectrum.prop.ConfigurableColorProperty;
import mb.spectrum.prop.ConfigurableIntegerProperty;
import mb.spectrum.prop.ConfigurableProperty;
import mb.spectrum.prop.IncrementalActionProperty;
import mb.spectrum.view.View;

public class Spectrum extends Application {
    
    private static final int SAMPLING_RATE = Integer.valueOf(
            ConfigService.getInstance().getOrCreateProperty("mb.sampling-rate", String.valueOf(48000)));
    private static final int BUFFER_SIZE = Integer.valueOf(
            ConfigService.getInstance().getOrCreateProperty("mb.buffer-size", String.valueOf(1024)));
    
    private static final int INIT_SCENE_WIDTH = 800;
    private static final int INIT_SCENE_HEIGHT = 480;
    private static final String VIEW_LABEL_COLOR = "#00aeff";
    private static final double VIEW_LABEL_FADE_IN_MS = 1000;
    private static final double VIEW_LABEL_LINGER_MS = 1000;
    private static final double VIEW_LABEL_FADE_OUT_MS = 1000;
    
    private PlatformStrategy strategy;
    private Scene scene;
    private StackPane stackPane;
    
    private List<View> views;
    private View currentView;
    private int currentViewIdx;
    private Timer viewRotateTimer;
    private boolean viewTransitioning;
    
    /* Property management */
    private PropertyPane propertyPane;
    private Transition currentPropertyTransition;
    private Map<Integer, Integer> lastPropertyMap;
    
    /* Global Properties */
    List<ConfigurableProperty<? extends Object>> globalPropertyList;
    private ConfigurableIntegerProperty propGlobalGain;
    private ConfigurableBooleanProperty propViewAutoRotate;
    private ConfigurableIntegerProperty propViewAutoRotateInterval;
    private ConfigurableBooleanProperty propEnableSmoothTransitions;
    
    public Spectrum() {
        views = new ViewLazyList(BUFFER_SIZE);
        currentViewIdx = 0;
        currentView = views.get(currentViewIdx);
        
        strategy = StrategyLoader.getInstance().getStrategy();
        /*
        if(DesktopStrategy.class.equals(strategy.getClass())) {
            views.add(new StereoAnalogMetersView());
        }
        */
    }

    @Override
    public void start(Stage stage) throws Exception {
        strategy.initialize(stage);
        createGlobalProperties();
        initLastPropertyMap();
        startAudio();
        setupStage(stage);
        startFrameListener();
    }

    @Override
    public void stop() throws Exception {
        stopAudio();
        strategy.close();
    }
    
    public boolean isPropertiesVisible() {
        return propertyPane.isVisible();
    }
    
    private boolean isPropertySelected() {
        return propertyPane.selectedProperty().get();
    }
    
    private void initLastPropertyMap() {
        lastPropertyMap = new HashMap<>(views.size());
        
        // -1 is used for the global property list
        for (int i = -1; i < views.size(); i++) {
            lastPropertyMap.put(i, 0);
        }
        
    }
    
    private void createGlobalProperties() {
        final String keyPrefix = "global.";
        propGlobalGain = createConfigurableIntegerProperty(
                keyPrefix + "gain", "Global Gain", 10, 400, 100, 10, "%");
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
                keyPrefix + "viewAutoRotateInterval", "View Rotate Interval", 5, 6000, 60, 5, "sec");
        propEnableSmoothTransitions = createConfigurableBooleanProperty(
                keyPrefix + "enableSmoothTransitions", "Enable Smooth Transitions", true);
        
        globalPropertyList = new ArrayList<>(Arrays.asList(
                propGlobalGain, propViewAutoRotate, propViewAutoRotateInterval, propEnableSmoothTransitions));
        
        // Poweroff
        if(strategy instanceof EmbeddedStrategy) {
            IncrementalActionProperty propPoweroff = new IncrementalActionProperty("Power Off", 8);
            propPoweroff.setOnAction((e) -> {
                togglePropertiesOff();
                UiUtils.createAndShowShutdownPrompt(strategy.getStage(), true);
            });
            globalPropertyList.add(propPoweroff);
        }
    }

    private void startAudio() {
        
        String path = getParameters().getNamed().get("file");
        if(path != null) {
            strategy.startAudio(path, BUFFER_SIZE);
        } else {
            strategy.startAudio(true, BUFFER_SIZE, SAMPLING_RATE, 16);
        }
        
        strategy.setListener(new AudioListener() {
            public void samples(float[] left, float[] right) {
                
                // Global gain
                float gain = propGlobalGain.getProp().get() / 100f;
                for (int i = 0; i < left.length; i++) {
                    left[i] = left[i] * gain;
                }
                for (int i = 0; i < right.length; i++) {
                    right[i] = right[i] * gain;
                }
                
                currentView.dataAvailable(left, right);
            }
        });
    }
    
    private void stopAudio() {
        strategy.stopAudio();
    }
    
    private void setupStage(Stage stage) {
        
        /* Create scene and root stack pane */
        stackPane = new StackPane(currentView.getRoot());
        
        // This is needed for correct smooth transitions
        stackPane.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
        stackPane.setAlignment(Pos.CENTER);
        stage.setScene(scene = new Scene(stackPane, 
                INIT_SCENE_WIDTH, INIT_SCENE_HEIGHT, false, 
                strategy instanceof EmbeddedStrategy ? SceneAntialiasing.DISABLED : SceneAntialiasing.BALANCED));
        scene.setFill(Color.BLACK);
        currentView.onShow();
        stackPane.prefWidthProperty().bind(scene.widthProperty());
        
        stage.setMaximized(!Boolean.valueOf(
                getParameters().getNamed().get("windowed")));
        stage.show();
        
        /* Event handlers */
        stage.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            public void handle(KeyEvent event) {
                onKey(event);
            }
        });
        
        /* View rotate timer */
        if(propViewAutoRotate.getProp().get()) {
            scheduleViewRotateTimer();
        }
        
        /* Property pane */
        propertyPane = createPropertyPane();
        propertyPane.setProperties(currentView.getProperties());
        stackPane.getChildren().add(propertyPane);
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
                
                // Update the value if a property is currently selected, else move to the next property
                if(isPropertySelected()) { 
                    changeCurrentPropertyValue(true, event.isShiftDown());
                } else {
                    nextProperty();
                }
                
            } else {
                nextView();
            }
            break;
            
        case LEFT:
            if(isPropertiesVisible()) {
                
                // Update the value if a property is currently selected, else move to the previous property
                if(isPropertySelected()) {
                    changeCurrentPropertyValue(false, event.isShiftDown());
                } else {
                    prevProperty();
                }
            } else {
                prevView();
            }
            break;
        
        case SPACE:
            if(event.isControlDown()) {
                if(isPropertiesVisible()) {
                    togglePropertiesOff();
                } else {
                    toggleGlobalPropertiesOn();
                }
            } else {
                if(isPropertiesVisible()) {
                    selectOrDeselectCurrentProperty();
                } else {
                    toggleCurrentViewPropertiesOn();
                }
            }
            
            // Prevents checkbox selection if the currently visible property is a boolean
            event.consume();
            break;
            
        case ENTER:
            if(isPropertiesVisible()) {
                firePropertyButtonIfInFocus();
            }
            break;
            
        case UP:
            if(isPropertiesVisible()) { 
                changeCurrentPropertyValue(true, event.isShiftDown());
            }
            break;
            
        case DOWN:
            if(isPropertiesVisible()) {
                changeCurrentPropertyValue(false, event.isShiftDown());
            }
            break;
            
        case C:
            if(event.isControlDown()) {
                Platform.exit();
            }
            break;
            
        case F:
            if(event.isControlDown()) {
                Stage stage = (Stage) scene.getWindow();
                stage.setFullScreen(true);
            }
            break;
            
        case ESCAPE:
            if(isPropertiesVisible()) {
                hideProperties();
            }
            break;
            
            
        default:
            break;
        }
    }
    
    /**
     * Triggers the current property pane fade out after two seconds
     */
    private void schedulePropertyFadeOut() {
        if(isPropertiesVisible() && isSmoothTransitionsEnabled()) {
            currentPropertyTransition = new SequentialTransition(
                    new PauseTransition(Duration.seconds(2)), 
                    UiUtils.createFadeOutTransition(propertyPane, 1000, 0.5, null));
            currentPropertyTransition.play();
        }
    }
    
    /**
     * Cancels the current property's transition
     */
    private void cancelPropertyFadeOutIfPlaying() {
        if(isPropertiesVisible() && isSmoothTransitionsEnabled()) {
            if(currentPropertyTransition != null) {
                currentPropertyTransition.stop();
            }
            propertyPane.setOpacity(1);
        }
    }
    
    /**
     * Toggles on the global properties pane
     */
    private void toggleGlobalPropertiesOn() {
        if(!isPropertiesVisible()) {
            propertyPane.setProperties(globalPropertyList);
            propertyPane.setCurrPropIdx(0);
            showProperties();
        }
    }
    
    /**
     * Resets the property index and shows first property of current view
     */
    private void toggleCurrentViewPropertiesOn() {
        if(!isPropertiesVisible() && !currentView.getProperties().isEmpty()) {
            propertyPane.setProperties(currentView.getProperties());
            propertyPane.setCurrPropIdx(lastPropertyMap.get(currentViewIdx));
            showProperties();
        }
    }
    
    /**
     * If properties are currently shown, hides the current property
     */
    private void togglePropertiesOff() {
        if(isPropertiesVisible()) {
            hideProperties();
        }
    }
    
    /**
     * Switch to the next view from the list.
     */
    private void nextView() {
        if(!viewTransitioning) {
            int idx = currentViewIdx + 1;
            if(idx > views.size() - 1) {
                idx = views.size() - 1;
            }
            switchView(idx);
        }
    }
    
    /**
     * Switch to the previous view from the list.
     */
    private void prevView() {
        if(!viewTransitioning) {
            int idx = currentViewIdx - 1;
            if(idx < 0) {
                idx = 0;
            }
            switchView(idx);
        }
    }
    
    private void switchView(int idx) {
        if(idx != currentViewIdx) {
            currentViewIdx = idx;
            
            // Reset properties
            togglePropertiesOff();
            
            if(isSmoothTransitionsEnabled()) {
            
                viewTransitioning = true;
                UiUtils.createFadeOutTransition(currentView.getRoot(), 500, new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent event) {
                    
                        // Trigger "hide" of current view
                        currentView.onHide();
                    
                        // Set new current view and add to scene
                        currentView = views.get(currentViewIdx);
                        stackPane.getChildren().set(0, currentView.getRoot());
                    
                        UiUtils.createFadeInTransition(currentView.getRoot(), 500, new EventHandler<ActionEvent>() {
                            public void handle(ActionEvent event) {
                            
                                // Trigger "show" of new view
                                currentView.onShow();
                                
                                // Show view label with animation
                                /*
                                Pane parent = currentView.getRoot();
                            
                                BorderPane title = createViewTitlePane(currentView.getName());
                                parent.getChildren().add(title);
                            
                                Transition trans = UiUtils.createFadeInOutTransition(
                                        title, VIEW_LABEL_FADE_IN_MS, VIEW_LABEL_LINGER_MS, VIEW_LABEL_FADE_OUT_MS, 
                                        new EventHandler<ActionEvent>() {
                                            public void handle(ActionEvent event) {
                                                parent.getChildren().remove(title);
                                            }
                                        });
                                trans.play();
                                */
                                
                                viewTransitioning = false;
                            
                            }
                        }).play();
                    
                    }
                }).play();
            } else {
                
                // Trigger "hide" of current view
                currentView.onHide();
            
                // Set new current view and add to scene
                currentView = views.get(currentViewIdx);
                currentView.getRoot().setOpacity(1);
                stackPane.getChildren().set(0, currentView.getRoot());
                
                // Trigger "show" of new view
                currentView.onShow();
            }
        }
    }
    
    private void nextProperty() {
        cancelPropertyFadeOutIfPlaying();
        
        // Special handling of color properties
        if(propertyPane.getControl() instanceof ColorControl) {
            ColorControl control = (ColorControl) propertyPane.getControl();
            if(!control.isLastSelected()) {
                control.selectNextGauge();
                
                // Resume fadeout
                schedulePropertyFadeOut();
                return;
            }
        }
        
        // Increment the current property
        propertyPane.nextProperty();
        updateLastPropertyMap();
        
        // Resume fadeout
        schedulePropertyFadeOut();
    }
    
    private void prevProperty() {
        cancelPropertyFadeOutIfPlaying();
        
        // Special handling of color properties
        if(propertyPane.getControl() instanceof ColorControl) {
            ColorControl control = (ColorControl) propertyPane.getControl();
            if(!control.isFirstSelected()) {
                control.selectPrevGauge();
                
                // Resume fadeout
                schedulePropertyFadeOut();
                return;
            }
        }
        
        // Decrement the current property
        propertyPane.prevProperty();
        updateLastPropertyMap();
        
        // Resume fadeout
        schedulePropertyFadeOut();
    }
    
    private void selectOrDeselectCurrentProperty() {
        cancelPropertyFadeOutIfPlaying();
        
        BooleanProperty selectedProperty = propertyPane.selectedProperty();
        selectedProperty.set(!selectedProperty.get());
        
        // Resume fadeout
        schedulePropertyFadeOut();
    }
    
    private void showProperties() {
        if(!propertyPane.getProperties().isEmpty()) {
            
            propertyPane.setVisible(true);
            
            if(isSmoothTransitionsEnabled()) {
                UiUtils.createFadeInTransition(propertyPane, 1000, new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent event) {
                        schedulePropertyFadeOut();
                    }
                }).play();
            }
        }
    }
    
    private void hideProperties() {
        
        // Remove all bindings of current property and deselect
        propertyPane.getCurrProperty().getProp().unbind();
        propertyPane.selectedProperty().set(false);
        
        if(isSmoothTransitionsEnabled()) {
            UiUtils.createFadeOutTransition(propertyPane, 1000, new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                    propertyPane.setVisible(false);
                }
            }).play();
        } else {
            propertyPane.setVisible(false);
        }
    }
    
    private void updateLastPropertyMap() {
        lastPropertyMap.put(currentViewIdx, propertyPane.getCurrPropIdx());
    }
    
    private BorderPane createViewTitlePane(String viewName) {
        
        Label label = new Label(viewName);
        Pane parent = currentView.getRoot();
        label.styleProperty().bind(Bindings.concat(
                "-fx-font-size: ", parent.widthProperty().divide(20), ";",
                "-fx-font-family: 'Alex Brush';",
                "-fx-text-fill: ", VIEW_LABEL_COLOR, ";"));
        label.setEffect(new Glow(1));
        
        BorderPane pane = createUtilityPane(currentView.getRoot(), 1.5, 4, 0.6);
        pane.setCenter(label);
        BorderPane.setAlignment(label, Pos.CENTER);
        
        return pane;
    }
    
    private boolean isSmoothTransitionsEnabled() {
        return propEnableSmoothTransitions.get();
    }
    
    private PropertyPane createPropertyPane() {
        
        PropertyPane pane = new PropertyPane();
        pane.widthRatioProperty().set(1.1);
        pane.heightRatioProperty().set(1.2);
        pane.setVisible(false);
        
        // TODO Verify this is not necessary anymore
        // This can be used to identify the control
        pane.setUserData("Property Control");
        
        return pane;
    }
    
    private void changeCurrentPropertyValue(boolean increment, boolean reverseIfChoiceProp) {
        cancelPropertyFadeOutIfPlaying();
        ConfigurableProperty<? extends Object> prop = propertyPane.getCurrProperty();
        
        // Special handling of choice properties
        if(prop instanceof ConfigurableChoiceProperty && reverseIfChoiceProp) {
            increment = !increment;
        }
        
        // Special handling of color properties
        if(prop instanceof ConfigurableColorProperty) {
            ColorControl control = (ColorControl) propertyPane.getControl();
            if(increment) {
                control.incrementCurrent();
            } else {
                control.decrementCurrent();
            }
            return;
        }
        
        if (increment) {
            prop.increment();
        } else {
            prop.decrement();
        }
        schedulePropertyFadeOut();
    }
    
    private void scheduleViewRotateTimer() {
        int interval = propViewAutoRotateInterval.getProp().get() * 1000;
        viewRotateTimer = new Timer(true);
        viewRotateTimer.schedule(new TimerTask() {
            public void run() {
                
                // Don't switch views if a property is currently visible
                if(!isPropertiesVisible()) {
                    final int idx;
                    if(currentViewIdx + 1 > views.size() - 1) {
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
    
    private void firePropertyButtonIfInFocus() {
        Node focusOwner = currentView.getRoot().getScene().getFocusOwner();
        if(focusOwner instanceof Button && isPropertiesVisible()) {
            cancelPropertyFadeOutIfPlaying();
            ((Button) focusOwner).fire();
            schedulePropertyFadeOut();
        }
    }
    
    public static void main(String[] args) {
//    	SvgImageLoaderFactory.install(new AttributeDimensionProvider());
        launch(args);
    }
}
