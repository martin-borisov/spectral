package mb.spectrum.view;

import java.util.Collections;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Color;

public abstract class AbstractView implements View {
	
	private static final int INIT_SCENE_WIDTH = 800;
	private static final int INIT_SCENE_HEIGHT = 600;
	private static final int SCENE_MARGIN = 45;
	
	private GraphSceneWrapper sw;
	
	public AbstractView() {
		createScene();
		setupScene();
	}

	@Override
	public Scene getScene() {
		return sw.getScene();
	}
	
	private void createScene() {
        Scene scene = new Scene(new Group(), 
        		INIT_SCENE_WIDTH, INIT_SCENE_HEIGHT, false, SceneAntialiasing.DISABLED);
        scene.setFill(Color.BLACK);
        scene.widthProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> observable, 
					Number oldValue, Number newValue) {
				onSceneWidthChange(oldValue, newValue);
			}
		});
        scene.heightProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> observable, 
					Number oldValue, Number newValue) {
				onSceneHeightChange(oldValue, newValue);
			}
		});
        sw = new GraphSceneWrapper(scene, SCENE_MARGIN);
	}
	
	private void setupScene() {
		((Group) sw.getScene().getRoot()).getChildren().addAll(collectNodes());
	}
	
	protected void onSceneWidthChange(Number oldValue, Number newValue) {
	}
	
	protected void onSceneHeightChange(Number oldValue, Number newValue) {
	}
	
	protected List<Node> collectNodes() {
		return Collections.emptyList();
	}
	
	protected double coordX(double x) {
		return sw.coordX(x);
	}

	protected double coordY(double y) {
		return sw.coordY(y);
	}

	protected double sceneWidth() {
		return sw.getSceneWidth();
	}

	protected double sceneHeight() {
		return sw.getSceneHeight();
	}
}
