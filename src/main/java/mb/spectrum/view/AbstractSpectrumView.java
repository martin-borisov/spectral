package mb.spectrum.view;

import static mb.spectrum.UiUtils.createConfigurableBooleanProperty;
import static mb.spectrum.UiUtils.createConfigurableChoiceProperty;
import static mb.spectrum.UiUtils.createConfigurableDoubleProperty;
import static mb.spectrum.UiUtils.createConfigurableIntegerProperty;
import static mb.spectrum.Utils.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ddf.minim.analysis.FFT;
import ddf.minim.analysis.FourierTransform;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import mb.spectrum.ConfigService;
import mb.spectrum.UiUtils;
import mb.spectrum.Utils;
import mb.spectrum.prop.ConfigurableBooleanProperty;
import mb.spectrum.prop.ConfigurableChoiceProperty;
import mb.spectrum.prop.ConfigurableColorProperty;
import mb.spectrum.prop.ConfigurableDoubleProperty;
import mb.spectrum.prop.ConfigurableIntegerProperty;
import mb.spectrum.prop.ConfigurableProperty;
import mb.spectrum.view.AnalogMeterView.Orientation;

public abstract class AbstractSpectrumView extends AbstractMixedChannelView {
    
    private enum WindowSize {
        SMALL(1), MEDIUM(2), LARGE(4);
        
        private int value;

