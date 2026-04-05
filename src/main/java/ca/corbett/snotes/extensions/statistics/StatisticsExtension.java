package ca.corbett.snotes.extensions.statistics;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.gradient.ColorSelectionType;
import ca.corbett.extras.progress.MultiProgressDialog;
import ca.corbett.extras.progress.SimpleProgressAdapter;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.ColorProperty;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.Resources;
import ca.corbett.snotes.extensions.SnotesExtension;
import ca.corbett.snotes.ui.MainWindow;
import ca.corbett.snotes.ui.actions.ActionGroup;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * An optional built-in extension for Snotes that can gather and display
 * statistics about the user's notes.
 * <ul>
 *     <li>Word frequency - what are the most frequently used words?</li>
 *     <li>Phrase frequency - what are the most frequently used phrases?</li>
 *     <li>Note length distribution - how long are the user's notes over time?</li>
 * </ul>
 * <p>
 *     All statistics above can be gathered either for a specific time period,
 *     or from all notes. Word and phrase frequency can be configured to ignore
 *     common words ("a", "the", "an", etc.)
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class StatisticsExtension extends SnotesExtension {

    private static final String extInfoLocation = "/ca/corbett/snotes/extensions/statistics/extInfo.json";
    private final AppExtensionInfo extInfo;

    private static final String COLD_PROP = "Statistics.Options.coldColor";
    private static final String HOT_PROP = "Statistics.Options.hotColor";
    private static final Color DEFAULT_COLD = new Color(48, 48, 212);
    private static final Color DEFAULT_HOT = new Color(212, 48, 96);

    /**
     * We must supply a no-arg constructor to be invoked by ExtensionManager.
     * We can load jar resources only during construction or in loadJarResources();
     * after loadJarResources() completes, our ClassLoader is closed.
     */
    public StatisticsExtension() {
        extInfo = AppExtensionInfo.fromExtensionJar(getClass(), extInfoLocation);
        if (extInfo == null) {
            throw new RuntimeException("StatisticsExtension: can't parse extInfo.json!");
        }
    }

    /**
     * We must return a valid AppExtensionInfo instance that describes our extension.
     * The parent application will query for this when we are loaded, and the information
     * we return here will be presented to the user in the ExtensionManagerDialog.
     */
    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    /**
     * We can optionally return configuration properties to be merged into the parent
     * application's configuration properties.
     */
    @Override
    protected List<AbstractProperty> createConfigProperties() {
        List<AbstractProperty> props = new ArrayList<>();
        props.add(new ColorProperty(COLD_PROP, "Cold color", ColorSelectionType.SOLID)
                          .setSolidColor(DEFAULT_COLD)
                          .setHelpText("Represents the 'cold' end of the data spectrum (lower values)."));
        props.add(new ColorProperty(HOT_PROP, "Hot color", ColorSelectionType.SOLID)
                          .setSolidColor(DEFAULT_HOT)
                          .setHelpText("Represents the 'hot' end of the data spectrum (higher values)."));
        return props;
    }

    /**
     * Returns our currently-configured "cold" color, which is used to represent the lower end of the data spectrum.
     */
    public static Color getColdColor() {
        AbstractProperty prop = AppConfig.getInstance().getPropertiesManager().getProperty(COLD_PROP);
        if (prop instanceof ColorProperty colorProp) {
            return colorProp.getSolidColor();
        }
        return DEFAULT_COLD;
    }

    /**
     * Returns our currently-configured "hot" color, which is used to represent the higher end of the data spectrum.
     */
    public static Color getHotColor() {
        AbstractProperty prop = AppConfig.getInstance().getPropertiesManager().getProperty(HOT_PROP);
        if (prop instanceof ColorProperty colorProp) {
            return colorProp.getSolidColor();
        }
        return DEFAULT_HOT;
    }

    /**
     * This will be invoked exactly once by the ExtensionManager when we are
     * first instantiated. It is our only opportunity to load resources from our jar file
     * (other than in our constructor), before our ClassLoader is closed.
     */
    @Override
    protected void loadJarResources() {
        // Currently none
    }

    /**
     * We will supply one action group for the main action panel.
     * At a minimum, we will expose our StatisticsDialog (once we have one).
     */
    @Override
    public List<ActionGroup> getActionGroups() {
        ActionGroup group = new ActionGroup("Statistics", Resources.getIconStats());
        group.addAction(new StatsDialogAction());
        return List.of(group);
    }

    /**
     * A very simple action to launch our StatisticsDialog.
     */
    private static class StatsDialogAction extends EnhancedAction {

        public StatsDialogAction() {
            super("Statistics dialog");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            StatisticsLoaderThread loader = new StatisticsLoaderThread(MainWindow.getInstance().getDataManager());
            loader.addProgressListener(new ProgressListener(loader));
            String message = "Gathering statistics";
            MultiProgressDialog dialog = new MultiProgressDialog(MainWindow.getInstance(), message);
            dialog.runWorker(loader, true);

        }
    }

    /**
     * Listeners to our loader thread, and will show the StatisticsDialog when loading is complete.
     * Our progress dialog will be visible while the loader is running, and the user will have
     * a "cancel" button they can use if the loader runs too long. If we detect a cancel event,
     * we simply do nothing here. User can try again later.
     */
    private static class ProgressListener extends SimpleProgressAdapter {

        private final StatisticsLoaderThread loader;

        public ProgressListener(StatisticsLoaderThread loader) {
            this.loader = loader;
        }

        @Override
        public void progressCanceled() {
            // User canceled; do nothing.
        }

        @Override
        public void progressComplete() {
            new StatisticsDialog(loader.getStatistics()).setVisible(true);
        }
    }
}
