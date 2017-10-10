package mb.spectrum.view;

import java.util.Collections;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public abstract class AbstractView implements View {
	
	private static final int SCENE_MARGIN_PX = 45;
	private static final Color BACKGROUND_COLOR = Color.BLACK;
	
	private GraphLayoutWrapper sw;
	
	public AbstractView() {
		sw = new GraphLayoutWrapper(SCENE_MARGIN_PX);
		createScene();
		setupScene();
	}

	@Override
	public Pane getRoot() {
		return sw.getPane();
	}
	
	private void createScene() {
		
        //Scene scene = new Scene(new Group(), 
        //		INIT_SCENE_WIDTH, INIT_SCENE_HEIGHT, false, SceneAntialiasing.DISABLED);
		
		Pane pane = sw.getPane();
		pane.setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, null, null)));
		pane.widthProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> observable, 
					Number oldValue, Number newValue) {
				onSceneWidthChange(oldValue, newValue);
			}
		});
		pane.heightProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> observable, 
					Number oldValue, Number newValue) {
				onSceneHeightChange(oldValue, newValue);
			}
		});
	}
	
	private void setupScene() {
		sw.getPane().getChildren().addAll(collectNodes());
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

	protected double areaWidth() {
		return sw.getLayoutWidth();
	}

	protected double areaHeight() {
		return sw.getLayoutHeight();
	}
}