        private WindowSize(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    private static final double GRID_LABELS_MARGIN_RATIO = 0.1;
    private static final double PIP_MARGIN_RATIO = 0.05;
    private static final int LABEL_SIZE_RATIO = 4;
    
    private static final int SAMPLING_RATE = Integer.valueOf(
            ConfigService.getInstance().getProperty("mb.sampling-rate"));
    private static final int BUFFER_SIZE = Integer.valueOf(
            ConfigService.getInstance().getProperty("mb.buffer-size"));
    
    /* Configuration properties */
    protected ConfigurableIntegerProperty propMinDbValue;
    private ConfigurableChoiceProperty propWindowSize;
    private ConfigurableDoubleProperty propSensitivity;
    private ConfigurableDoubleProperty propTrailStayFactor;
    private ConfigurableDoubleProperty propTrailAccelerationFactor;
    private ConfigurableIntegerProperty propDbLinesCount;
    private ConfigurableIntegerProperty propHzLineOnNthBar;
    private ConfigurableColorProperty propGridColor;
    private ConfigurableBooleanProperty propShowPip;
    private ConfigurableChoiceProperty propPipViewType;
    private ConfigurableDoubleProperty propPipOpacity;
    
    /* Operational properties */
    protected List<SimpleDoubleProperty> bandValues;
    protected List<SimpleDoubleProperty> trailValues;
    
    private List<Line> vLines, hLines;
    private List<Label> vLabels, hLabels;
    private FFT fft;
    private SubScene pip;
    
    protected int bandCount;
    private double[] bandValuesDB, trailValuesDB;
    private double[] trailOpValues;
    private Timeline mainAnimationTimeline;
    private Timeline[] timelines;
    private int buffFramesCounter;
    private float[] buffer;
    private boolean resetting;
    
    private Map<String, View> subViews;
    
    public AbstractSpectrumView() {
        super(true);
        createSubViews();
        bandValues = new ArrayList<>();
        trailValues = new ArrayList<>();
        buffFramesCounter = 0;
        init();
    }
    
    protected abstract String getBasePropertyKey();
    
    private void createSubViews() {
        subViews = new LinkedHashMap<>();
        subViews.put("Analog Meter", 
                new AnalogMeterView("Analog Meter", "analogMeterView", "Peak", Orientation.HORIZONTAL));
        subViews.put("Stereo Analog Meters", new StereoAnalogMetersView());
        subViews.put("Stereo Levels LED", new StereoLevelsLedView());
        subViews.put("Stereo Levels", new StereoLevelsView());
    }
    
    @Override
    public List<ConfigurableProperty<? extends Object>> getProperties() {
        return Arrays.asList(propMinDbValue, 
                propWindowSize,
                propSensitivity,
                propTrailStayFactor,
                propTrailAccelerationFactor,
                propDbLinesCount,
                propHzLineOnNthBar,
                propShowPip,
                propPipViewType,
                propPipOpacity,
                propGridColor);
    }
    
    @Override
    protected void initProperties() {
        
        /* Configuration Properties */
        propMinDbValue = createConfigurableIntegerProperty(
                getBasePropertyKey() + ".minDbValue", "Min. DB Value", -100, 0, -60, 1, "dB");
        propWindowSize = UiUtils.createConfigurableChoiceProperty(
                getBasePropertyKey() + ".windowSize", "FFT Window Size", WindowSize.class);
        propWindowSize.getProp().addListener((obs, oldVal, newVal) -> {
            if(!newVal.equals(oldVal)) {
                reset();
            }
        });
        propSensitivity = createConfigurableDoubleProperty(
                getBasePropertyKey() + ".sensitivity", "Sensitivity", 1.0, 5.0, 1.0, 0.1);
        propTrailStayFactor = createConfigurableDoubleProperty(
                getBasePropertyKey() + ".trailStayFactor", "Trail Stay", 0.001, 0.05, 0.01, 0.001);
        propTrailAccelerationFactor = createConfigurableDoubleProperty(
                getBasePropertyKey() + ".trailAccelerationFactor", "Trail Acceleration", 1.0, 1.2, 1.08, 0.01);
        propDbLinesCount = createConfigurableIntegerProperty(
                getBasePropertyKey() + ".dbLineCount", "dB Lines Count", 1, 20, 4, 1);
        propDbLinesCount.getProp().addListener((obs, oldVal, newVal) -> {
            if(newVal != oldVal) {
                reset();
            }
        });
        propHzLineOnNthBar = createConfigurableIntegerProperty(
                getBasePropertyKey() + ".hzLineOnNthBar", "Hz Line on Nth Bar", 1, 20, 5, 1);
        propHzLineOnNthBar.getProp().addListener((obs, oldVal, newVal) -> {
            if(newVal != oldVal) {
                reset();
            }
        });
        propShowPip = createConfigurableBooleanProperty(
                getBasePropertyKey() + ".showPip", "Show Pip", true);
        propPipViewType = createConfigurableChoiceProperty(
                getBasePropertyKey() + ".pipViewType", "PIP View Type", 
                new ArrayList<>(subViews.keySet()), subViews.keySet().iterator().next());
        propPipOpacity =  createConfigurableDoubleProperty(
                getBasePropertyKey() + ".pipOpacity", "PIP Opacity", 0.0, 1.0, 0.8, 0.05);
        propGridColor = UiUtils.createConfigurableColorProperty(
                getBasePropertyKey() + ".gridColor", "Grid Color", Color.web("#fd4a11"));
    }

    @Override
    protected List<Node> collectNodes() {
        
        vLines = new ArrayList<>();
        hLines = new ArrayList<>();
        vLabels = new ArrayList<>();
        hLabels = new ArrayList<>();
        
        // Get number of bands
        fft = new FFT(BUFFER_SIZE * getBufferSizeMultiplier(), SAMPLING_RATE);
        fft.window(FourierTransform.BLACKMAN);
        buffer = new float[BUFFER_SIZE * getBufferSizeMultiplier()];
        
        // For 44100 the values should be 22, 3
        // TODO bandsPerOctave should be configurable
        fft.logAverages(24, getBufferSizeMultiplier() * 4);
                
        bandCount = fft.avgSize();
                
        bandValuesDB = new double[bandCount];
        Arrays.fill(bandValuesDB, propMinDbValue.getProp().get());
        for (int i = 0; i < bandCount; i++) {
            bandValues.add(new SimpleDoubleProperty(propMinDbValue.getProp().get()));
        }
                
        trailValuesDB = new double[bandCount];
        Arrays.fill(trailValuesDB, propMinDbValue.getProp().get());
        for (int i = 0; i < bandCount; i++) {
            trailValues.add(new SimpleDoubleProperty(propMinDbValue.getProp().get()));
        }
                
        trailOpValues = new double[bandCount];
        Arrays.fill(trailOpValues, propTrailStayFactor.getProp().get());
        
        timelines = new Timeline[bandCount];
        for (int i = 0; i < bandValuesDB.length; i++) {
            timelines[i] = new Timeline();
        }
        
        for (int i = 0; i < bandCount; i++) {
            
            // Create grid lines and labels
            if(i % propHzLineOnNthBar.getProp().get() == 0) {
                createHzLineAndLabel(i, Math.round(fft.getAverageCenterFrequency(i) - fft.getAverageBandWidth(i) / 2));
            } else if(i == bandCount - 1) {
                createHzLineAndLabel(i + 1, Math.round(fft.getAverageCenterFrequency(i + 1) - fft.getAverageBandWidth(i + 1) / 2));
            }
        }
        
        // DB lines and labels (horizontal)
        for (int i = 0; i <= propDbLinesCount.getProp().get(); i++) {
            createDbGridLineAndLabel(i);
        }
        
        /* PIP View */
        
        // Get initial sub view, preventing erroneous configuration property values
        View view = subViews.get(propPipViewType.getProp().get());
        if(view == null) {
            view = subViews.get(subViews.keySet().iterator().next());
        }
        
        // Preserve the sub scene due to issues when resetting the view (the same root has to be added to a new sub scene instance,
        // which results in an exception)
        if(pip == null) {
            pip = new SubScene(view.getRoot(), 
                    pane.getWidth() / 4, pane.getHeight() / 4, true, SceneAntialiasing.BALANCED);
            pip.layoutXProperty().bind(pane.heightProperty().multiply(PIP_MARGIN_RATIO));
            pip.layoutYProperty().bind(pip.layoutXProperty());
            pip.widthProperty().bind(pane.widthProperty().divide(4));
            pip.heightProperty().bind(pane.heightProperty().divide(4));
            pip.visibleProperty().bind(propShowPip.getProp());
            pip.setMouseTransparent(true);
            pip.rootProperty().bind(Bindings.createObjectBinding(
                    () -> {
                        return subViews.get(propPipViewType.getProp().get()).getRoot();
                    }, propPipViewType.getProp()));
            pip.opacityProperty().bind(propPipOpacity.getProp());
        }
        
        // This should happen here, needed when resetting the view
        startAnimation();
        
        List<Node> shapes = new ArrayList<>();
        shapes.addAll(vLines);
        shapes.addAll(vLabels);
        shapes.addAll(hLines);
        shapes.addAll(hLabels);
        shapes.add(pip);
        
        return shapes;
    }
    
    @Override
    public void dataAvailable(float[] data) {
        if(!resetting) {
        
            if(buffFramesCounter < getBufferSizeMultiplier()) {
            
                // Add a frame to the buffer
                System.arraycopy(data, 0, buffer, BUFFER_SIZE * buffFramesCounter, data.length);
                buffFramesCounter++;
            } else {
        
                // Perform forward FFT
                fft.forward(buffer);
        
                int minDbValue = propMinDbValue.getProp().get();
        
                // Update band values
                for (int i = 0; i < bandCount; i++) {
                    double bandDB = Utils.toDB(fft.getAvg(i), fft.timeSize());
                    bandDB = bandDB < minDbValue ? minDbValue : bandDB;
                    bandValuesDB[i] = bandDB;
                
                    // Reset buffer
                    buffFramesCounter = 0;
                }
            }
        }
    }
    
    private void startAnimation() {
        
        // Make sure the original timer is stopped when the view is reset
        if(mainAnimationTimeline != null) {
            mainAnimationTimeline.stop();
        }
        
        final int duration = 60 * getBufferSizeMultiplier();

        KeyFrame frame = new KeyFrame(Duration.millis(duration), new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                
                for (int i = 0; i < bandValuesDB.length; i++) {

                    Timeline tl = timelines[i];
                    tl.stop();
                    tl.getKeyFrames().clear();
                    KeyValue kv = new KeyValue(bandValues.get(i), bandValuesDB[i]);
                    KeyFrame kf = new KeyFrame(Duration.millis(duration), kv);
                    tl.getKeyFrames().add(kf);
                    tl.playFromStart();
                }
            }
        });
        
