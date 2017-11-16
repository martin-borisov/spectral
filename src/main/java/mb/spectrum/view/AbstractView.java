package mb.spectrum.view;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public abstract class AbstractView implements View {
	
	private static final Color BACKGROUND_COLOR = Color.BLACK;
	protected static final double SCENE_MARGIN_RATIO = 0.05;
	
	protected Pane pane;
	
	public AbstractView() {
		pane = new Pane();
		initProperties();
		createScene();
	}
	
	protected abstract List<Node> collectNodes();

	@Override
	public Pane getRoot() {
		return pane;
	}
	
	protected void initProperties() {
	}
	
	private void createScene() {
		pane.setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, null, null)));
		pane.getChildren().addAll(collectNodes());
	}
	
	protected void reset() {
		
		// TODO: This is currently a bit of a hack to preserve the currently shown property,
		// but is bad design and should be replaced by a different solution
		Node property = null;
		for (Node node : pane.getChildren()) {
			if("Property Control".equals(node.getUserData())) {
				property = node;
				break;
			}
		}

		pane.getChildren().clear();
		pane.getChildren().addAll(collectNodes());
		
		if(property != null) {
			pane.getChildren().add(property);
		}
	}
}
