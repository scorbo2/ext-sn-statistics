package ca.corbett.snotes.extensions.statistics.charts;

import ca.corbett.extras.LookAndFeelManager;
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
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a fixed-width horizontal heatmap chart with 12 cells, one for each
 * month in a calendar year. The chart can either display data from a specific year,
 * or it can combine data from all years and show the total for each month across all years.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class YearChart extends JPanel {

    private int minValue;
    private int maxValue;
    private final boolean empty;
    private final List<ValueCell> valueCells;

    /**
     * Builds a YearChart showing the total word count for each month across all years.
     */
    public YearChart(Statistics stats) {
        this(stats, null);
    }

    /**
     * Builds a YearChart showing the total word count for each month in the given year.
     */
    public YearChart(Statistics stats, Integer year) {
        this(stats, year, Integer.MAX_VALUE, Integer.MIN_VALUE);
    }

    /**
     * If you are building sibling charts and want them to share a common color scale, you can use this constructor
     * to specify the min and max values for the color scale. If you do this, it's important to check
     * getMinValue() and getMaxValue() after construction to see if the data range has been expanded
     * during construction of this chart.
     *
     * @param stats    The Statistics instance containing all the data we need to build this chart.
     * @param year     The year in question, or null to gather and sum data from all years.
     * @param minValue The lowest value observed in any data set so far.
     * @param maxValue The highest value observed in any data set so far.
     */
    public YearChart(Statistics stats, Integer year, int minValue, int maxValue) {
        valueCells = new ArrayList<>(12); // 12 entries, one for each month.

        // Handle the case where we were given nothing at all to work with:
        if (stats == null || stats.getTotalNoteCount() == 0 || stats.getTotalWordCount() == 0) {
            buildEmptyLayout();
            this.empty = true;
            return;
        }

        // Edge case: we were given a specific year, but there's no data in that year:
        if (year != null && !stats.getUniqueYears().contains(year)) {
            buildEmptyLayout();
            this.empty = true;
            return;
        }

        this.empty = false;
        this.minValue = minValue;
        this.maxValue = maxValue;
        buildHeatmapLayout(stats, year);
    }

    /**
     * Reports whether this chart was given no data to work with.
     * If this returns true, we are not displaying a heat map, but
     * rather a simple label display saying "(no data)".
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Returns the lowest value observed in any month in this chart.
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * Returns the highest value observed in any month in this chart.
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * If we were given nothing at all to work with, build out a very simple
     * label-based layout that just says "(no data)".
     */
    private void buildEmptyLayout() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(new JLabel("(no data)"));
    }

    private void buildHeatmapLayout(Statistics stats, Integer year) {
        // Gather all data up front and build all ValueCells at once.
        // This lets us figure out min/max values for the color scale.
        Color defaultColor = LookAndFeelManager.getLafColor("Panel.background", Color.LIGHT_GRAY);
        valueCells.clear();
        for (int month = 1; month <= 12; month++) {
            int noteCount;
            int wordCount;
            if (year == null) {
                // Sum totals for this month across all years:
                noteCount = stats.getAllMonthsNoteCount().getOrDefault(month, 0);
                wordCount = stats.getAllMonthsWordCount().getOrDefault(month, 0);
            }
            else {
                // Find out how many notes were written in this month and year, and how many words those notes contain:
                Map<Integer, Integer> noteCountByMonth = stats.getNoteCountByYearAndMonth().get(year);
                Map<Integer, Integer> wordCountByMonth = stats.getWordCountByYearAndMonth().get(year);
                noteCount = noteCountByMonth != null ? noteCountByMonth.getOrDefault(month, 0) : 0;
                wordCount = wordCountByMonth != null ? wordCountByMonth.getOrDefault(month, 0) : 0;
            }
            minValue = wordCount != 0 ? Math.min(minValue, wordCount) : minValue; // ignore 0 -> "no data"
            maxValue = Math.max(maxValue, wordCount);

            // Compute our ValueCell, but don't color it just yet:
            LocalDate dateForTooltip = LocalDate.of(year != null ? year : 0, month, 1);
            String monthName = dateForTooltip.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
            String label = year == null ? monthName + " (all years)" : monthName + " " + year;
            label = "<html><b>" + label + ":</b> "
                    + String.format("%,d notes", noteCount)
                    + String.format(" (%,d words)", wordCount) + "</html>";
            valueCells.add(new ValueCell(label, defaultColor, wordCount));
        }

        // Now render each one, and compute its color in our range based on the value:
        Color coldColor = StatisticsExtension.getColdColor();
        Color hotColor = StatisticsExtension.getHotColor();
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        for (ValueCell cell : valueCells) {
            cell.setValueColor(StatisticsUtil.normalizeValue(cell.getValue(), minValue, maxValue), coldColor, hotColor);
            add(cell, gbc);
            gbc.gridx++;
        }

        // This could be very wide indeed for large datasets.
        // It's up to the caller to stick us into a JScrollPane.
        // Let's try to figure out our preferred size so that the scroll pane's scroll bars will work correctly:
        int cellSize = valueCells.get(0).getCellSize();
        setPreferredSize(new Dimension(cellSize * valueCells.size(), cellSize));
    }

    /**
     * If the data range for this chart has changed since we built it,
     * call this method to update the colors of all the cells accordingly.
     *
     * @param newMinValue The new low value for this chart.
     * @param newMaxValue The new high value for this chart.
     */
    public void recolorCells(int newMinValue, int newMaxValue) {
        this.minValue = newMinValue;
        this.maxValue = newMaxValue;
        Color coldColor = StatisticsExtension.getColdColor();
        Color hotColor = StatisticsExtension.getHotColor();
        for (ValueCell cell : valueCells) {
            cell.setValueColor(StatisticsUtil.normalizeValue(cell.getValue(), minValue, maxValue), coldColor, hotColor);
        }
    }
}
