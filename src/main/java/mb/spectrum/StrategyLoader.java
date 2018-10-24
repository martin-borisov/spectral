package mb.spectrum;

public class StrategyLoader {
	
	private static final String STRATEGY_CLASS = 
			ConfigService.getInstance().getOrCreateProperty(
					"mb.strategy", "mb.spectrum.desktop.DesktopStrategy");
	private static StrategyLoader ref;
	private PlatformStrategy strategy;
	
	private StrategyLoader() {
		try {
			strategy = (PlatformStrategy) Class.forName(STRATEGY_CLASS).newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Loading of strategy class failed", e);
		}
	}
	
	public static StrategyLoader getInstance() {
		synchronized (StrategyLoader.class) {
			if(ref == null) {
				ref = new StrategyLoader();
			}
		}
		return ref;
	}

	public PlatformStrategy getStrategy() {
		return strategy;
	}
}
