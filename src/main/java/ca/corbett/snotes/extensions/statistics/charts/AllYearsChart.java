package ca.corbett.snotes.extensions.statistics.charts;

import ca.corbett.snotes.extensions.statistics.Statistics;
import ca.corbett.snotes.extensions.statistics.StatisticsExtension;
import ca.corbett.snotes.extensions.statistics.StatisticsUtil;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
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
     * Creates a new AllYearsChart using the notes contained by the given Statistics instance.
     */
    public AllYearsChart(Statistics stats) {
        // Handle the case where we were given nothing at all to work with:
        if (stats == null ||
                stats.getTotalNoteCount() == 0 ||
                stats.getTotalWordCount() == 0 ||
                stats.getUniqueYears().isEmpty()) {
            buildEmptyLayout();
            return;
        }

        // For all other cases, build the appropriate layout based on how many years we have to work with:
        List<Integer> uniqueYears = stats.getUniqueYears();
        if (uniqueYears.size() < MIN_YEARS) {
            buildLabelLayout(stats);
        }
        else {
            buildHeatmapLayout(stats);
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
    private void buildLabelLayout(Statistics stats) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 20, 4, 4);
        for (int year : stats.getUniqueYears()) {
            String noteCount = String.format("%,d", stats.getNoteCountByYear().getOrDefault(year, 0));
            String wordCount = String.format("%,d", stats.getWordCountByYear().getOrDefault(year, 0));
            String label = String.format("<html><b>%d:</b> %s notes (%s words)</html>", year, noteCount, wordCount);
            add(new JLabel(label), gbc);
            gbc.gridy++;
        }
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = stats.getUniqueYears().size();
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(new JLabel(), gbc); // spacer to push labels to the left
    }

    /**
     * If we were given at least MIN_YEARS distinct years to work with,
     * build out a heatmap layout with a cell for each year.
     */
    private void buildHeatmapLayout(Statistics stats) {
        final int minWordCount = findMinWordCount(stats);
        final int maxWordCount = findMaxWordCount(stats);
        final Color coldColor = StatisticsExtension.getColdColor();
        final Color hotColor = StatisticsExtension.getHotColor();

        // Now we can create and color a ValueCell for each year:
        List<ValueCell> valueCells = new ArrayList<>();
        for (int year : stats.getUniqueYears()) {
            int wordCountForYear = stats.getWordCountByYear().getOrDefault(year, 0);
            String noteCount = String.format("%,d", stats.getNoteCountByYear().getOrDefault(year, 0));
            String wordCount = String.format("%,d", wordCountForYear);
            String tooltip = String.format("<html><b>%d:</b> %s notes (%s words)</html>", year, noteCount, wordCount);
            ValueCell cell = new ValueCell(tooltip, coldColor, wordCountForYear);
            float normalized = StatisticsUtil.normalizeValue(wordCountForYear, minWordCount, maxWordCount);
            cell.setValueColor(normalized, coldColor, hotColor);
            valueCells.add(cell);
        }

        // Now render each one:
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        for (ValueCell cell : valueCells) {
            add(cell, gbc);
            gbc.gridx++;
        }

        // This could be very wide indeed for large datasets.
        // It's up to the caller to stick us into a JScrollPane.
        // Let's try to figure out our preferred size so that the scroll pane's scroll bars will work correctly:
        int cellSize = valueCells.get(0).getCellSize();
        setPreferredSize(new Dimension(cellSize * valueCells.size(), cellSize));
    }

    private int findMinWordCount(Statistics stats) {
        return stats.getUniqueYears().stream()
                    .map(year -> stats.getWordCountByYear().getOrDefault(year, 0))
                    .min(Integer::compareTo)
                    .orElse(0);
    }

    private int findMaxWordCount(Statistics stats) {
        int max = stats.getUniqueYears().stream()
                       .map(year -> stats.getWordCountByYear().getOrDefault(year, 0))
                       .max(Integer::compareTo)
                       .orElse(1); // avoid divide-by-zero if all values are zero
        return max == 0 ? 1 : max; // ensure max is at least 1 to avoid divide-by-zero
    }
}
