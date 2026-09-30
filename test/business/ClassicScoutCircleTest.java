package business;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The classic scout marker, pinned to the geometry it has had since 2022.
 *
 * <h3>Why this test exists</h3>
 *
 * The marker was removed in September 2026 in favour of the vector footprint overlay, and restored
 * when a player asked for it back: it had been in front of every player for four years and they read
 * it fluently. It is now one of three choices under {@code mapOverlayStyle}.
 *
 * <p>
 * "Restored exactly" is the whole point of it, so what it draws is asserted rather than assumed. The
 * inner oval sits ON the hex at {@code HEX_SIZE} across and the outer one is half again as wide, drawn
 * a quarter-hex up and left so the two stay concentric. Anyone who "tidies" those two {@code drawOval}
 * calls into one, or rounds the offsets differently, changes a picture players navigate by.
 *
 * <p>
 * Allies are deliberately drawn in the SAME colour as your own scouts. That was true of the original
 * and is kept on purpose: the ally case is part of what was restored, not a bug found along the way.
 */
public class ClassicScoutCircleTest {

    /** Room for the outer oval plus a wide margin, so nothing is clipped by the canvas. */
    private static final int CANVAS = ImageManager.HEX_SIZE * 4;
    private static final Point AT = new Point(ImageManager.HEX_SIZE, ImageManager.HEX_SIZE);

    private static BufferedImage draw(boolean ally) {
        final BufferedImage img = new BufferedImage(CANVAS, CANVAS, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = img.createGraphics();
        try {
            if (ally) {
                ImageManager.getInstance().doDrawScoutAlly(g, AT);
            } else {
                ImageManager.getInstance().doDrawScout(g, AT);
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    /** Bounding box of everything that got painted, or null if nothing did. */
    private static int[] inkBounds(BufferedImage img) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) == 0) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        return maxX < 0 ? null : new int[]{minX, minY, maxX, maxY};
    }

    @Test
    public void theMarkerCoversTheOuterOvalNotJustTheHex() {
        final int[] ink = inkBounds(draw(false));
        assertTrue(ink != null, "the classic marker drew nothing at all");
        // Outer oval: origin a quarter-hex up and left, 1.5 hexes across. Antialiasing and the 1.75f
        // stroke spread the ink about a pixel either side, so the box is checked with that tolerance
        // rather than exactly - the point is the SIZE, which a single-oval "tidy-up" would halve.
        final int expectedX = AT.x - ImageManager.HEX_SIZE / 4;
        final int expectedY = AT.y - ImageManager.HEX_SIZE / 4;
        final int expectedSpan = ImageManager.HEX_SIZE * 3 / 2;
        assertTrue(Math.abs(ink[0] - expectedX) <= 2,
                "outer oval starts at x=" + ink[0] + ", expected about " + expectedX);
        assertTrue(Math.abs(ink[1] - expectedY) <= 2,
                "outer oval starts at y=" + ink[1] + ", expected about " + expectedY);
        assertTrue(Math.abs((ink[2] - ink[0]) - expectedSpan) <= 3,
                "outer oval is " + (ink[2] - ink[0]) + " wide, expected about " + expectedSpan);
        assertTrue(Math.abs((ink[3] - ink[1]) - expectedSpan) <= 3,
                "outer oval is " + (ink[3] - ink[1]) + " tall, expected about " + expectedSpan);
    }

    @Test
    public void theMarkerIsDashedAndLeavesTheMiddleAlone() {
        final BufferedImage img = draw(false);
        // Centre of the hex: both ovals are outlines, so the middle must stay clear of ink or the
        // marker would hide the terrain and the army icons it is drawn over.
        final int cx = AT.x + ImageManager.HEX_SIZE / 2;
        final int cy = AT.y + ImageManager.HEX_SIZE / 2;
        assertEquals(0, img.getRGB(cx, cy) >>> 24, "the marker filled the hex instead of outlining it");
        // Dashed, not solid: walking the inner oval's horizontal midline must cross ink and gaps both.
        boolean sawInk = false, sawGap = false;
        for (int x = AT.x; x <= AT.x + ImageManager.HEX_SIZE; x++) {
            if ((img.getRGB(x, cy) >>> 24) != 0) {
                sawInk = true;
            } else {
                sawGap = true;
            }
        }
        assertTrue(sawInk, "no ink along the marker's midline");
        assertTrue(sawGap, "the marker is solid - the dash pattern is gone");
    }

    @Test
    public void alliedScoutsWearTheSameColourAsYourOwn() {
        // Kept from the original on purpose. A player reading the map since 2022 has never had the two
        // told apart here, and "restore it exactly" includes this.
        final int[] mine = inkBounds(draw(false));
        final int[] ally = inkBounds(draw(true));
        assertTrue(mine != null && ally != null, "one of the two markers drew nothing");
        assertEquals(java.util.Arrays.toString(mine), java.util.Arrays.toString(ally),
                "the allied marker no longer matches your own");
        // and it really is the blue the rest of the map uses for your own drawings
        final BufferedImage img = draw(true);
        boolean sawBlue = false;
        for (int x = mine[0]; x <= mine[2] && !sawBlue; x++) {
            for (int y = mine[1]; y <= mine[3]; y++) {
                if (img.getRGB(x, y) == Color.BLUE.getRGB()) {
                    sawBlue = true;
                    break;
                }
            }
        }
        assertTrue(sawBlue, "the classic marker is no longer drawn in blue");
    }

    @Test
    public void theThreeOverlayStylesAreDistinct() {
        assertEquals("mapOverlayStyle", MapaManager.MAP_OVERLAY_STYLE);
        assertEquals(3, new java.util.HashSet<>(java.util.Arrays.asList(
                MapaManager.OVERLAY_STYLE_ANIMATED,
                MapaManager.OVERLAY_STYLE_STATIC,
                MapaManager.OVERLAY_STYLE_CIRCLE)).size(),
                "two overlay styles share a stored value, so one of them is unreachable");
    }
}
