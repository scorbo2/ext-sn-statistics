package ca.corbett.snotes.extensions.statistics;

import java.awt.Color;

public class StatisticsCharts {

    // TODO make these user-configurable.
    public static Color COLD = new Color(96, 96, 212);
    public static Color HOT = new Color(212, 96, 96);

    private StatisticsCharts() {
    }

    /**
     * Input: a Map of month number(1-12) to some value count for that month.
     * Output: a horizontal heat map chart with a single row of 12 cells.
     * The color of each cell will range from cold (low value) to hot (high value).
     * Your input map does not need to include all months. If a month is missing,
     * it is treated as having a value of zero. If your input map contains an
     * invalid month number (0 or less, 13 or more), it will be ignored.
     * Note that our month numbers are 1-based!
     */
    /*
    public static JFreeChart buildYearChart(Map<Integer, Integer> valuesByMonth) {
        final int minCount = 0; // min is always zero regardless of the input
        int maxCount = valuesByMonth.values().stream().max(Integer::compareTo).orElse(1);

        double[] xValues = new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        double[] yValues = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        double[] zValues = new double[12];
        for (int month = 1; month <= 12; month++) {
            zValues[month - 1] = valuesByMonth.getOrDefault(month, 0);
        }
        DefaultXYZDataset dataset = new DefaultXYZDataset();
        dataset.addSeries("", new double[][]{xValues, yValues, zValues});

        LookupPaintScale paintScale = buildPaintScale(minCount, maxCount, 10);
        XYBlockRenderer renderer = new XYBlockRenderer();
        renderer.setBlockWidth(1.0);   // width of each cell in data units
        renderer.setBlockHeight(1.0);
        renderer.setPaintScale(paintScale);

        // Tooltip generator — called automatically on mouseover
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        renderer.setDefaultToolTipGenerator((ds, series, item) -> {
            XYZDataset xyzDs = (XYZDataset)ds;  // safe cast — we know what we put in
            int x = (int)ds.getXValue(series, item);
            double z = xyzDs.getZValue(series, item);
            return String.format("%s: %.0f", monthNames[x], z);
        });

        NumberAxis xAxis = new SymbolAxis(null, monthNames);
        xAxis.setRange(-0.5, 11.5);
        xAxis.setTickLabelsVisible(false);
        xAxis.setMinorTickMarksVisible(false);
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        NumberAxis yAxis = new NumberAxis(null);
        yAxis.setRange(-0.5, 0.5);
        yAxis.setTickLabelsVisible(false);
        yAxis.setMinorTickMarksVisible(false);
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.DARK_GRAY);

        JFreeChart chart = new JFreeChart("", plot);
        chart.getLegend().setVisible(false);
        return chart;
    }

     */

    /**
     * Builds a LookupPaintScale with the given number of color steps in between our COLD color
     * and our HOT color, mapped across the given min and max values. For example, if min=0, max=100, stepCount=5,
     * then the paint scale will have entries at 0, 25, 50, 75, and 100, with colors ranging from COLD at 0 to HOT at
     * 100.
     *
     * @param min       The minimum data value.
     * @param max       The maximum data value.
     * @param stepCount The number of color steps to generate.
     * @return A LookupPaintScale mapping values from min to max to colors from COLD to HOT.
     */
    /*
    private static LookupPaintScale buildPaintScale(int min, int max, int stepCount) {
        LookupPaintScale paintScale = new LookupPaintScale(min, max, Color.BLACK);
        for (int i = 0; i <= stepCount; i++) {
            float t = (float)i / (stepCount - 1);
            Color c = blendColors(t, COLD, HOT);
            paintScale.add(min + t * (max - min), c);
        }
        return paintScale;
    }
    */

    /**
     * Blends the given colors together according to t, which is in the range [0,1]. If t=0, returns the first color.
     * If t=1, returns the last color. If t is in between, returns a blend of the two colors that t falls between.
     * For example, if there are 3 colors and t=0.5, this would return a blend of the second and third colors,
     * since t=0.5 falls halfway between the second and third color stops.
     */
    public static Color blendColors(float t, Color... colors) {
        if (colors.length == 1) { return colors[0]; }
        // t is in [0,1]; map it across the color stops
        float scaled = t * (colors.length - 1);
        int idx = (int)scaled;
        if (idx >= colors.length - 1) { return colors[colors.length - 1]; }
        float local = scaled - idx;
        Color a = colors[idx], b = colors[idx + 1];
        return new Color(
            (int)(a.getRed() + local * (b.getRed() - a.getRed())),
            (int)(a.getGreen() + local * (b.getGreen() - a.getGreen())),
            (int)(a.getBlue() + local * (b.getBlue() - a.getBlue()))
        );
    }
}
