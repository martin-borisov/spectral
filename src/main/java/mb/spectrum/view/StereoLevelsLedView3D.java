package mb.spectrum.view;

import static mb.spectrum.UiUtils.createConfigurableBooleanProperty;
import static mb.spectrum.UiUtils.createConfigurableColorProperty;
import static mb.spectrum.UiUtils.createConfigurableDoubleProperty;
import static mb.spectrum.UiUtils.createConfigurableIntegerProperty;
import static mb.spectrum.Utils.peakLevel;
import static mb.spectrum.Utils.rmsLevel;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point3D;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import mb.spectrum.Utils;
import mb.spectrum.Utils3D;
import mb.spectrum.prop.ConfigurableBooleanProperty;
import mb.spectrum.prop.ConfigurableColorProperty;
import mb.spectrum.prop.ConfigurableDoubleProperty;
import mb.spectrum.prop.ConfigurableIntegerProperty;
import mb.spectrum.prop.ConfigurableProperty;
import mb.spectrum.view.StereoLevelsLedView.Channel;

public class StereoLevelsLedView3D extends AbstractView {
	
	private static final double BAR_HEIGHT = 100;
	
	/* Configuration Properties */
	
	// Requiring reset
	private ConfigurableProperty<Integer> propLedCount;
	
	// Not requiring reset
	private ConfigurableIntegerProperty propMinDbValue;
	private ConfigurableIntegerProperty propMidDbValue;
	private ConfigurableIntegerProperty propClipDbValue;
	private ConfigurableDoubleProperty propLedGapRatio;
	private ConfigurableColorProperty propLedColorNormal;
	private ConfigurableColorProperty propLedColorMid;
	private ConfigurableColorProperty propLedColorClip;
	private ConfigurableIntegerProperty propCameraDistance;
	private ConfigurableIntegerProperty propCameraVPan;
	private ConfigurableIntegerProperty propCameraHPan;
	private ConfigurableIntegerProperty propXRotate;
	private ConfigurableIntegerProperty propYRotate;
	private ConfigurableIntegerProperty propZRotate;
	private ConfigurableBooleanProperty propCameraAutoRotateX;
	private ConfigurableBooleanProperty propCameraAutoRotateY;
	private ConfigurableBooleanProperty propCameraAutoRotateZ;
	private ConfigurableDoubleProperty propAutoRotateSpeed;
	private ConfigurableBooleanProperty propLineMode;
	private ConfigurableBooleanProperty propRms;
	
	/* Operational Properties */
	private DoubleProperty currentDbLProp, currentDbRProp;
	private BooleanProperty effectTriggerProp;
	
	private double currentDbL, currentDbR;
	private Random random = new Random();
	private int effectTriggerCounter;
	private Group root;
	
	@Override
	public String getName() {
		return "Stereo LED 3D Meters";
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Arrays.asList(
				propMinDbValue,
				propMidDbValue,
				propClipDbValue,
				propLedCount,
				propLedGapRatio,
				propLedColorNormal,
				propLedColorMid,
				propLedColorClip,
				propCameraDistance,
				propCameraVPan,
				propCameraHPan,
				propXRotate,
				propYRotate,
				propZRotate,
				propCameraAutoRotateX,
				propCameraAutoRotateY,
				propCameraAutoRotateZ,
				propAutoRotateSpeed,
				propLineMode,
				propRms
				);
	}

