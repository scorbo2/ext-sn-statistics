package ca.corbett.snotes.extensions.statistics;

import ca.corbett.extras.ScrollUtil;
import ca.corbett.snotes.Resources;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.filter.BooleanFilterType;
import ca.corbett.snotes.model.filter.DateFilterType;
import ca.corbett.snotes.model.filter.MonthFilter;
import ca.corbett.snotes.model.filter.YearFilter;
import ca.corbett.snotes.ui.MainWindow;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsDialog extends JDialog {

    public StatisticsDialog() {
        super(MainWindow.getInstance(), "Statistics", true);
        setSize(600, 300);
        setLocationRelativeTo(MainWindow.getInstance());
        setIconImage(Resources.getIconStats().getImage());
        initComponents();
    }

    private void initComponents() {
        // TODO tabbed pane with tabs for different statistics

        JTabbedPane tabPane = new JTabbedPane();

        // For now, let's just try to get a year chart up:
        JPanel freeChartPanel = new JPanel();
        freeChartPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
//        for (int year = 2010; year <= 2025; year++) {
//            ChartPanel chartPanel = new ChartPanel(StatisticsCharts.buildYearChart(fetchData(year)));
//            chartPanel.setBorder(null);
//            freeChartPanel.add(chartPanel, gbc);
//            gbc.gridy++;
//        }
//        tabPane.addTab("JFreeChart", freeChartPanel);

        JPanel customPanel = new JPanel();
        customPanel.setLayout(new GridBagLayout());
        gbc.gridy = 0;
        for (int year = 2010; year <= 2025; year++) {
            gbc.gridx = 0;
            JLabel label = new JLabel(Integer.toString(year));
            customPanel.add(label, gbc);
            gbc.gridx++;
            customPanel.add(new TestPanel(fetchData(year)), gbc);
            gbc.gridy++;
        }
        JPanel containerPanel = new JPanel();
        containerPanel.add(customPanel);
        tabPane.addTab("Custom", ScrollUtil.buildScrollPane(containerPanel));

        setLayout(new BorderLayout());
        add(tabPane, BorderLayout.CENTER);
    }

    private Map<Integer, Integer> fetchData(int year) {
        DataManager dataManager = MainWindow.getInstance().getDataManager();
        Map<Integer, Integer> monthToNoteCount = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            // Get a simple count of notes per month for the given year:
            Query query = new Query();
            query.addFilter(new YearFilter(year, DateFilterType.ON));
            query.addFilter(new MonthFilter(month, BooleanFilterType.IS));
            List<Note> notes = query.execute(dataManager.getNotes());
            monthToNoteCount.put(month, StatisticsUtil.countWords(notes));
        }
        return monthToNoteCount;
    }
}
