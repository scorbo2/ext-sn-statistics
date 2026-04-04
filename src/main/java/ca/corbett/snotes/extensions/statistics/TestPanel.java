package ca.corbett.snotes.extensions.statistics;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;

public class TestPanel extends JPanel {

    public TestPanel(Map<Integer, Integer> data) {
        final int minCount = 0; // min is always zero regardless of the input
        int maxCount = data.values().stream().max(Integer::compareTo).orElse(1);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int month = 1; month <= 12; month++) {
            int value = data.getOrDefault(month, 0);
            float valueF = (float)value / maxCount; // normalize to 0-1
            JPanel cell = new JPanel();
            cell.setBackground(StatisticsCharts.blendColors(valueF, StatisticsCharts.COLD, StatisticsCharts.HOT));
            cell.setBorder(BorderFactory.createLoweredBevelBorder());
            cell.setPreferredSize(new Dimension(24, 24));
            cell.setMinimumSize(new Dimension(24, 24));
            cell.setMaximumSize(new Dimension(24, 24));
            cell.setToolTipText(String.format("%s: %d", monthNames[month - 1], value));
            add(cell, gbc);
            gbc.gridx++;
        }
        setBorder(BorderFactory.createRaisedBevelBorder());
    }
}