	@Override
	protected void initProperties() {
		
		final String keyPrefix = "stereoLevelsLedView3D.";
		
		/* Configuration Properties */
		
		// Requiring reset
		propLedCount = createConfigurableIntegerProperty(
				keyPrefix + "ledCount", "Led Count", 2, 1000, 18, 1);
		propLedCount.addUpdateFinishedListener((obs, oldVal, newVal) -> {
			if(newVal != oldVal) {
				reset();
			}
		});
		
		// Not requiring reset
		propMinDbValue = createConfigurableIntegerProperty(
				keyPrefix + "minDbValue", "Min. dB Value", -100, -10, -60, 1, "dB");
		propMidDbValue = createConfigurableIntegerProperty(
				keyPrefix + "midDbValue", "Middle dB Value", -100, 0, -20, 1, "dB");
		propClipDbValue = createConfigurableIntegerProperty(
				keyPrefix + "clipDbValue", "Clip dB Value", -100, 0, -15, 1, "dB");
		propLedGapRatio = createConfigurableDoubleProperty(
				keyPrefix + "ledGapRatio", "LED Gap Ratio", 0.01, 0.5, 0.2, 0.01);
		propLedColorNormal = createConfigurableColorProperty(
				keyPrefix + "normalLevelColor", "Normal Level Color", Color.LAWNGREEN);
		propLedColorMid = createConfigurableColorProperty(
				keyPrefix + "middleLevelColor", "Middle Level Color", Color.YELLOW);
		propLedColorClip = createConfigurableColorProperty(
				keyPrefix + "clipLevelColor", "Clip Level Color", Color.RED);
		propCameraDistance = createConfigurableIntegerProperty(
				keyPrefix + "cameraDistance", "Camera Distance", 0, 1000, 200, 10);
		propCameraVPan = createConfigurableIntegerProperty(
				keyPrefix + "cameraVPan", "Camera Vert. Pan", -200, 200, -10, 1);
		propCameraHPan = createConfigurableIntegerProperty(
				keyPrefix + "cameraHPan", "Camera Hor. Pan", -200, 200, -44, 1);
		propXRotate = createConfigurableIntegerProperty(
				keyPrefix + "xRotate", "Rotate X", 0, 360, 325, 5);
		propYRotate = createConfigurableIntegerProperty(
				keyPrefix + "yRotate", "Rotate Y", 0, 360, 335, 5);
		propZRotate = createConfigurableIntegerProperty(
				keyPrefix + "zRotate", "Rotate Z", 0, 360, 270, 5);
		propCameraAutoRotateX = createConfigurableBooleanProperty(
				keyPrefix + "cameraAutoRotateX", "Auto Rotate Camera X", false);
		propCameraAutoRotateY = createConfigurableBooleanProperty(
				keyPrefix + "cameraAutoRotateY", "Auto Rotate Camera Y", false);
		propCameraAutoRotateZ = createConfigurableBooleanProperty(
				keyPrefix + "cameraAutoRotateZ", "Auto Rotate Camera Z", false);
		propAutoRotateSpeed = createConfigurableDoubleProperty(
				keyPrefix + "autoRotateSpeed", "Auto Rotate Speed", 0.1, 10.0, 1.0, 0.1);
		propLineMode = createConfigurableBooleanProperty(
				keyPrefix + "lineMode", "Line Mode", false);
		propRms = createConfigurableBooleanProperty(
				keyPrefix + "enableRmsMode", "RMS Mode", false);
		
		/* Operational properties */
		currentDbLProp = new SimpleDoubleProperty(propMinDbValue.getProp().get());
		currentDbRProp = new SimpleDoubleProperty(propMinDbValue.getProp().get());
		effectTriggerProp = new SimpleBooleanProperty(false);
		
		// Trigger effect
		effectTriggerProp.addListener((obs, oldVal, newVal) -> {
			if(newVal) {
				onTriggerEffect();
				effectTriggerProp.set(false);
			}
		});
	}

	@Override
	public void nextFrame() {
		
		// Update operational properties from UI thread
		currentDbLProp.set(currentDbL);
		currentDbRProp.set(currentDbR);
	}

	@Override
	public void dataAvailable(float[] left, float[] right) {
		
		float levelLeft = 0, levelRight = 0;
		if(propRms.getProp().get()) {
			levelLeft = rmsLevel(left);
			levelRight = rmsLevel(right);
		} else {
			levelLeft = peakLevel(left);
			levelRight = peakLevel(right);
		}
		
		currentDbL = Utils.toDB(levelLeft);
		currentDbR = Utils.toDB(levelRight);
		
		// Update effect trigger counter and trigger the effect
		if(effectTriggerCounter == 0) {
			effectTriggerProp.set(true);
			effectTriggerCounter =  random.nextInt((100 - 1) + 1) + 1;
		}
		effectTriggerCounter--;
	}
	
