package mb.spectrum.view;

import static mb.spectrum.UiUtils.createConfigurableBooleanProperty;
import static mb.spectrum.UiUtils.createConfigurableColorProperty;
import static mb.spectrum.UiUtils.createConfigurableDoubleProperty;
import static mb.spectrum.UiUtils.createConfigurableIntegerProperty;
import static mb.spectrum.Utils.peakLevel;
import static mb.spectrum.Utils.rmsLevel;

import java.util.Arrays;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
	private ConfigurableIntegerProperty propClipDbValue;
	private ConfigurableIntegerProperty propMidDbValue;
	private ConfigurableDoubleProperty propLedGapRatio;
	private ConfigurableIntegerProperty propCameraDistance;
	private ConfigurableIntegerProperty propCameraVPan;
	private ConfigurableIntegerProperty propCameraHPan;
	private ConfigurableIntegerProperty propCameraXRotate;
	private ConfigurableIntegerProperty propCameraYRotate;
	private ConfigurableIntegerProperty propCameraZRotate;
	private ConfigurableColorProperty propLedColorNormal;
	private ConfigurableColorProperty propLedColorMid;
	private ConfigurableColorProperty propLedColorClip;
	private ConfigurableBooleanProperty propAutoRotateCamera;
	private ConfigurableBooleanProperty propLineMode;
	private ConfigurableBooleanProperty propRms;
	
	/* Operational Properties */
	private DoubleProperty currentDbLProp, currentDbRProp;
	
	private double currentDbL, currentDbR;
	
	@Override
	public String getName() {
		return "Stereo LED 3D Meters";
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Arrays.asList(
				propMinDbValue,
				propClipDbValue,
				propMidDbValue,
				propLedCount,
				propLedGapRatio,
				propCameraDistance,
				propCameraVPan,
				propCameraHPan,
				propCameraXRotate,
				propCameraYRotate,
				propCameraZRotate,
				propLedColorNormal,
				propLedColorMid,
				propLedColorClip,
				propAutoRotateCamera,
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
		propLedCount.getProp().addListener((obs, oldVal, newVal) -> {
			if(newVal != oldVal) {
				reset();
			}
		});
		
		// Not requiring reset
		propMinDbValue = createConfigurableIntegerProperty(
				keyPrefix + "minDbValue", "Min. dB Value", -100, -10, -60, 1);
		propClipDbValue = createConfigurableIntegerProperty(
				keyPrefix + "clipDbValue", "Clip dB Value", -100, 0, -15, 1);
		propMidDbValue = createConfigurableIntegerProperty(
				keyPrefix + "midDbValue", "Middle dB Value", -100, 0, -20, 1);
		propLedGapRatio = createConfigurableDoubleProperty(
				keyPrefix + "ledGapRatio", "LED Gap Ratio", 0.01, 0.5, 0.2, 0.01);
		propCameraDistance = createConfigurableIntegerProperty(
				keyPrefix + "cameraDistance", "Camera Distance", 0, 1000, 200, 10);
		propCameraVPan = createConfigurableIntegerProperty(
				keyPrefix + "cameraVPan", "Camera Vert. Pan", -200, 200, -10, 1);
		propCameraHPan = createConfigurableIntegerProperty(
				keyPrefix + "cameraHPan", "Camera Hor. Pan", -200, 200, -44, 1);
		propCameraXRotate = createConfigurableIntegerProperty(
				keyPrefix + "cameraXRotate", "Camera X Rotate", 0, 360, 325, 5);
		propCameraYRotate = createConfigurableIntegerProperty(
				keyPrefix + "cameraYRotate", "Camera Y Rotate", 0, 360, 335, 5);
		propCameraZRotate = createConfigurableIntegerProperty(
				keyPrefix + "cameraZRotate", "Camera Z Rotate", 0, 360, 270, 5);
		propLedColorNormal = createConfigurableColorProperty(
				keyPrefix + "normalLevelColor", "Normal Level Color", Color.LAWNGREEN);
		propLedColorMid = createConfigurableColorProperty(
				keyPrefix + "middleLevelColor", "Middle Level Color", Color.YELLOW);
		propLedColorClip = createConfigurableColorProperty(
				keyPrefix + "clipLevelColor", "Clip Level Color", Color.RED);
		propAutoRotateCamera = createConfigurableBooleanProperty(
				keyPrefix + "autoRotateCamera", "Auto Rotate Camera", false);
		propLineMode = createConfigurableBooleanProperty(
				keyPrefix + "lineMode", "Line Mode", false);
		propRms = createConfigurableBooleanProperty(
				keyPrefix + "enableRmsMode", "RMS Mode", false);
		
		/* Operational properties */
		currentDbLProp = new SimpleDoubleProperty(propMinDbValue.getProp().get());
		currentDbRProp = new SimpleDoubleProperty(propMinDbValue.getProp().get());
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
	}
	
	@Override
	protected List<Node> collectNodes() {
		
		Group root = new Group();
		
		for (int i = 0; i < propLedCount.getProp().get(); i++) {
			root.getChildren().add(createLed(Channel.LEFT, i));
			root.getChildren().add(createLed(Channel.RIGHT, i));
		}
		
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
		box.setLayoutX(channel == Channel.LEFT ? 0 - width - width / 2 : width / 2);
		
		box.heightProperty().bind(Bindings.createDoubleBinding(
				() -> {
					int ledCount = propLedCount.getProp().get();
					double ledGapRatio = propLedGapRatio.getProp().get();
					double ledGap = (BAR_HEIGHT / ledCount) * ledGapRatio;
					return BAR_HEIGHT / ledCount - ledGap - ledGap / ledCount;
				}, propLedCount.getProp(), propLedGapRatio.getProp()));
		
		box.layoutYProperty().bind(Bindings.createDoubleBinding(
				() -> {
					int ledCount = propLedCount.getProp().get();
					double ledGapRatio = propLedGapRatio.getProp().get();
					double ledGap = (BAR_HEIGHT / ledCount) * ledGapRatio;
					return ledGap + idx * (ledGap + box.heightProperty().get()) * -1;
				}, propLedCount.getProp(), propLedGapRatio.getProp(), box.heightProperty()));
		
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
        Rotate yRotate = new Rotate(0, Rotate.Y_AXIS);
        yRotate.angleProperty().bind(Bindings.createDoubleBinding(
        		() -> { 
        			return Double.valueOf(propCameraYRotate.getProp().get()); 
        			}, propCameraYRotate.getProp()));
        
        Rotate xRotate = new Rotate(0, Rotate.X_AXIS);
        xRotate.angleProperty().bind(Bindings.createDoubleBinding(
        		() -> { 
        			return Double.valueOf(propCameraXRotate.getProp().get()); 
        			}, propCameraXRotate.getProp()));
        
        Rotate zRotate = new Rotate(0, Rotate.Z_AXIS);
        zRotate.angleProperty().bind(Bindings.createDoubleBinding(
        		() -> { 
        			return Double.valueOf(propCameraZRotate.getProp().get()); 
        			}, propCameraZRotate.getProp()));
        
        Rotate xRotateAuto = new Rotate(0, Rotate.Y_AXIS);
		
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000);
        camera.getTransforms().addAll(
                pivot,
                yRotate,
                xRotate,
                zRotate,
                position,
                xRotateAuto
        );
        
        
        Timeline timeline = new Timeline(
//                new KeyFrame(
//                        Duration.seconds(0), 
//                        new KeyValue(yRotate.angleProperty(), 0)
//                ),
//                new KeyFrame(
//                        Duration.seconds(15), 
//                        new KeyValue(yRotate.angleProperty(), 360)
//                ),
                new KeyFrame(
                        Duration.seconds(0), 
                        new KeyValue(xRotateAuto.angleProperty(), 0)
                ),
                new KeyFrame(
                        Duration.seconds(15), 
                        new KeyValue(xRotateAuto.angleProperty(), 360)
                )
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        //timeline.play();
        
        propAutoRotateCamera.getProp().addListener((obs, newVal, oldVal) -> {
        	if(newVal) {
        		timeline.play();
        	} else {
        		timeline.stop();
        	}
        }); 
        
        return camera;
	}
}
