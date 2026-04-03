package ca.corbett.snotes.extensions.statistics;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.snotes.extensions.SnotesExtension;

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

    /**
     * We must supply a no-arg constructor to be invoked by ExtensionManager.
     * We can load jar resources only here or in loadJarResources() - after we
     * are instantiated, our ClassLoader is closed.
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
        // Currently none
        return List.of();
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
    /* TODO uncomment when StatisticsDialog is implemented
    @Override
    public List<ActionGroup> getActionGroups() {
        ActionGroup group = new ActionGroup("Statistics", Resources.getIconStats());
        group.addAction(new StatsDialogAction());
        return List.of(group);
    }
     */

    /**
     * A very simple action to launch our StatisticsDialog.
     */
    /* TODO uncomment when StatisticsDialog is implemented
    private static class StatsDialogAction extends EnhancedAction {

        public StatsDialogAction() {
            super("Statistics dialog");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new StatisticsDialog().setVisible(true);
        }
    }
     */
}