	@Override
	protected List<Node> collectNodes() {
		
		root = new Group();
		
		// Display the x, y and z axes if the debug property is set
		if(Boolean.getBoolean("spectrumDebug")) {
			root.getChildren().add(Utils3D.createLine(new Point3D(-100, 0, 0), new Point3D(100, 0, 0), Color.RED));
			root.getChildren().add(Utils3D.createLine(new Point3D(0, -100, 0), new Point3D(0, 100, 0), Color.BLUE));
			root.getChildren().add(Utils3D.createLine(new Point3D(0, 0, -100), new Point3D(0, 0, 100), Color.GREEN));
		}
		
		// Collect all "leds" and add them to a group
		for (int i = 0; i < propLedCount.getProp().get(); i++) {
			root.getChildren().add(createLed(Channel.LEFT, i));
			root.getChildren().add(createLed(Channel.RIGHT, i));
		}
		
		// Make rotation configurable
        Rotate yRotate = new Rotate(0, Rotate.Y_AXIS);
        yRotate.angleProperty().bind(Bindings.createDoubleBinding(
        		() -> { 
        			return Double.valueOf(propYRotate.getProp().get()); 
        			}, propYRotate.getProp()));
        
        Rotate xRotate = new Rotate(0, Rotate.X_AXIS);
        xRotate.angleProperty().bind(Bindings.createDoubleBinding(
        		() -> { 
        			return Double.valueOf(propXRotate.getProp().get()); 
        			}, propXRotate.getProp()));
        
        Rotate zRotate = new Rotate(0, Rotate.Z_AXIS);
        zRotate.angleProperty().bind(Bindings.createDoubleBinding(
        		() -> { 
        			return Double.valueOf(propZRotate.getProp().get()); 
        			}, propZRotate.getProp()));
		root.getTransforms().addAll(xRotate, yRotate, zRotate);
		
		// Create sub-scene for 3d content
		SubScene scene = new SubScene(root, pane.getWidth(), pane.getHeight(), 
				true, SceneAntialiasing.BALANCED);
		scene.widthProperty().bind(pane.widthProperty());
		scene.heightProperty().bind(pane.heightProperty());
		scene.setCamera(createCamera());
		return Arrays.asList(scene);
	}
	
	private Node createLed(Channel channel, int idx) {
		
		final double width = 20;
		
		Box box = new Box();
		box.setWidth(width);
		box.setDepth(width);
		box.setTranslateX(channel == Channel.LEFT ? (width) * -1 : width);
		
		// Maintain dimensions when updating the led count and gap
		box.heightProperty().bind(Bindings.createDoubleBinding(
				() -> {
					int ledCount = propLedCount.getProp().get();
					double ledGapRatio = propLedGapRatio.getProp().get();
					double ledGap = (BAR_HEIGHT / ledCount) * ledGapRatio;
					double ledGapTotal = (ledCount - 1) * ledGap;
					return (BAR_HEIGHT - ledGapTotal) / ledCount;
				}, propLedCount.getProp(), propLedGapRatio.getProp()));
		
		box.layoutYProperty().bind(Bindings.createDoubleBinding(
				() -> {
					int ledCount = propLedCount.getProp().get();
					double ledGapRatio = propLedGapRatio.getProp().get();
					double ledGap = (BAR_HEIGHT / ledCount) * ledGapRatio;
					return (idx * (box.heightProperty().get() + ledGap) + box.heightProperty().get() / 2) * -1;
				}, propLedCount.getProp(), propLedGapRatio.getProp(), box.heightProperty()));
		
		// Make colors configurable
		PhongMaterial off = new PhongMaterial(Color.rgb(30, 30, 30));
		PhongMaterial onNorm = new PhongMaterial();
		onNorm.diffuseColorProperty().bind(propLedColorNormal.getProp());
		PhongMaterial onMid = new PhongMaterial();
		onMid.diffuseColorProperty().bind(propLedColorMid.getProp());
		PhongMaterial onClip = new PhongMaterial();
		onClip.diffuseColorProperty().bind(propLedColorClip.getProp());
		
		DoubleProperty currentDbProp = 
				Channel.LEFT.equals(channel) ? currentDbLProp : currentDbRProp;
		box.materialProperty().bind(Bindings.createObjectBinding(
				() -> {
					double range = Math.abs(propMinDbValue.getProp().get() / propLedCount.getProp().get());
					double min = propMinDbValue.getProp().get() + range * idx;
					
					Material mat;
					if(currentDbProp.get() > min) {
						if(min > propClipDbValue.getProp().get()) {
							mat = onClip;
						} else if(min > propMidDbValue.getProp().get()) {
							mat = onMid;
						} else {
							mat = onNorm;
						}
					} else {
						mat = off;
					}
					
					return mat;
				}, propMinDbValue.getProp(), propClipDbValue.getProp(), propMidDbValue.getProp(), 
				propLedCount.getProp(), currentDbProp));
		
		// Make line mode configurable
		box.drawModeProperty().bind(Bindings.createObjectBinding(
				() -> {
					return propLineMode.getProp().get() ? DrawMode.LINE : DrawMode.FILL;
				}, propLineMode.getProp()));
		
		return box;
	}
	
