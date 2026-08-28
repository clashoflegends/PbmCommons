package business.converter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The default-portrait house recolour. Pins the behaviour that matters: the mask decides what moves,
 * and an ACHROMATIC house colour must not collapse onto red - black and white are the two most common
 * house colours in live games, and a naive hue swap makes both of them crimson.
 */
public class ColorFactoryPortraitTest {

    private static final Color NAVY = new Color(40, 55, 110);

    /** 2x2 portrait of flat navy. */
    private BufferedImage portrait() {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                img.setRGB(x, y, NAVY.getRGB());
            }
        }
        return img;
    }

    /** Mask that recolours only the top-left pixel; the rest stay black = preserve. */
    private BufferedImage mask(int topLeft) {
        BufferedImage m = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        m.setRGB(0, 0, new Color(topLeft, topLeft, topLeft).getRGB());
        return m;
    }

    private float hue(int rgb) {
        return Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null)[0];
    }

    private float brightness(int rgb) {
        return Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null)[2];
    }

    @Test
    public void maskBlackPreservesThePixel() {
        BufferedImage out = ColorFactory.recolorPortrait(portrait(), mask(255), Color.RED, null, 0);
        assertEquals(NAVY.getRGB(), out.getRGB(1, 1), "a black mask pixel must be left alone");
    }

    @Test
    public void chromaticHouseTakesItsHue() {
        BufferedImage out = ColorFactory.recolorPortrait(portrait(), mask(255), Color.GREEN, null, 0);
        float expected = Color.RGBtoHSB(0, 255, 0, null)[0];
        assertEquals(expected, hue(out.getRGB(0, 0)), 0.02f, "a fully masked pixel takes the house hue");
    }

    /** The bug this whole branch exists to prevent. */
    @Test
    public void blackAndWhiteHousesDoNotBothBecomeRed() {
        int black = ColorFactory.recolorPortrait(portrait(), mask(255), Color.BLACK, null, 0).getRGB(0, 0);
        int white = ColorFactory.recolorPortrait(portrait(), mask(255), Color.WHITE, null, 0).getRGB(0, 0);
        assertNotEquals(black, white, "black and white houses must not render identically");
        assertTrue(brightness(black) < brightness(white), "a black house must be darker than a white one");
        assertNotEquals(Color.RGBtoHSB(255, 0, 0, null)[0], hue(black), 0.02f,
                "an achromatic house must not land on red");
    }

    @Test
    public void borderIsDrawnAsAFrame() {
        BufferedImage out = ColorFactory.recolorPortrait(portrait(), mask(0), null, Color.YELLOW, 1);
        assertEquals(Color.YELLOW.getRGB(), out.getRGB(0, 0), "the frame paints the outer ring");
    }

    @Test
    public void noMaskOrNoFillLeavesTheArtAlone() {
        assertEquals(NAVY.getRGB(),
                ColorFactory.recolorPortrait(portrait(), null, Color.RED, null, 0).getRGB(0, 0),
                "no mask means no recolour");
        assertEquals(NAVY.getRGB(),
                ColorFactory.recolorPortrait(portrait(), mask(255), null, null, 0).getRGB(0, 0),
                "no house colour means no recolour");
    }

    /** A distinguishable 4x2 portrait: left half red, right half blue, so a flip is detectable. */
    private BufferedImage sided() {
        BufferedImage img = new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 4; x++) {
                img.setRGB(x, y, (x < 2 ? Color.RED : Color.BLUE).getRGB());
            }
        }
        return img;
    }

    @Test
    public void variantZeroLeavesTheFramingAlone() {
        BufferedImage in = sided();
        BufferedImage out = ColorFactory.recolorPortrait(in, null, null, null, 0, 0);
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 4; x++) {
                assertEquals(in.getRGB(x, y), out.getRGB(x, y), "variant 0 must be a no-op");
            }
        }
    }

    @Test
    public void aFlipVariantMirrorsTheArt() {
        //variant 1 is {zoom 1.0, flip} - the only difference from variant 0 is the mirror
        BufferedImage out = ColorFactory.recolorPortrait(sided(), null, null, null, 0, 1);
        assertEquals(Color.BLUE.getRGB(), out.getRGB(0, 0), "the right half must end up on the left");
        assertEquals(Color.RED.getRGB(), out.getRGB(3, 0), "and the left half on the right");
    }

    @Test
    public void everyVariantKeepsTheOriginalSize() {
        for (int v = 0; v < ColorFactory.PORTRAIT_VARIANT_COUNT; v++) {
            BufferedImage out = ColorFactory.recolorPortrait(portrait(), mask(255), Color.GREEN, null, 0, v);
            assertEquals(2, out.getWidth(), "variant " + v + " changed the width");
            assertEquals(2, out.getHeight(), "variant " + v + " changed the height");
        }
    }

    @Test
    public void outOfRangeVariantDegradesToNoOp() {
        BufferedImage out = ColorFactory.recolorPortrait(sided(), null, null, null, 0, 999);
        assertEquals(Color.RED.getRGB(), out.getRGB(0, 0), "an unknown variant must not throw or mirror");
    }
}