        mainAnimationTimeline = new Timeline(frame);
        mainAnimationTimeline.setCycleCount(Animation.INDEFINITE);
        mainAnimationTimeline.play();
    }
    
    @Override
    public void dataAvailable(float[] left, float[] right) {
        super.dataAvailable(left, right);
        
        // Update the sub view, which is the only purpose this override exists
        View view = subViews.get(propPipViewType.getProp().get());
        if(view.getRoot().isVisible()) {
            view.dataAvailable(left, right);
        }
    }

    @Override
    public void nextFrame() {
        
        int minDbValue = propMinDbValue.getProp().get();
        for (int i = 0; i < bandValuesDB.length; i++) {
            
            // Trail drop           
            trailValuesDB[i] = trailValuesDB[i] - trailOpValues[i];
            trailOpValues[i] = trailOpValues[i] * propTrailAccelerationFactor.getProp().get();
            
            Double bandValue = bandValues.get(i).getValue();
            if(bandValue > trailValuesDB[i]) {
                trailValuesDB[i] = bandValue;
                trailOpValues[i] = propTrailStayFactor.getProp().get();
            }
            if(trailValuesDB[i] < minDbValue) {
                trailValuesDB[i] = minDbValue;
            }
            trailValues.get(i).set(trailValuesDB[i]);
        }
        
        // Update the sub view
        View view = subViews.get(propPipViewType.getProp().get());
        if(view.getRoot().isVisible()) {
            view.nextFrame();
        }
    }
    