	private Camera createCamera() {
        
        Translate position = new Translate();
        position.zProperty().bind(Bindings.createDoubleBinding(
        		() -> { 
        			return propCameraDistance.getProp().get() * -1.0; 
        			}, propCameraDistance.getProp()));
        position.yProperty().bind(Bindings.createDoubleBinding(
        		() -> { 
        			return Double.valueOf(propCameraVPan.getProp().get()); 
        			}, propCameraVPan.getProp()));
        position.xProperty().bind(Bindings.createDoubleBinding(
        		() -> { 
        			return propCameraHPan.getProp().get() * -1.0; 
        			}, propCameraHPan.getProp()));
        
		Translate pivot = new Translate();
        
        Rotate xRotateAuto = new Rotate(0, Rotate.X_AXIS);
        Rotate yRotateAuto = new Rotate(0, Rotate.Y_AXIS);
        Rotate zRotateAuto = new Rotate(0, Rotate.Z_AXIS);
		
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000);
        
        Timeline timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(0), 
                        new KeyValue(yRotateAuto.angleProperty(), 0)
                ),
                new KeyFrame(
                        Duration.seconds(15), 
                        new KeyValue(yRotateAuto.angleProperty(), 360)
                ),
                new KeyFrame(
                        Duration.seconds(0), 
                        new KeyValue(xRotateAuto.angleProperty(), 0)
                ),
                new KeyFrame(
                        Duration.seconds(15), 
                        new KeyValue(xRotateAuto.angleProperty(), 360)
                ),
                new KeyFrame(
                        Duration.seconds(0), 
                        new KeyValue(zRotateAuto.angleProperty(), 0)
                ),
                new KeyFrame(
                        Duration.seconds(15), 
                        new KeyValue(zRotateAuto.angleProperty(), 360)
                )
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        
        // Make the auto-rotation speed configurable
        timeline.rateProperty().bind(propAutoRotateSpeed.getProp());
        
        // Define what happens when the play toggle is switched
        ChangeListener<Boolean> playStopChangeListener = new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
	        	if(shouldCameraRotate()) {
	        		camera.getTransforms().clear();
	        		camera.getTransforms().add(pivot);
	        		
	        		if(propCameraAutoRotateX.getProp().get()) {
	        			camera.getTransforms().add(xRotateAuto);
	        		} else {
	        			camera.getTransforms().remove(xRotateAuto);
	        		}
	        		
	        		if(propCameraAutoRotateY.getProp().get()) {
	        			camera.getTransforms().add(yRotateAuto);
	        		} else {
	        			camera.getTransforms().remove(yRotateAuto);
	        		}
	        		
	        		if(propCameraAutoRotateZ.getProp().get()) {
	        			camera.getTransforms().add(zRotateAuto);
	        		} else {
	        			camera.getTransforms().remove(zRotateAuto);
	        		}
	        		
	        		camera.getTransforms().add(position);
	        		
	        		timeline.play();
	        	} else {
	        		timeline.stop();
	        		camera.getTransforms().clear();
	        		camera.getTransforms().addAll(pivot, position);
	        	}
			}
        };
        
        propCameraAutoRotateX.getProp().addListener(playStopChangeListener);
        propCameraAutoRotateY.getProp().addListener(playStopChangeListener); 
        propCameraAutoRotateZ.getProp().addListener(playStopChangeListener);
        
        // Initialize the camera play/stop state
        playStopChangeListener.changed(null, null, null);
        
        return camera;
	}
	
	private boolean shouldCameraRotate() {
		return propCameraAutoRotateX.getProp().get() 
				|| propCameraAutoRotateY.getProp().get()
				|| propCameraAutoRotateZ.getProp().get();
	}
	
	private void onTriggerEffect() {
		// TODO Implement random effects like flashes or color changes
	}
}
