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

import org.kordamp.ikonli.dashicons.Dashicons;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
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
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import mb.spectrum.desktop.DesktopStrategy;
import mb.spectrum.prop.ActionProperty;
import mb.spectrum.prop.ConfigurableBooleanProperty;
import mb.spectrum.prop.ConfigurableChoiceProperty;
import mb.spectrum.prop.ConfigurableColorProperty;
import mb.spectrum.prop.ConfigurableDoubleProperty;
import mb.spectrum.prop.ConfigurableIntegerProperty;
import mb.spectrum.prop.ConfigurableProperty;
import mb.spectrum.view.AnalogMeterView;
import mb.spectrum.view.AnalogMeterView.Orientation;
import mb.spectrum.view.GaugeView;
import mb.spectrum.view.SoundWaveView;
import mb.spectrum.view.SpectrumAreaView;
import mb.spectrum.view.SpectrumBarView;
import mb.spectrum.view.StereoAnalogMetersView;
import mb.spectrum.view.StereoGaugeView;
import mb.spectrum.view.StereoLevelsLedView;
import mb.spectrum.view.StereoLevelsLedView3D;
import mb.spectrum.view.StereoLevelsView;
import mb.spectrum.view.View;

public class Spectrum extends Application {
    
    private static final int SAMPLING_RATE = Integer.valueOf(
            ConfigService.getInstance().getOrCreateProperty("mb.sampling-rate", String.valueOf(48000)));
    private static final int BUFFER_SIZE = Integer.valueOf(
            ConfigService.getInstance().getOrCreateProperty("mb.buffer-size", String.valueOf(2048)));
    
    private static final int INIT_SCENE_WIDTH = 800;
    private static final int INIT_SCENE_HEIGHT = 600;
    private static final String VIEW_LABEL_COLOR = "#00aeff";
    private static final double VIEW_LABEL_FADE_IN_MS = 1000;
    private static final double VIEW_LABEL_LINGER_MS = 1000;
    private static final double VIEW_LABEL_FADE_OUT_MS = 1000;
    private static final int PROPS_BEFORE_AND_AFTER = 4;
    
    private PlatformStrategy strategy;
    private Scene scene;
    private List<View> views = new ArrayList<>(
            Arrays.asList(
                    new StereoGaugeView(),
                    new GaugeView("Analog Meter", "gaugeView", false),
                    new SoundWaveView(BUFFER_SIZE),
                    new StereoLevelsLedView3D(),
                    new AnalogMeterView("Analog Meter", "analogMeterView", "Peak", Orientation.HORIZONTAL),
                    new StereoLevelsLedView(),
                    new SpectrumBarView(),
                    new SpectrumAreaView(),
                    new StereoLevelsView()
            )
        );
    private View currentView;
    private int currentViewIdx;
    private Timer viewRotateTimer;
    
    /* Property management */
    private List<ConfigurableProperty<? extends Object>> currentPropertyList;
    private int currentPropIdx;
    private BorderPane currentPropertyNode;
    private Transition currentPropertyTransition;
    private Map<Integer, Integer> lastPropertyMap;
    
    /* Global Properties */
    List<ConfigurableProperty<? extends Object>> globalPropertyList;
    private ConfigurableIntegerProperty propGlobalGain;
    private ConfigurableBooleanProperty propViewAutoRotate;
    private ConfigurableIntegerProperty propViewAutoRotateInterval;
    private ConfigurableBooleanProperty propEnableSmoothTransitions;
    
