package ca.corbett.snotes.extensions.statistics;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;
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
import java.util.logging.Logger;

/**
 * An optional built-in extension for Snotes that can gather and display
 * statistics about the user's notes.
 * <ul>
 *     <li>Word frequency - what are the most frequently used words?</li>
 *     <li>Phrase frequency - what are the most frequently used phrases?</li>
 *     <li>Note length distribution - how long are the user's notes over time?</li>
 * </ul>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class StatisticsExtension extends SnotesExtension {

    private static final Logger log = Logger.getLogger(StatisticsExtension.class.getName());
    private static final String extInfoLocation = "/ca/corbett/snotes/extensions/statistics/extInfo.json";
    private final AppExtensionInfo extInfo;

    private static final String COLD_PROP = "Statistics.Options.coldColor";
    private static final String HOT_PROP = "Statistics.Options.hotColor";
    private static final Color DEFAULT_COLD = new Color(48, 48, 212);
    private static final Color DEFAULT_HOT = new Color(212, 48, 96);

    private MessageUtil messageUtil;
    private static final Object lockObject = new Object();
    private static volatile boolean loading;
    private static Statistics statistics;

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
     * Returns true if a StatisticsLoaderThread is currently running and loading statistics data, false otherwise.
     */
    public static boolean isLoading() {
        return loading;
    }

    /**
     * Invoked when loading of statistics is completed - we will accept the new
     * object and mark ourselves as no longer loading. If showDialogOnComplete
     * is true, we will show the StatisticsDialog immediately (assuming the
     * given Statistics instance is not null).
     */
    public static void setStatistics(Statistics stats, boolean showDialogOnComplete) {
        synchronized(lockObject) {
            statistics = stats;
            loading = false;
        }
        if (showDialogOnComplete && stats != null) {
            new StatisticsDialog(stats).setVisible(true);
        }
    }

    /**
     * Returns our currently-loaded statistics data, or null if we haven't loaded any yet.
     */
    public static Statistics getStatistics() {
        synchronized(lockObject) {
            return statistics;
        }
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
     * This exposes actions for launching the StatisticsDialog,
     * and for refreshing the cached statistics data.
     */
    @Override
    public List<ActionGroup> getActionGroups() {
        ActionGroup group = new ActionGroup("Statistics", Resources.getIconStats());
        group.addAction(new StatsDialogAction());
        group.addAction(new StatsRefreshAction());
        return List.of(group);
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }

    /**
     * This action will disregard any currently cached statistics data and
     * kick off a loader thread to load fresh statistics data.
     */
    private class StatsRefreshAction extends EnhancedAction {

        public StatsRefreshAction() {
            super("Refresh statistics");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            StatisticsLoaderThread loader = new StatisticsLoaderThread(MainWindow.getInstance().getDataManager());
            loader.addProgressListener(new ProgressListener(loader, false));
            String message = "Refreshing statistics";
            MultiProgressDialog dialog = new MultiProgressDialog(MainWindow.getInstance(), message);
            loading = true;
            dialog.runWorker(loader, true);
        }
    }

    /**
     * This action will show the StatisticsDialog if statistics have already been loaded.
     * Otherwise, will kick off a loader thread to load all statistics, and then show the dialog.
     */
    private class StatsDialogAction extends EnhancedAction {

        public StatsDialogAction() {
            super("Statistics dialog");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // If we already have some, just show them:
            Statistics stats = getStatistics();
            if (stats != null) {
                new StatisticsDialog(stats).setVisible(true);
                return;
            }

            // If a load is already in progress, do nothing - the user has clicked the
            // action link multiple times. They must wait, or cancel the load in progress.
            if (isLoading()) {
                log.warning("Ignoring request to show statistics dialog because statistics are currently loading.");
                return;
            }

            // Otherwise, let's load them now, and show the dialog when complete:
            loading = true;
            StatisticsLoaderThread loader = new StatisticsLoaderThread(MainWindow.getInstance().getDataManager());
            loader.addProgressListener(new ProgressListener(loader, true));
            String message = "Gathering statistics";
            MultiProgressDialog dialog = new MultiProgressDialog(MainWindow.getInstance(), message);
            dialog.runWorker(loader, true);
        }
    }

    /**
     * Listens to our loader thread, and shows the StatisticsDialog when loading is complete.
     * Our progress dialog will be visible while the loader is running, and the user will have
     * a "cancel" button they can use if the loader runs too long. If we detect a cancel event,
     * we simply do nothing here. User can try again later.
     */
    private class ProgressListener extends SimpleProgressAdapter {

        private final StatisticsLoaderThread loader;
        private final boolean showDialogOnComplete;

        public ProgressListener(StatisticsLoaderThread loader, boolean showDialogOnComplete) {
            this.loader = loader;
            this.showDialogOnComplete = showDialogOnComplete;
        }

        @Override
        public void progressCanceled() {
            setStatistics(null, false);
            getMessageUtil().info("Canceled", "Statistics gathering was canceled.");
        }

        @Override
        public void progressComplete() {
            setStatistics(loader.getStatistics(), showDialogOnComplete);

            if (!showDialogOnComplete) {
                getMessageUtil().info("Completed", "Statistics refresh is complete!");
            }
        }
    }
}
