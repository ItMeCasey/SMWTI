package com.itmecasey.smwti.client;

/**
 * Where a freely placed overlay may sit. Everything is in GUI pixels, the panel's size as drawn
 * (already scaled). Three rules keep it usable:
 * <ul>
 *     <li>it keeps {@link #MARGIN} pixels from every edge of the screen, so it never sits flat
 *     against one;</li>
 *     <li>it stays off the hotbar and the bars above it: under that strip of the screen it stops
 *     above them, and beside it, it may go all the way down;</li>
 *     <li>while it is dragged it clings softly (within {@link #SNAP} pixels) to the centers and the
 *     four corners.</li>
 * </ul>
 * The spot is stored as fractions of the room the panel can move in, so it survives a change of
 * window or overlay size.
 */
public final class OverlayPlacement {
    /** Room kept free around the panel on every edge of the screen. */
    public static final int MARGIN = 6;
    /** How close, in pixels, a dragged panel must be to a center or corner before it clings. */
    public static final int SNAP = 8;
    /** Height of the hotbar and the health, food and experience bars above it, from the screen's bottom edge. */
    private static final int HOTBAR_AREA = 59;
    /** Half the width of the hotbar. */
    private static final int HOTBAR_HALF_WIDTH = 91;

    private OverlayPlacement() {
    }

    /** The lowest top edge the panel may have at this horizontal position. */
    private static int lowest(int guiWidth, int guiHeight, int width, int height, int x) {
        int floor = guiHeight - height - MARGIN;
        int left = guiWidth / 2 - HOTBAR_HALF_WIDTH;
        int right = guiWidth / 2 + HOTBAR_HALF_WIDTH;
        boolean overHotbar = x < right && x + width > left;
        return Math.max(MARGIN, overHotbar ? Math.min(floor, guiHeight - HOTBAR_AREA - height - MARGIN) : floor);
    }

    /** Pulls a position inside the margins and off the hotbar. */
    public static int[] constrain(int guiWidth, int guiHeight, int width, int height, int x, int y) {
        int cx = Math.max(MARGIN, Math.min(guiWidth - width - MARGIN, x));
        int cy = Math.max(MARGIN, Math.min(lowest(guiWidth, guiHeight, width, height, cx), y));
        return new int[] {cx, cy};
    }

    /** The position for stored fractions, constrained. */
    public static int[] fromFractions(int guiWidth, int guiHeight, int width, int height, double fractionX, double fractionY) {
        int roomX = Math.max(0, guiWidth - width - 2 * MARGIN);
        int roomY = Math.max(0, guiHeight - height - 2 * MARGIN);
        return constrain(guiWidth, guiHeight, width, height, MARGIN + (int) Math.round(fractionX * roomX),
                MARGIN + (int) Math.round(fractionY * roomY));
    }

    /** The fractions that put the panel at a position; the reverse of {@link #fromFractions}. */
    public static double[] toFractions(int guiWidth, int guiHeight, int width, int height, int x, int y) {
        int roomX = Math.max(0, guiWidth - width - 2 * MARGIN);
        int roomY = Math.max(0, guiHeight - height - 2 * MARGIN);
        return new double[] {roomX == 0 ? 0.0 : clamp01((x - MARGIN) / (double) roomX),
                roomY == 0 ? 0.0 : clamp01((y - MARGIN) / (double) roomY)};
    }

    /** What a snap did, so the screen can show it: the position and whether each center was caught. */
    public record Snapped(int x, int y, boolean centeredX, boolean centeredY) {
    }

    /**
     * Clings a dragged position to the nearest edge margin or center of each axis when it is within
     * {@link #SNAP} pixels, then constrains it. Catching an edge on both axes is catching a corner.
     */
    public static Snapped snap(int guiWidth, int guiHeight, int width, int height, int x, int y) {
        boolean centeredX = false;
        boolean centeredY = false;
        int sx = x;
        int[] xs = {MARGIN, (guiWidth - width) / 2, guiWidth - width - MARGIN};
        for (int i = 0; i < xs.length; i++) {
            if (Math.abs(x - xs[i]) <= SNAP) {
                sx = xs[i];
                centeredX = i == 1;
                break;
            }
        }
        int sy = y;
        int[] ys = {MARGIN, (guiHeight - height) / 2, lowest(guiWidth, guiHeight, width, height, sx)};
        for (int i = 0; i < ys.length; i++) {
            if (Math.abs(y - ys[i]) <= SNAP) {
                sy = ys[i];
                centeredY = i == 1;
                break;
            }
        }
        int[] fixed = constrain(guiWidth, guiHeight, width, height, sx, sy);
        // A center that the constraints moved is not a center any more.
        return new Snapped(fixed[0], fixed[1], centeredX && fixed[0] == sx, centeredY && fixed[1] == sy);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
