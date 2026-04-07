package ca.corbett.snotes.extensions.statistics;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.ScrollUtil;
import ca.corbett.extras.progress.MultiProgressDialog;
import ca.corbett.extras.progress.SimpleProgressAdapter;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ButtonField;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.snotes.Resources;
import ca.corbett.snotes.extensions.statistics.charts.AllYearsChart;
import ca.corbett.snotes.extensions.statistics.charts.MonthChart;
import ca.corbett.snotes.extensions.statistics.charts.WeekChart;
import ca.corbett.snotes.extensions.statistics.charts.YearChart;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.TagList;
import ca.corbett.snotes.ui.MainWindow;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Provides a JTabbedPane-based dialog for showing various statistics about the user's notes.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class StatisticsDialog extends JDialog {

    private final Logger log = Logger.getLogger(StatisticsDialog.class.getName());

    private static final int DIALOG_WIDTH = 600;
    private final Statistics stats;
    private ComboField<String> monthCombo;
    private ComboField<String> yearCombo;
    private MonthChart monthChart;
    private ComboField<Integer> phraseLengthCombo;
    private final LabelField[] phraseLabels = new LabelField[10];
    private FormPanel wordSearchForm;
    private ShortTextField wordSearchField;
    private MessageUtil messageUtil;

    /**
     * Creates a StatisticsDialog using the given Statistics instance as the source of all
     * data for the charts and displays in this dialog. For performance reasons, we don't
     * want to gather stats on the UI thread. Use StatisticsLoaderThread to load up a
     * Statistics instance on a background thread.
     */
    public StatisticsDialog(Statistics stats) {
        super(MainWindow.getInstance(), "Statistics", true);
        setSize(DIALOG_WIDTH, 500);
        setLocationRelativeTo(MainWindow.getInstance());
        setIconImage(Resources.getIconStats().getImage());
        this.stats = stats;
        initComponents();
    }

    private void initComponents() {
        JTabbedPane tabPane = new JTabbedPane();
        tabPane.addTab("Overview", ScrollUtil.buildScrollPane(buildOverviewTab()));
        tabPane.addTab("Years", ScrollUtil.buildScrollPane(buildYearsTab()));
        tabPane.addTab("Months", ScrollUtil.buildScrollPane(buildMonthsTab()));
        tabPane.addTab("Weeks", ScrollUtil.buildScrollPane(buildWeeksTab()));
        tabPane.addTab("Phrases", ScrollUtil.buildScrollPane(buildPhrasesTab()));
        tabPane.addTab("Words", ScrollUtil.buildScrollPane(buildWordsTab()));

        setLayout(new BorderLayout());
        add(tabPane, BorderLayout.CENTER);
    }

    /**
     * Builds an at-a-glance overview tab showing sum totals and an AllYearsChart.
     */
    private JComponent buildOverviewTab() {
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(8);

        // Quick stats:
        formPanel.add(LabelField.createBoldHeaderLabel("Overview"));
        formPanel.add(new LabelField(String.format("You have written %,d notes in total.", stats.getTotalNoteCount())));
        formPanel.add(new LabelField(String.format("Those notes contain a total of %,d words.", stats.getTotalWordCount())));

        // We'll show an AllYearsChart on this overview tab,
        // but clip it horizontally, as it can grow unbounded with many years of notes.
        PanelField panelField = new PanelField(new FlowLayout(FlowLayout.LEFT));
        AllYearsChart chart = new AllYearsChart(stats);
        JScrollPane scrollPane = ScrollUtil.buildScrollPane(chart);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        int scrollBarHeight = new JScrollBar(JScrollBar.HORIZONTAL).getPreferredSize().height;
        scrollPane.setPreferredSize(new Dimension(DIALOG_WIDTH - 100, chart.getPreferredSize().height + scrollBarHeight));
        panelField.getPanel().add(scrollPane);
        formPanel.add(panelField);

        return formPanel;
    }

    /**
     * Builds a tab showing a YearChart for each year represented in the notes.
     */
    private JComponent buildYearsTab() {
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(8);
        formPanel.add(LabelField.createBoldHeaderLabel("Notes by year"));

        // We'll start with a YearChart showing the sum total of ALL years:
        PanelField summaryPanel = new PanelField(new FlowLayout(FlowLayout.LEFT));
        YearChart allYearsChart = new YearChart(stats);
        summaryPanel.getPanel().add(allYearsChart);
        summaryPanel.getFieldLabel().setText("All years:");
        formPanel.add(summaryPanel);

        // If that chart isn't empty, we can use it to build individual YearCharts for each year:
        int minValue = Integer.MAX_VALUE; // We can't use the values from allYearsChart,
        int maxValue = Integer.MIN_VALUE; // because they are summed across all years.
        if (! allYearsChart.isEmpty()) {
            PanelField yearsPanel = new PanelField(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            for (int year : stats.getUniqueYears()) {
                gbc.gridx = 0;
                gbc.insets = new Insets(0, 30, 0, 6);
                yearsPanel.getPanel().add(new JLabel(year + ":"), gbc);

                gbc.insets = new Insets(0, 0, 0, 0);
                gbc.gridx = 1;
                YearChart yearChart = new YearChart(stats, year, minValue, maxValue);
                yearsPanel.getPanel().add(yearChart, gbc);
                gbc.gridy++;

                // Our min and max values have been updated:
                minValue = yearChart.getMinValue();
                maxValue = yearChart.getMaxValue();
            }

            // Now that we have data from each individual year, we can recompute
            // our color scale across all the year charts, to make them true
            // sibling charts. This turns all the individual year heatmap charts
            // into one consistent heatmap, making it easier to compare years at a glance.
            for (Component comp : yearsPanel.getPanel().getComponents()) {
                if (comp instanceof YearChart yearChart) {
                    yearChart.recolorCells(minValue, maxValue);
                }
            }

            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.gridheight = stats.getUniqueYears().size();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            yearsPanel.getPanel().add(new JLabel(), gbc); // dummy filler to push content to the left

            formPanel.add(yearsPanel);
        }

        return formPanel;
    }

    /**
     * Builds a MonthChart showing data for the days of the month.
     */
    private JComponent buildMonthsTab() {
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(8);
        formPanel.add(LabelField.createBoldHeaderLabel("Notes by month"));

        Month mostActiveMonthByWords = stats.getMostActiveMonthByWords();
        if (mostActiveMonthByWords != null) {
            formPanel.add(new LabelField("You seem to write the most words in "
                                                 + mostActiveMonthByWords.getDisplayName(TextStyle.FULL,
                                                                                         Locale.getDefault())
                                                 + ": "
                                                 + String.format("%,d words", stats.getAllMonthsWordCount()
                                                                                   .getOrDefault(
                                                                                           mostActiveMonthByWords.getValue(),
                                                                                           0)) + " total."));
        }
        Month mostActiveMonthByNotes = stats.getMostActiveMonthByNotes();
        if (mostActiveMonthByNotes != null) {
            formPanel.add(new LabelField("You seem to create the most notes in "
                                                 + mostActiveMonthByNotes.getDisplayName(TextStyle.FULL,
                                                                                         Locale.getDefault())
                                                 + ": "
                                                 + String.format("%,d notes", stats.getAllMonthsNoteCount()
                                                                                   .getOrDefault(
                                                                                           mostActiveMonthByNotes.getValue(),
                                                                                           0)) + " total."));
        }

        List<String> months = new ArrayList<>(13);
        months.add("All months");
        for (int i = 1; i <= 12; i++) {
            months.add(Month.of(i).getDisplayName(TextStyle.FULL, Locale.getDefault()));
        }
        monthCombo = new ComboField<>("Select month:", months, 0);
        monthCombo.addValueChangedListener(field -> monthFilterChanged());
        formPanel.add(monthCombo);

        List<String> years = new ArrayList<>(stats.getUniqueYears().size() + 1);
        years.add("All years");
        stats.getUniqueYears().stream().sorted().forEach(year -> years.add(String.valueOf(year)));
        yearCombo = new ComboField<>("Select year:", years, 0);
        yearCombo.addValueChangedListener(field -> monthFilterChanged());
        formPanel.add(yearCombo);

        monthChart = new MonthChart(stats.getAllNotes(), stats.getUniqueYears());
        PanelField panelField = new PanelField(new FlowLayout(FlowLayout.LEFT));
        panelField.getPanel().add(monthChart);
        formPanel.add(panelField);

        // No sense allowing filter options if there's nothing to filter:
        if (monthChart.isEmpty()) {
            monthCombo.setEnabled(false);
            yearCombo.setEnabled(false);
        }

        return formPanel;
    }

    /**
     * Invoked internally when the user changes either the month or year selection in the combo boxes on the Months tab.
     */
    private void monthFilterChanged() {
        Integer yearFilter = yearCombo.getSelectedIndex() > 0 ? Integer.valueOf(yearCombo.getSelectedItem()) : null;
        Integer monthFilter = monthCombo.getSelectedIndex() > 0 ? monthCombo.getSelectedIndex() : null;
        monthChart.setFilter(yearFilter, monthFilter);
    }

    /**
     * Builds a day-of-week tab showing the most active day of the week, and a WeekChart for all notes.
     */
    private JComponent buildWeeksTab() {
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(8);
        formPanel.add(LabelField.createBoldHeaderLabel("Notes by day of week"));

        DayOfWeek mostActiveDayByNotes = stats.getMostActiveDayByNotes();
        if (mostActiveDayByNotes != null) {
            formPanel.add(new LabelField("You seem to create the most notes on "
                                                 + mostActiveDayByNotes.getDisplayName(TextStyle.FULL, Locale.getDefault())+"."));
        }
        DayOfWeek mostActiveDayByWords = stats.getMostActiveDayByWords();
        if (mostActiveDayByWords != null) {
            formPanel.add(new LabelField("You seem to write the most words on "
                                                 + mostActiveDayByWords.getDisplayName(TextStyle.FULL, Locale.getDefault())+"."));
        }

        WeekChart weekChart = new WeekChart(stats);
        PanelField panelField = new PanelField(new FlowLayout(FlowLayout.LEFT));
        panelField.getPanel().add(weekChart);
        panelField.getFieldLabel().setText("Days of week (all time):");
        formPanel.add(panelField);

        return formPanel;
    }

    /**
     * Builds a commonly-used-phrases tab, showing the most common phrases of a user-selected length.
     * The user can select a phrase length from 2-10, and we will show the top 10 most common phrases
     * of that length, along with their occurrence counts.
     */
    private JComponent buildPhrasesTab() {
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(8);
        formPanel.add(LabelField.createBoldHeaderLabel("Most common phrases"));

        phraseLengthCombo = new ComboField<>("Phrase length:", List.of(2, 3, 4, 5, 6, 7, 8, 9, 10), 0);
        phraseLengthCombo.addValueChangedListener(field -> phraseLengthChanged());
        formPanel.add(phraseLengthCombo);

        for (int i = 0; i < phraseLabels.length; i++) {
            phraseLabels[i] = new LabelField("");
            phraseLabels[i].getMargins().setLeft(20);
            formPanel.add(phraseLabels[i]);
        }

        phraseLengthChanged(); // initialize with default selection

        return formPanel;
    }

    /**
     * Invoked when the user changes the phrase length selection in the combo box.
     * This is an easy flip to the new data - everything is loaded in advance,
     * so there's no heavy lift here.
     */
    private void phraseLengthChanged() {
        // Clear any previous data:
        for (LabelField label : phraseLabels) {
            label.setText("");
        }

        // See if we have any phrases of this length:
        Integer length = phraseLengthCombo.getSelectedItem();
        List<Phrase> filtered = stats.getPhrasesByLength().get(length == null ? 2 : length);

        // Handle case where there simply are none:
        if (filtered == null || filtered.isEmpty()) {
            phraseLabels[0].setText("(no phrases of length " + length + " found)");
            return;
        }

        // Otherwise, load them up (they are already in descending order):
        for (int i = 0; i < filtered.size() && i < phraseLabels.length; i++) {
            Phrase phrase = filtered.get(i);
            String label = String.format("<html>%d. <b>%s</b> (used %,d times)</html>", (i + 1), phrase.phrase(),
                                         phrase.occurrenceCount());
            phraseLabels[i].setText(label);
        }
    }

    /**
     * Builds out our "Top N Words" tab, showing the most common words across all notes and their occurrence counts.
     * Also provides a control to allow an on-the-fly search for specific word(s).
     */
    private JComponent buildWordsTab() {
        wordSearchForm = new FormPanel(Alignment.TOP_LEFT);
        wordSearchForm.setBorderMargin(8);
        wordSearchForm.add(LabelField.createBoldHeaderLabel("Most common words"));

        WordList topWords = stats.getTopWords();

        // If the "total unique words" feature is enabled, show that count here.
        if (topWords.getUniqueWordCount() != WordList.NO_DATA) {
            wordSearchForm.add(
                    new LabelField(String.format("You've typed a total of %,d unique words across all your notes.",
                                                 topWords.getUniqueWordCount())));
        }

        if (topWords.getWords().isEmpty()) {
            wordSearchForm.add(new LabelField("(no data)"));
            return wordSearchForm; // Don't show the rest if there's nothing to work with.
        }

        // Show a label for each one. The list should already be sorted in descending order.
        // We don't know exactly how many entries there will be here, but it should be reasonable
        // enough to just show inline like this:
        int number = 1;
        for (Word word : topWords.getWords()) {
            String label = String.format("<html>%d. <b>%s</b> (used %,d times)</html>", number, word.word(),
                                         word.occurrenceCount());
            LabelField labelField = new LabelField(label);
            labelField.getMargins().setLeft(20); // indent the list a bit
            wordSearchForm.add(labelField);
            number++;
        }

        wordSearchForm.add(LabelField.createBoldHeaderLabel("Word search", 14));
        wordSearchField = new ShortTextField("Search for word(s):", 20);
        wordSearchField.setAllowBlank(false);
        wordSearchField.setHelpText("Enter a comma-separated list of 1-25 words to search for.");
        wordSearchForm.add(wordSearchField);

        wordSearchForm.add(new ButtonField(List.of(new WordSearchAction())));

        return wordSearchForm;
    }

    /**
     * Shows the result of a word search in an info dialog.
     */
    private void showWordSearchResults(WordList foundWords) {
        if (foundWords == null) {
            getMessageUtil().error("Word search error", "An unexpected error occurred while searching for words.");
            return;
        }

        StringBuilder sb = new StringBuilder("Search results:\n\n");
        int totalOccurrences = foundWords.getWords().stream().mapToInt(Word::occurrenceCount).sum();
        sb.append(String.format("Found %,d combined occurrences across all notes.\n\n", totalOccurrences));
        int num = 1;
        for (Word word : foundWords.getWords()) {
            // This list might be smaller than we expect! Not all words may have been found.
            // Any word not mentioned here has zero occurrences, and we can just ignore it in the results.
            // If no word was found, the label above will show "0 combined occurrences", which is fine.
            sb.append(String.format("  %d. %s: %,d occurrences\n", num, word.word(), word.occurrenceCount()));
            num++;
        }
        getMessageUtil().info("Word search results", sb.toString());
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, log);
        }
        return messageUtil;
    }

    /**
     * Invoked when the user clicks the "Search" button on the Words tab. This will kick off a WordSearchThread
     * to do the actual searching, and will show a progress dialog while the search is underway.
     * A dialog with the results is shown upon completion.
     */
    private class WordSearchAction extends EnhancedAction {

        private WordSearchAction() {
            super("Search");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!wordSearchForm.isFormValid()) {
                return; // field was left blank - user must fill it in first
            }

            // This is of course not a TagList, but TagList conveniently already
            // has parsing logic for turning a comma-separated string into a list of normalized,
            // de-duplicated, non-blank items, so let's use it:
            TagList tagList = TagList.fromRawString(wordSearchField.getText());

            // Convert to a set for findWords():
            Set<String> wordSet = new HashSet<>(tagList.getTags().size());
            for (Tag tag : tagList.getTags()) {
                wordSet.add(tag.getTag());
            }

            // Fire off a worker thread to do the loading off the Swing EDT:
            WordSearchThread worker = new WordSearchThread(stats.getAllNotes(), wordSet);
            worker.addProgressListener(new SimpleProgressAdapter() {
                @Override
                public void progressComplete() {
                    SwingUtilities.invokeLater(() -> showWordSearchResults(worker.getResults()));
                }
            });
            MultiProgressDialog dialog = new MultiProgressDialog(StatisticsDialog.this, "Searching for words...");
            dialog.runWorker(worker, true);
        }
    }
}