    public Spectrum() {
        currentViewIdx = 0;
        currentView = views.get(currentViewIdx);
        initLastPropertyMap();
        strategy = StrategyLoader.getInstance().getStrategy();
        
        if(DesktopStrategy.class.equals(strategy.getClass())) {
            views.add(new StereoAnalogMetersView());
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        strategy.initialize(stage);
        initGlobalProperties();
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
        return currentPropertyList != null;
    }
    
    private void initLastPropertyMap() {
        lastPropertyMap = new HashMap<>(views.size());
        
        // -1 is used for the global property list
        for (int i = -1; i < views.size(); i++) {
            lastPropertyMap.put(i, 0);
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
        propEnableSmoothTransitions = createConfigurableBooleanProperty(
                keyPrefix + "enableSmoothTransitions", "Enable Smooth Transitions", true);
        globalPropertyList = new ArrayList<>(Arrays.asList(
                propGlobalGain, propViewAutoRotate, propViewAutoRotateInterval, propEnableSmoothTransitions));
    }

    private void startAudio() {
        
        Parameters params = getParameters();
        String path = params.getNamed().get("file");
        
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
        
        // This is necessary for the fade out/in transitions when switching views 
        if(isSmoothTransitionsEnabled()) {
            for (int i = 0; i < views.size(); i++) {
                if(i != currentViewIdx) {
                    views.get(i).getRoot().setOpacity(0);
                }
            }
        }
        
        // Create scene
        stage.setScene(scene = new Scene(currentView.getRoot(), 
                INIT_SCENE_WIDTH, INIT_SCENE_HEIGHT, false, SceneAntialiasing.BALANCED));
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
                } else {
                    cancelPropertyFadeOutIfPlaying();
                    schedulePropertyFadeOut();
                }
            } else {
                nextView();
            }
            break;
            
        case LEFT:
            if(isPropertiesVisible()) {
                if(!isPropertySliderInFocusAndNotFirst()) {
                    prevProperty();
                } else {
                    cancelPropertyFadeOutIfPlaying();
                    schedulePropertyFadeOut();
                }
            } else {
                prevView();
            }
            break;
        
        case SPACE:
            if(event.isControlDown()) {
                toggleGlobalPropertiesOn();
            } else {
                if(isPropertiesVisible()) {
                    togglePropertiesOff();
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
                changeCurrentPropertyValue(true);
            }
            break;
            
        case DOWN:
            if(isPropertiesVisible()) {
                changeCurrentPropertyValue(false);
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
                    UiUtils.createFadeOutTransition(currentPropertyNode, 1000, 0.5, null));
            currentPropertyTransition.play();
        }
    }
    
    /**
     * Cancels the current property's transition
     */
    private void cancelPropertyFadeOutIfPlaying() {
        if(isPropertiesVisible() && isSmoothTransitionsEnabled()) {
            currentPropertyTransition.stop();
            currentPropertyNode.setOpacity(1);
        }
    }
    
