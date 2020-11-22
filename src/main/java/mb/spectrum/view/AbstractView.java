package mb.spectrum.view;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Convenience class that should be extended by all concrete views.
 */
public abstract class AbstractView implements View {
	
	private static final Color BACKGROUND_COLOR = Color.BLACK;
	protected static final double SCENE_MARGIN_RATIO = 0.05;
	
	protected Pane pane;
	
	/**
	 * Performs typical initialization of the view by calling {@link #initProperties() and {@link #collectNodes()}.
	 * The former should be implemented by the subclass in case it exposes configuration properties, while the latter must
	 * be implemented.
	 */
	public AbstractView() {
		init();
	}
	
	/**
	 * Allows skipping initialization with the purpose of passing aeguments to the constructor of the subclass.
	 * The subclass is responsible for calling {@link #init()}.
	 * @param skipInit Skips initialization if <code>true</code>
	 */
	public AbstractView(boolean skipInit) {
		if(!skipInit) {
			init();
		}
	}
	
	/**
	 * Initializes the view before it can be shown.
	 */
	protected void init() {
		pane = new Pane();
		initProperties();
		createScene();
	}
	
	/**
	 * Returns a collection of the view's nodes.
	 * @return Collection of the view's nodes
	 */
	protected abstract List<Node> collectNodes();

	@Override
	public Pane getRoot() {
		return pane;
	}
	
	/**
	 * Initializes exposed configuration properties.
	 */
	protected void initProperties() {
	}
	
	/**
	 * Creates the scene by invoking {@link #collectNodes()}
	 */
	private void createScene() {
		pane.setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, null, null)));
		pane.getChildren().addAll(collectNodes());
	}
	
	/**
	 * Called when the view should be reset, i.e. when there are drastic changes in the node structure of the view.
	 */
	protected void reset() {
		pane.getChildren().clear();
		pane.getChildren().addAll(collectNodes());
	}

	@Override
	public void onShow() {
	}

	@Override
	public void onHide() {
	}
}
