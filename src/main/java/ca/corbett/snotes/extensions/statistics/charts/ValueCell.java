package ca.corbett.snotes.extensions.statistics.charts;

import ca.corbett.extras.LookAndFeelManager;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;

/**
 * Represents a single cell in a heatmap chart. This class is purely
 * a UI component - each cell knows nothing of any other cell.
 * All we have here is a String for the tooltip, and color that
 * was presumably calculated based on whatever value we represent.
 * The caller has to figure that out and supply it to us. We just display it.
 * <p>
 * Idea for future development: add a mouse motion listener so we
 * can dynamically change our border when the user hovers over us.
 * We could then add a mouse listener and report click events
 * via a FunctionalInterface to any listeners, so that additional
 * information can be shown in a side panel or something when the user clicks on a cell.
 * Technically, this could all be done by the caller right now, since we extend JPanel,
 * but we could add facilities here to make that much easier.
 * </p>
 * <p>
 *     Note: for this domain, zero is considered a "special" value, meaning "no data".
 *     So, if our value is zero, we ignore the color scale, and instead show
 *     the default panel background color.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class ValueCell extends JPanel {

    /**
     * Default square dimensions for each cell. Caller can override if wanted.
     */
    private static final int CELL_SIZE = 24;

    /**
     * The specific numeric value that we represent.
     * We have no idea what this is! Could be a count of notes,
     * a count of words, or whatever. Doesn't matter here.
     */
    private final int value;

    private final int cellSize;

    public ValueCell(String tooltip, Color color, int value) {
        this(tooltip, color, value, CELL_SIZE);
    }

    /**
     * Creates a ValueCell with the given tooltip and background color.
     */
    public ValueCell(String tooltip, Color color, int value, int cellSize) {
        setToolTipText(tooltip);
        setBackground(color);
        setBorder(BorderFactory.createLoweredBevelBorder());
        this.value = value;

        // Presumably, we will be dropped into a GridBagLayout,
        // which only cares about preferred size, but let's
        // not make any assumptions.
        this.cellSize = cellSize;
        setPreferredSize(new Dimension(cellSize, cellSize));
        setMinimumSize(new Dimension(cellSize, cellSize));
        setMaximumSize(new Dimension(cellSize, cellSize));
    }

    /**
     * Returns the numeric value that we represent.
     * This is caller-defined, and means nothing to us.
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns the effective cell size of this ValueCell.
     */
    public int getCellSize() {
        return cellSize;
    }

    /**
     * Sets our background color based on the given value f, which is in the range [0,1],
     * and the given cold and hot colors. We will interpolate a Color in between the
     * given cold and hot colors based on f, and set that as our background color.
     * For example, a value of 0.5 with colors BLACK,WHITE would set our background to a medium gray.
     * <p>
     * You can retrieve the computed color with getBackground().
     * </p>
     */
    public void setValueColor(float f, Color coldColor, Color hotColor) {
        if (f <= 0.0001f) { // don't do == 0 because of potential floating point weirdness
            // Zero is a special value that means "no data":
            setBackground(LookAndFeelManager.getLafColor("Panel.background", Color.LIGHT_GRAY));
            return;
        }
        setBackground(blendColors(f, coldColor, hotColor));
    }

    /**
     * Blends the given colors together according to t, which is in the range [0,1]. If t=0, returns the first color.
     * If t=1, returns the last color. If t is in between, returns a blend of the two colors that t falls between.
     * For example, if there are 3 colors and t=0.5, this would return a blend of the second and third colors,
     * since t=0.5 falls halfway between the second and third color stops.
     */
    private static Color blendColors(float t, Color... colors) {
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