    private void createHzLineAndLabel(int barIdx, int hz) {
        
        // Create line
        Line line = new Line();
        line.startXProperty().bind(Bindings.createDoubleBinding(
                () -> {
                    double parentWidth = getRoot().widthProperty().get();
                    double barW = (parentWidth - parentWidth * SCENE_MARGIN_RATIO * 2) / bandCount;
                    return parentWidth * SCENE_MARGIN_RATIO + barIdx * barW;
                }, 
                getRoot().widthProperty()));
        line.endXProperty().bind(line.startXProperty());
        line.startYProperty().bind(getRoot().heightProperty().multiply(SCENE_MARGIN_RATIO));
        line.endYProperty().bind(getRoot().heightProperty().subtract(
                getRoot().heightProperty().multiply(SCENE_MARGIN_RATIO)));
        line.strokeProperty().bind(propGridColor.getProp());
        line.getStrokeDashArray().addAll(2d);
        line.setCache(true);
        vLines.add(line);
        
        // Create label
        Label label = UiUtils.createLabel(hz + "Hz", vLabels);
        label.layoutXProperty().bind(
                line.startXProperty().subtract(
                        label.widthProperty().divide(2)));
        label.layoutYProperty().bind(line.endYProperty().add(label.heightProperty().multiply(GRID_LABELS_MARGIN_RATIO)));
        label.textFillProperty().bind(propGridColor.getProp());
        label.styleProperty().bind(Bindings.concat(
                "-fx-font-size: ", Bindings.createDoubleBinding(
                        () -> (getRoot().widthProperty().get() * SCENE_MARGIN_RATIO) / LABEL_SIZE_RATIO,
                        getRoot().widthProperty())));
    }
    
    private void createDbGridLineAndLabel(int idx) {
        
        // Create line
        Line line = new Line();
        line.startXProperty().bind(getRoot().widthProperty().multiply(SCENE_MARGIN_RATIO));
        line.endXProperty().bind(getRoot().widthProperty().subtract(
                getRoot().widthProperty().multiply(SCENE_MARGIN_RATIO)));
        line.startYProperty().bind(
                Bindings.createDoubleBinding(() -> {
                    double dbVal = map(idx, 0, propDbLinesCount.getProp().get(), propMinDbValue.getProp().get(), 0);
                    double parentHeigth = getRoot().heightProperty().get();
                    return map(dbVal, propMinDbValue.getProp().get(), 0,  
                            parentHeigth - parentHeigth * SCENE_MARGIN_RATIO,
                            parentHeigth * SCENE_MARGIN_RATIO);
                }, 
                getRoot().heightProperty(), propMinDbValue.getProp(), propDbLinesCount.getProp()));
        line.endYProperty().bind(line.startYProperty());
        line.strokeProperty().bind(propGridColor.getProp());
        line.getStrokeDashArray().addAll(2d);
        line.setCache(true);
        hLines.add(line);
        
        // Create label
        Label label = UiUtils.createLabel("", hLabels);
        label.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    double dBVal = map(idx, 0, propDbLinesCount.getProp().get(), propMinDbValue.getProp().get(), 0);
                    return Math.round(dBVal) + "dB";
                }, propMinDbValue.getProp(), propDbLinesCount.getProp()));
        label.layoutXProperty().bind(
                line.startXProperty().subtract(
                        label.widthProperty().add(
                                label.widthProperty().multiply(GRID_LABELS_MARGIN_RATIO))));
        label.layoutYProperty().bind(line.startYProperty().subtract(label.heightProperty().divide(2)));
        label.textFillProperty().bind(propGridColor.getProp());
        label.styleProperty().bind(Bindings.concat(
                "-fx-font-size: ", Bindings.createDoubleBinding(
                        () -> (getRoot().widthProperty().get() * SCENE_MARGIN_RATIO) / LABEL_SIZE_RATIO,
                        getRoot().widthProperty())));
    }
    
    protected int getBufferSizeMultiplier() {
        return WindowSize.valueOf(propWindowSize.get()).getValue();
    }
    
    @Override
    protected void reset() {
        resetting = true;
        super.reset();
        resetting = false;
    }

    @Override
    public void onShow() {
    }

    @Override
    public void onHide() {
    }
}
