package ca.corbett.snotes.extensions.statistics.charts;

import ca.corbett.snotes.extensions.statistics.StatisticsExtension;
import ca.corbett.snotes.extensions.statistics.StatisticsUtil;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.QueryFactory;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Represents a variable-width heatmap chart showing a cell for each
 * year represented in the given notes. If fewer than 4 years are
 * represented, then simple text-based JLabels are used (no point
 * in showing a chart for so little data). Otherwise, a single-row
 * heatmap chart is shown. The dimensions of each cell is fixed,
 * and the chart will grow unbounded. Callers are advised to put
 * this chart inside a JScrollPane in case there are many years represented.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class AllYearsChart extends JPanel {

    /**
     * If we are given fewer than this number of distinct years
     * in the data set, we will switch to a text-based display
     * instead of a heatmap.
     */
    public static final int MIN_YEARS = 4;

    /**
     * Creates a new AllYearsChart using the notes contained by the given DataManager.
     */
    public AllYearsChart(DataManager dataManager) {
        // Handle the case where we were given nothing at all to work with:
        if (dataManager == null ||
                dataManager.getNotes().isEmpty() ||
                dataManager.getUniqueYears().isEmpty()) {
            buildEmptyLayout();
            return;
        }

        // For all other cases, build the appropriate layout based on how many years we have to work with:
        List<Integer> uniqueYears = dataManager.getUniqueYears();
        if (uniqueYears.size() < MIN_YEARS) {
            buildLabelLayout(dataManager, uniqueYears);
        }
        else {
            buildHeatmapLayout(dataManager, uniqueYears);
        }
    }

    /**
     * If we were given nothing at all to work with, build out a very simple
     * label-based layout that just says "(no data)".
     */
    private void buildEmptyLayout() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(new JLabel("(no data)"));
    }

    /**
     * If we were given less than MIN_YEARS distinct years to work with,
     * build out a simple label-based layout that just lists the years we have.
     */
    private void buildLabelLayout(DataManager dataManager, List<Integer> uniqueYears) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);
        for (int year : uniqueYears) {
            Query yearQuery = QueryFactory.year(year);
            List<Note> notes = yearQuery.execute(dataManager.getNotes());
            String noteCount = String.format("%,d", notes.size());
            String wordCount = String.format("%,d", StatisticsUtil.countWords(notes));
            String label = String.format("<html><b>%d:</b> %s notes (%s words)</html>", year, noteCount, wordCount);
            add(new JLabel(label), gbc);
            gbc.gridy++;
        }
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = uniqueYears.size();
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(new JLabel(), gbc); // spacer to push labels to the left
    }

    /**
     * If we were given at least MIN_YEARS distinct years to work with,
     * build out a heatmap layout with a cell for each year.
     */
    private void buildHeatmapLayout(DataManager dataManager, List<Integer> uniqueYears) {
        // This is a lot of Query executions on potentially a lot of data.
        // Should perhaps be done in a background thread...
        // But DataManager operates entirely via an in-memory cache, so maybe it's not that bad?

        // Compute all our ValueCells first so we can determine the max value for normalization:
        List<ValueCell> cells = uniqueYears.stream()
                                           .map(year -> {
                                               Query yearQuery = QueryFactory.year(year);
                                               List<Note> notes = yearQuery.execute(dataManager.getNotes());
                                               int wordCount = StatisticsUtil.countWords(notes);
                                               String tooltip = String.format(
                                                       "<html><b>%d</b>: %,d notes (%,d words)</html>",
                                                       year, notes.size(), wordCount);
                                               return new ValueCell(tooltip, StatisticsExtension.getColdColor(),
                                                                    wordCount);
                                           })
                                           .toList();

        final int minWordCount = cells.stream()
                                      .map(ValueCell::getValue)
                                      .min(Integer::compareTo)
                                      .orElse(0);
        final int maxWordCount = cells.stream()
                                      .map(ValueCell::getValue)
                                      .max(Integer::compareTo)
                                      .orElse(1); // avoid divide-by-zero if all values are zero

        // Now render each one, and compute its color in our range based on the value:
        Color coldColor = StatisticsExtension.getColdColor();
        Color hotColor = StatisticsExtension.getHotColor();
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        for (ValueCell cell : cells) {
            float valueF = (float)(cell.getValue() - minWordCount) / (maxWordCount - minWordCount); // normalize to 0-1
            cell.setValueColor(valueF, coldColor, hotColor);
            add(cell, gbc);
            gbc.gridx++;
        }

        // This could be very wide indeed for large datasets.
        // It's up to the caller to stick us into a JScrollPane.
        // Let's try to figure out our preferred size so that the scroll pane's scroll bars will work correctly:
        setPreferredSize(new Dimension(ValueCell.CELL_SIZE * cells.size(), ValueCell.CELL_SIZE));
    }
}
