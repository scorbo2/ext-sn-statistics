package ca.corbett.snotes.extensions.statistics.charts;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.snotes.extensions.statistics.StatisticsExtension;
import ca.corbett.snotes.extensions.statistics.StatisticsUtil;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.filter.BooleanFilterType;
import ca.corbett.snotes.model.filter.DateFilterType;
import ca.corbett.snotes.model.filter.DayOfMonthFilter;
import ca.corbett.snotes.model.filter.MonthFilter;
import ca.corbett.snotes.model.filter.YearFilter;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A fixed-size heatmap chart with six rows of seven cells, representing the days
 * of a calendar month, broken into weeks. Each row represents the days of a single
 * week, ranging from Sunday on the left up to Saturday on the right.
 * (We use the North American convention of Sunday as first day, ignoring ISO-8601).
 * <p>
 * If no specific month or year are given, the chart will show 31 filled cells,
 * representing the summed note and word count for each day of the month across
 * all years. This means that the first filled cell would contain statistics
 * for all notes that were created on the first day of any month in any year,
 * and so on for all 31 cells.
 * </p>
 * <p>
 * If a specific month is given, but no year, the chart will show 28 to 31 filled
 * cells (depending on the month), with each cell showing the summed total for
 * all notes created on that day of that month across all years.
 * For example, if the month is February, then the first cell would show statistics
 * for all notes created on February 1st of any year, the second cell would show
 * statistics for all notes created on February 2nd of any year, and so on.
 * </p>
 * <p>
 * If a specific month and a specific year are given, the chart will show 28 to 31
 * filled cells (depending on the month and year), with data from that
 * specific month in that specific year.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class MonthChart extends JPanel {

    private final List<Note> allNotes;
    private final List<Integer> uniqueYears;
    private final ValueCell[][] cells = new ValueCell[6][7];
    private final boolean empty;
    private Integer yearFilter = null;
    private Integer monthFilter = null;

    public MonthChart(List<Note> allNotes, List<Integer> uniqueYears) {
        this.allNotes = allNotes;
        this.uniqueYears = uniqueYears;
        if (allNotes == null || allNotes.isEmpty() || uniqueYears == null || uniqueYears.isEmpty()) {
            empty = true;
            buildEmptyLayout();
        }
        else {
            empty = false;
            buildHeatmapLayout();
            this.yearFilter = 9999; // this is dumb, but setFilter rejects no-ops, so we need a dummy initial value.
            setFilter(null, null); // initialize with a summary view across all time
        }
    }

    /**
     * Reports whether this chart was given any data at all to work with.
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Filters this chart to the specific year and/or month, either of which can be null.
     * See the class javadoc for an explanation as to what data this chart will show.
     * The default filter is (null,null) - this means "sum data for all months and all years".
     *
     * @param year  an optional year to filter by, or null to include all years
     * @param month an optional month to filter by (1-12), or null to include all months
     */
    public void setFilter(Integer year, Integer month) {
        // Check for no-ops and ignore them:
        if (Objects.equals(year, this.yearFilter) && Objects.equals(month, this.monthFilter)) {
            return;
        }
        this.yearFilter = year;
        this.monthFilter = month;
        resetChart();

        // If we were given a year AND a month, then figure out which cell in our grid should
        // be the first filled cell, depending on what day of the week the 1st of that month was.
        int x = 0;
        int y = 0;
        if (yearFilter != null && monthFilter != null) {
            LocalDate firstOfMonth = LocalDate.of(yearFilter, monthFilter, 1);
            DayOfWeek firstDayOfMonth = firstOfMonth.getDayOfWeek();
            x = convert8601(firstDayOfMonth) - 1; // convert from ISO-8601 to our Sunday-first convention
        }

        // Now go through every day of the calendar month:
        int minWords = Integer.MAX_VALUE;
        int maxWords = Integer.MIN_VALUE;
        for (int currentDay = 1; currentDay <= 31; currentDay++) {
            Query query = new Query();
            query.addFilter(new DayOfMonthFilter(currentDay, BooleanFilterType.IS));
            if (yearFilter != null) {
                query.addFilter(new YearFilter(yearFilter, DateFilterType.ON));
            }
            if (monthFilter != null) {
                query.addFilter(new MonthFilter(monthFilter, BooleanFilterType.IS));
            }

            // This is the only chart type where we query for data on the UI thread,
            // rather than relying on StatisticsLoaderThread to load it in advance.
            // The reason is that we'd have to preload every day for every month for
            // every year to load it in advance, which seems unfeasible.
            List<Note> dayNotes = query.execute(allNotes); // may be empty but will never be null
            int wordCount = StatisticsUtil.countWords(dayNotes);
            int noteCount = dayNotes.size();

            // All our ValueCells were cleared by resetChart() above, so if nothing returns from
            // the query, we can simply skip it and move on. The cell is already showing the "no data" color.
            if (wordCount > 0 && noteCount > 0) {
                String tooltip = String.format("%s: %,d notes (%,d words)",
                                               getDateDescription(currentDay), noteCount, wordCount);
                cells[y][x].setToolTipText(tooltip);
                cells[y][x].setValue(wordCount);
                minWords = Math.min(minWords, wordCount);
                maxWords = Math.max(maxWords, wordCount);
            }

            // Move to next cell:
            x++;
            if (x == 7) { // end of week, move down to next row
                x = 0;
                y++;
            }
        }

        // Now we can color the cells based on the min/max word count observed for this month:
        Color coldColor = StatisticsExtension.getColdColor();
        Color hotColor = StatisticsExtension.getHotColor();
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                ValueCell cell = cells[row][col];
                int wordCount = cell.getValue();
                if (wordCount != 0) { // this cell has data, so color it based on the word count
                    cell.setValueColor(StatisticsUtil.normalizeValue(wordCount, minWords, maxWords), coldColor,
                                       hotColor);
                }
            }
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
     * Creates our fixed-size layout with 6 rows of 7 columns, and fills each cell
     * with the "no data" color for a default starting position.
     */
    private void buildHeatmapLayout() {
        Color noDataColor = LookAndFeelManager.getLafColor("Panel.background", Color.LIGHT_GRAY);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                cells[row][col] = new ValueCell("", noDataColor, 0, 42);
                add(cells[row][col], gbc);
                gbc.gridx++;
            }
            gbc.gridx = 0;
            gbc.gridy++;
        }

        // Let's try to set our preferred size accurately:
        int cellSize = cells[0][0].getCellSize();
        setPreferredSize(new Dimension(cellSize * 7, cellSize * 6));
    }

    /**
     * Clears the chart, before populating with new data.
     */
    private void resetChart() {
        Color noDataColor = LookAndFeelManager.getLafColor("Panel.background", Color.LIGHT_GRAY);
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                cells[row][col].setBackground(noDataColor);
                cells[row][col].setToolTipText("");
                cells[row][col].setValue(0);
            }
        }
    }

    /**
     * For reasons unknown, ISO-8601 numbers days of the week from 1 (Monday) to 7 (Sunday).
     * But our weeks start with Sunday, so we need to shift the numbers around a bit.
     *
     * @param day a java.time.DayOfWeek instance
     * @return A number from 1 (Sunday) to 7 (Saturday) representing the given day of the week.
     */
    private int convert8601(DayOfWeek day) {
        int isoValue = day.getValue();
        return isoValue == 7 ? 1 : isoValue + 1;
    }

    /**
     * Based on our current yearFilter (which might be null) and our current monthFilter
     * (which might also be null), returns a human-readable description of given day of the month.
     * For example, given a filter of (null,null) and a day number of 1, this returns "1st of all months in all years".
     *
     * @param dayNumber A day number from 1 to 31 representing the day of the month.
     * @return A user-presentable String representation of that day.
     */
    private String getDateDescription(int dayNumber) {
        // English is so much fun:
        String suffix;
        if (dayNumber == 1 || dayNumber == 21 || dayNumber == 31) {
            suffix = "st";
        }
        else if (dayNumber == 2 || dayNumber == 22) {
            suffix = "nd";
        }
        else if (dayNumber == 3 || dayNumber == 23) {
            suffix = "rd";
        }
        else {
            suffix = "th";
        }

        // We may or may not be filtered to a specific month:
        String month = monthFilter == null
                ? "all months"
                : Month.of(monthFilter).getDisplayName(TextStyle.FULL, Locale.getDefault());

        // We may or may not be filtered to a specific year:
        String year = yearFilter == null
                ? "all years"
                : yearFilter.toString();

        // Now we can put it all together:
        return String.format("%d%s of %s in %s", dayNumber, suffix, month, year);
    }
}
