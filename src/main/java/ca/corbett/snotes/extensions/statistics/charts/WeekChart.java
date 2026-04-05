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
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A fixed-width heatmap chart showing the days of the week.
 * We will sum word and note count for each day of the week across all years,
 * and show the results in a 7-cell horizontal heatmap.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class WeekChart extends JPanel {

    public WeekChart(Statistics stats) {
        if (stats == null || stats.getTotalNoteCount() == 0 || stats.getTotalWordCount() == 0) {
            buildEmptyLayout();
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

    private void buildHeatmapLayout(Statistics stats) {
        final int minWordCount = findMinWordCount(stats);
        final int maxWordCount = findMaxWordCount(stats);
        final Color coldColor = StatisticsExtension.getColdColor();
        final Color hotColor = StatisticsExtension.getHotColor();

        // Now we can create and color a ValueCell for each day of the week:
        List<ValueCell> valueCells = new ArrayList<>();
        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            int wordCount = stats.getWordCountByDayOfWeek().getOrDefault(dayOfWeek, 0);
            int noteCount = stats.getNoteCountByDayOfWeek().getOrDefault(dayOfWeek, 0);
            DayOfWeek actualDay = StatisticsUtil.DAYS_OF_WEEK[dayOfWeek - 1];
            String tooltip = String.format("%s: %,d notes (%,d words)",
                                           actualDay.getDisplayName(TextStyle.FULL, Locale.getDefault()), noteCount,
                                           wordCount);
            ValueCell cell = new ValueCell(tooltip, coldColor, wordCount);
            float normalized = StatisticsUtil.normalizeValue(wordCount, minWordCount, maxWordCount);
            cell.setValueColor(normalized, coldColor, hotColor);
            valueCells.add(cell);
        }

        // Now render each one:
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
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
        int min = Integer.MAX_VALUE;
        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            int wordCount = stats.getWordCountByDayOfWeek().getOrDefault(dayOfWeek, 0);
            if (wordCount < min) {
                min = wordCount;
            }
        }
        return min;
    }

    private int findMaxWordCount(Statistics stats) {
        int max = Integer.MIN_VALUE;
        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            int wordCount = stats.getWordCountByDayOfWeek().getOrDefault(dayOfWeek, 0);
            if (wordCount > max) {
                max = wordCount;
            }
        }
        return max;
    }
}