    /**
     * Toggles on the global properties pane
     */
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
        if(!isPropertiesVisible() && !currentView.getProperties().isEmpty()) {
            currentPropertyList = currentView.getProperties();
            currentPropIdx = lastPropertyMap.get(currentViewIdx);
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
        if(idx > views.size() - 1) {
            idx = views.size() - 1;
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
            
            if(isSmoothTransitionsEnabled()) {
            
                UiUtils.createFadeOutTransition(currentView.getRoot(), 500, new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent event) {
                    
                        // Trigger "hide" of current view
                        currentView.onHide();
                    
                        // Set new current view and add to scene
                        currentView = views.get(currentViewIdx);
                        scene.setRoot(currentView.getRoot());
                    
                        UiUtils.createFadeInTransition(currentView.getRoot(), 500, new EventHandler<ActionEvent>() {
                            public void handle(ActionEvent event) {
                            
                                // Trigger "show" of new view
                                currentView.onShow();
                                
                                // Show view label with animation
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
                            
                            }
                        }).play();
                    
                    }
                }).play();
            } else {
                
                // Trigger "hide" of current view
                //currentView.onHide();
            
                // Set new current view and add to scene
                currentView = views.get(currentViewIdx);
                currentView.getRoot().setOpacity(1);
                scene.setRoot(currentView.getRoot());
                
                // Trigger "show" of new view
                //currentView.onShow();
            }
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
        updateLastPropertyMap();
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
        updateLastPropertyMap();
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
                    prop instanceof ConfigurableIntegerProperty || 
                    prop instanceof ConfigurableChoiceProperty) {
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
            } else if(prop instanceof ActionProperty) {
                Button button = UiUtils.createActionPropertyButton(prop.getName());
                button.setOnAction(new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent event) {
                        ((ActionProperty) prop).trigger();
                    }
                });
                control = button;
            }
            
            currentView.getRoot().getChildren().add(
                    currentPropertyNode = createPropertyPane(prop.getName(), control));
            
            if(isSmoothTransitionsEnabled()) {
                UiUtils.createFadeInTransition(currentPropertyNode, 1000, null).play();
                schedulePropertyFadeOut();
            }
        }
    }
    
    private void hideProperty(Pane node) {
        currentPropertyList.get(currentPropIdx).getProp().unbind();
        
        if(isSmoothTransitionsEnabled()) {
            UiUtils.createFadeOutTransition(node, 1000, new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                    currentView.getRoot().getChildren().remove(node);
                    node.getChildren().clear();
                }
            }).play();
        } else {
            currentView.getRoot().getChildren().remove(node);
            node.getChildren().clear();
        }
    }
    
    private void updateLastPropertyMap() {
        lastPropertyMap.put(currentViewIdx, currentPropIdx);
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
    
    private BorderPane createPropertyPane(String name, Region control) {
        
        BorderPane pane = createUtilityPane(currentView.getRoot(), 2, 2, 1);

        pane.setCenter(control);
        BorderPane.setAlignment(control, Pos.CENTER);
        
        // This can be used to identify the control
        pane.setUserData("Property Control");

        // Show a few properties before and after the current one
        VBox box = new VBox();
        box.setStyle("-fx-padding: 10");
        
        int startIdx = Math.min(currentPropIdx - PROPS_BEFORE_AND_AFTER, 
                currentPropertyList.size() - 1 - PROPS_BEFORE_AND_AFTER * 2);
        startIdx = startIdx < 0 ? 0 : startIdx;
        
        int endIdx = Math.max(currentPropIdx + PROPS_BEFORE_AND_AFTER, PROPS_BEFORE_AND_AFTER * 2);
        endIdx = endIdx > currentPropertyList.size() - 1 ? currentPropertyList.size() - 1 : endIdx;
        
        for (int i = startIdx; i < endIdx + 1; i++) {
            Text text = new Text(currentPropertyList.get(i).getName());
            
            if(i == currentPropIdx) {
                text.setFill(Color.BLACK);
                text.setUnderline(true);
            } else {
                text.setFill(Color.DIMGRAY);
            }
            
            text.fontProperty().bind(Bindings.createObjectBinding(
                    () -> {
                        return Font.font(currentView.getRoot().widthProperty().get() / 50);
                    }, currentView.getRoot().widthProperty()));

            box.getChildren().add(text);
        }
        
        // Show arrows if there are more properties above/below
        FontIcon iconUp = FontIcon.of(Dashicons.ARROW_UP);
        iconUp.setVisible(startIdx > 0);
        iconUp.iconSizeProperty().bind(currentView.getRoot().widthProperty().divide(30));
        
        FontIcon iconDown = FontIcon.of(Dashicons.ARROW_DOWN);
        iconDown.setVisible(endIdx < currentPropertyList.size() - 1);
        iconDown.iconSizeProperty().bind(iconUp.iconSizeProperty());
        
        box.getChildren().add(0, iconUp);
        box.getChildren().add(iconDown);
        
        // Align and add property list to pane
        box.setAlignment(Pos.CENTER_LEFT);
        pane.setLeft(box);
        
        // Automatically resize the contained property control based on the pane size
        control.prefWidthProperty().bind(pane.widthProperty().divide(2));
        control.prefHeightProperty().bind(pane.heightProperty().divide(4));
        
        return pane;
    }
    
    private void changeCurrentPropertyValue(boolean increment) {
        cancelPropertyFadeOutIfPlaying();
        ConfigurableProperty<? extends Object> prop = currentPropertyList.get(currentPropIdx);
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
                if(currentPropertyNode == null) {
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
    
    private void firePropertyButtonIfInFocus() {
        Node focusOwner = currentView.getRoot().getScene().getFocusOwner();
        if(focusOwner instanceof Button && isPropertiesVisible()) {
            cancelPropertyFadeOutIfPlaying();
            ((Button) focusOwner).fire();
            schedulePropertyFadeOut();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
