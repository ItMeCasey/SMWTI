package com.itmecasey.smwti.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OverlayPlacementTest {
    private static final int W = 427;
    private static final int H = 240;
    private static final int PANEL_W = 100;
    private static final int PANEL_H = 40;

    @Test
    void aPanelIsKeptAwayFromEveryEdge() {
        int[] corner = OverlayPlacement.constrain(W, H, PANEL_W, PANEL_H, -50, -50);
        assertArrayEquals(new int[] {OverlayPlacement.MARGIN, OverlayPlacement.MARGIN}, corner);
        // Far right and low, beside the hotbar's strip, it stops at the margin on both edges.
        int[] far = OverlayPlacement.constrain(W, H, PANEL_W, PANEL_H, 10_000, 10_000);
        assertEquals(W - PANEL_W - OverlayPlacement.MARGIN, far[0]);
        assertEquals(H - PANEL_H - OverlayPlacement.MARGIN, far[1]);
    }

    @Test
    void aPanelOverTheHotbarStopsAboveItsBars() {
        int middle = (W - PANEL_W) / 2;
        int[] spot = OverlayPlacement.constrain(W, H, PANEL_W, PANEL_H, middle, 10_000);
        assertTrue(spot[1] + PANEL_H <= H - 59 - OverlayPlacement.MARGIN + 1, "bottom edge " + (spot[1] + PANEL_H));
    }

    @Test
    void besideTheHotbarThePanelMayGoAllTheWayDown() {
        int[] spot = OverlayPlacement.constrain(W, H, PANEL_W, PANEL_H, OverlayPlacement.MARGIN, 10_000);
        assertEquals(H - PANEL_H - OverlayPlacement.MARGIN, spot[1]);
    }

    @Test
    void nearTheCenterThePanelClingsToIt() {
        int center = (W - PANEL_W) / 2;
        OverlayPlacement.Snapped snapped = OverlayPlacement.snap(W, H, PANEL_W, PANEL_H, center + 5, 30);
        assertEquals(center, snapped.x());
        assertTrue(snapped.centeredX());
        assertFalse(snapped.centeredY());
    }

    @Test
    void nearACornerThePanelClingsToTheCorner() {
        OverlayPlacement.Snapped snapped = OverlayPlacement.snap(W, H, PANEL_W, PANEL_H, 10, 12);
        assertEquals(OverlayPlacement.MARGIN, snapped.x());
        assertEquals(OverlayPlacement.MARGIN, snapped.y());
    }

    @Test
    void farFromAnythingThePanelStaysWhereItIs() {
        OverlayPlacement.Snapped snapped = OverlayPlacement.snap(W, H, PANEL_W, PANEL_H, 60, 70);
        assertEquals(60, snapped.x());
        assertEquals(70, snapped.y());
    }

    @Test
    void fractionsAndPositionsConvertBothWays() {
        double[] fractions = OverlayPlacement.toFractions(W, H, PANEL_W, PANEL_H, 120, 50);
        int[] back = OverlayPlacement.fromFractions(W, H, PANEL_W, PANEL_H, fractions[0], fractions[1]);
        assertArrayEquals(new int[] {120, 50}, back);
    }

    @Test
    void aPanelBiggerThanTheScreenStillGetsAPosition() {
        int[] spot = OverlayPlacement.constrain(100, 50, 200, 100, 30, 30);
        assertArrayEquals(new int[] {OverlayPlacement.MARGIN, OverlayPlacement.MARGIN}, spot);
    }
}
