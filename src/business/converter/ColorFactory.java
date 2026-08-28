/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.converter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.Serializable;
import javax.swing.JPanel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class ColorFactory implements Serializable {

    private static final Log log = LogFactory.getLog(ColorFactory.class);
    public static final Color[] colorFill = {
        new Color(180, 180, 180), //0
        new Color(255, 0, 0), //1    red A
        new Color(0, 0, 255), //2    azul B
        new Color(255, 255, 0), //3  amarelo A
        new Color(0, 128, 0), //4    verde B
        new Color(0, 255, 255), //5  azul claro B
        new Color(255, 110, 110), //6    pink A
        new Color(255, 128, 0), //7  laranja
        new Color(128, 255, 0), //8  verde amarelado
        new Color(128, 128, 0), //9  olive
        new Color(0, 255, 128), //10 verde claro B
        new Color(255, 255, 255), //11   branco
        new Color(128, 0, 255), //12 roxo
        new Color(0, 128, 255), //13 azul medio
        new Color(0, 0, 0), //14 preto
        new Color(25, 25, 25), //15  dark gray
        new Color(255, 0, 255), //16 Fuchsia
        new Color(0, 128, 128), //17 teal
        new Color(255, 215, 32), //18 Gold
        new Color(218, 165, 32), //19 
        new Color(180, 0, 128), //20
        new Color(0, 180, 128), //21
        new Color(175, 238, 238), //22 paleturquoise
        new Color(255, 0, 0) //23
    };
    public static final Color[] colorFillNew = {
        new Color(Integer.parseInt("AAAAAA", 16)), //00 unknown
        new Color(Integer.parseInt("B4AC32", 16)), //01 KC
        new Color(Integer.parseInt("00ADEF", 16)), //02 arryn
        new Color(Integer.parseInt("FFF100", 16)), //03 baratheon
        new Color(Integer.parseInt("666666", 16)), //04 greyjoy
        new Color(Integer.parseInt("ED1B23", 16)), //05 lannister
        new Color(Integer.parseInt("F48365", 16)), //06 martell
        new Color(Integer.parseInt("777777", 16)), //07 stark
        new Color(Integer.parseInt("000000", 16)), //08 targaryen
        new Color(Integer.parseInt("2E3092", 16)), //09 tully
        new Color(Integer.parseInt("53A175", 16)), //10 tyrell
        new Color(Integer.parseInt("000000", 16)), //11 nightwatch
        new Color(Integer.parseInt("2E3092", 16)), //12 braavos/FC
        new Color(Integer.parseInt("A78B6A", 16)), //13 wildlings
        new Color(Integer.parseInt("FFFFFF", 16)), //14 Barbarians
        new Color(Integer.parseInt("DDDDDD", 16)), //15 WhiteWalker
        new Color(Integer.parseInt("F1709A", 16)), //16 Bolton
        new Color(Integer.parseInt("C96E81", 16)), //17 Yronwood
        new Color(Integer.parseInt("FFCA01", 16)), //18 StormEnd
        new Color(Integer.parseInt("2E3092", 16)), //19 Frey
        new Color(Integer.parseInt("9E76B4", 16)), //20 Hightower
        new Color(Integer.parseInt("FDD09E", 16)), //21 Volantis
        new Color(Integer.parseInt("53C5CF", 16)), //22 Pentos
        new Color(Integer.parseInt("0F0F0F", 16)), //23 Future 1
        new Color(Integer.parseInt("0F0F0F", 16)), //24 Future 2
        new Color(Integer.parseInt("0F0F0F", 16))
    ///25 Future 3
    };
    public static final Color[] colorBorder = {
        new Color(Integer.parseInt("FF0000", 16)), //00 unknown
        new Color(Integer.parseInt("000000", 16)), //01 KC
        new Color(Integer.parseInt("000000", 16)), //02 arryn
        new Color(Integer.parseInt("000000", 16)), //03 baratheon
        new Color(Integer.parseInt("000000", 16)), //04 greyjoy
        new Color(Integer.parseInt("000000", 16)), //05 lannister
        new Color(Integer.parseInt("000000", 16)), //06 martell
        new Color(Integer.parseInt("000000", 16)), //07 stark
        new Color(Integer.parseInt("000000", 16)), //08 targaryen
        new Color(Integer.parseInt("000000", 16)), //09 tully
        new Color(Integer.parseInt("000000", 16)), //10 tyrell
        new Color(Integer.parseInt("000000", 16)), //11 nightwatch
        new Color(Integer.parseInt("000000", 16)), //12 braavos/FC
        new Color(Integer.parseInt("000000", 16)), //13 wildlings
        new Color(Integer.parseInt("000000", 16)), //14 Barbarians
        new Color(Integer.parseInt("000000", 16)), //15 WhiteWalker
        new Color(Integer.parseInt("000000", 16)), //16 Bolton
        new Color(Integer.parseInt("000000", 16)), //17 Yronwood
        new Color(Integer.parseInt("000000", 16)), //18 StormEnd
        new Color(Integer.parseInt("000000", 16)), //19 Frey
        new Color(Integer.parseInt("000000", 16)), //20 Hightower
        new Color(Integer.parseInt("000000", 16)), //21 Volantis
        new Color(Integer.parseInt("000000", 16)), //22 Pentos
        new Color(Integer.parseInt("000000", 16)), //23 Future 1
        new Color(Integer.parseInt("000000", 16)), //24 Future 2
        new Color(Integer.parseInt("000000", 16))
    ///25 Future 3
    };
    public static final Color[] colorBorderNew = {
        new Color(Integer.parseInt("FFFFFF", 16)), //00 unknown
        new Color(Integer.parseInt("000000", 16)), //01 KC
        new Color(Integer.parseInt("000000", 16)), //02 arryn
        new Color(Integer.parseInt("000000", 16)), //03 baratheon
        new Color(Integer.parseInt("ED1B23", 16)), //04 greyjoy
        new Color(Integer.parseInt("000000", 16)), //05 lannister
        new Color(Integer.parseInt("000000", 16)), //06 martell
        new Color(Integer.parseInt("000000", 16)), //07 stark
        new Color(Integer.parseInt("ED1B23", 16)), //08 targaryen
        new Color(Integer.parseInt("000000", 16)), //09 tully
        new Color(Integer.parseInt("000000", 16)), //10 tyrell
        new Color(Integer.parseInt("FFFFFF", 16)), //11 nightwatch
        new Color(Integer.parseInt("ED1B23", 16)), //12 braavos/FC
        new Color(Integer.parseInt("FFFFFF", 16)), //13 wildlings
        new Color(Integer.parseInt("000000", 16)), //14 Barbarians
        new Color(Integer.parseInt("ED1B23", 16)), //15 WhiteWalker
        new Color(Integer.parseInt("ED1B23", 16)), //16 Bolton
        new Color(Integer.parseInt("FFFFFF", 16)), //17 Yronwood
        new Color(Integer.parseInt("ED1B23", 16)), //18 StormEnd
        new Color(Integer.parseInt("FFFFFF", 16)), //19 Frey
        new Color(Integer.parseInt("000000", 16)), //20 Hightower
        new Color(Integer.parseInt("ED1B23", 16)), //21 Volantis
        new Color(Integer.parseInt("ED1B23", 16)), //22 Pentos
        new Color(Integer.parseInt("ED1B23", 16)), //23 Future 1
        new Color(Integer.parseInt("ED1B23", 16)), //24 Future 2
        new Color(Integer.parseInt("ED1B23", 16))

    ///25 Future 3
    };

    public static Image setNacaoColor(Image image, final Color nacaoCor, final Color nacaoBorder, JPanel form) {
        ImageFilter filter = new RGBImageFilter() {
            @Override
            public final int filterRGB(int x, int y, int rgb) {
                switch (rgb) {
                    case -197116:
                        // "recheio"
                        rgb = (new Color(nacaoCor.getRed(), nacaoCor.getGreen(), nacaoCor.getBlue(), 255)).getRGB();
//                    rgb = nacaoCor.getRGB();
                        break;
                    case -16514556:
                        // borda
                        rgb = nacaoBorder.getRGB();
                        break;
                    case 16515588:
                        // fundo
                        //Color col = new Color(255, 0, 0);
                        rgb = (new Color(nacaoCor.getRed(), nacaoCor.getGreen(), nacaoCor.getBlue(), 0)).getRGB();
                        break;
                    default:
                        break;
                }
                return rgb;
            }
        };

        ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
        image = Toolkit.getDefaultToolkit().createImage(ip);
        MediaTracker mt = new MediaTracker(form);
        mt.addImage(image, 0);
        //this.desenhoCPs = desenho;
        try {
            mt.waitForAll();
        } catch (InterruptedException e) {
            log.fatal("Problem", e);
        }
        return image;
    }

    public static Image setWatermarkColor(Image image, int redIncrement, int greenIncrement, int blueIncrement, JPanel form) {
        ImageFilter filter = new RGBImageFilter() {
            @Override
            public int filterRGB(int x,
                    int y,
                    int rgb) {
                int alpha = (rgb & 0xff000000);
                int red = (rgb & 0xff0000) >> 16;
                int green = (rgb & 0x00ff00) >> 8;
                int blue = (rgb & 0x0000ff);

                red = Math.max(0, Math.min(0xff, red + redIncrement));
                green = Math.max(0, Math.min(0xff, green + greenIncrement));
                blue = Math.max(0, Math.min(0xff, blue + blueIncrement));

                return alpha | (red << 16) | (green << 8) | blue;
            }
        };

        ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
        image = Toolkit.getDefaultToolkit().createImage(ip);
        MediaTracker mt = new MediaTracker(form);
        mt.addImage(image, 0);
        //this.desenhoCPs = desenho;
        try {
            mt.waitForAll();
        } catch (InterruptedException e) {
            log.fatal("Problem", e);
        }
        return image;
    }

    /** Below this HSB saturation a house colour carries no usable hue (pure black, white, grey). */
    private static final float PORTRAIT_ACHROMATIC = 0.08f;

    /** Translucent hairline drawn inside the nation frame so a dark border still reads on dark art. */
    private static final Color PORTRAIT_FRAME_SEPARATOR = new Color(255, 255, 255, 90);

    /**
     * Recolours a default portrait to a nation's colours: the house {@code fill} drives the cloak and
     * background through the portrait's grayscale mask, and the {@code border} is drawn as a frame,
     * the same split the map uses (fill = house, border = team).
     * <p>
     * Mask contract, from the art set's README: black preserves the source pixel, white recolours it
     * completely, and intermediate grey blends the two so antialiased edges stay clean. Skin, hair,
     * eyes, armour and weapons are black in the mask and never move.
     * <p>
     * Two colour paths, because <b>the two most common house colours in live games are pure black and
     * pure white</b> and neither has a hue to borrow - swapping hue on them lands on red and makes
     * every black and every white house look identically crimson:
     * <ul>
     * <li><b>Chromatic house</b> - replace the hue, keep the source saturation (scaled, with a floor
     * so dark cloth still reads as coloured) and darken slightly.</li>
     * <li><b>Achromatic house</b> - there is no hue, so drive BRIGHTNESS instead and drop almost all
     * saturation: a black house gets near-black cloth, a white house pale grey. Keeps them apart.</li>
     * </ul>
     *
     * @param portrait the source portrait
     * @param mask the aligned grayscale house mask, or null to skip recolouring entirely
     * @param fill the nation's fill colour, or null to skip recolouring
     * @param border the nation's border colour, or null to draw no frame
     * @param frameWidth border thickness in pixels; 0 or less draws no frame
     * @return a new image; the source is never modified
     */
    public static BufferedImage recolorPortrait(BufferedImage portrait, BufferedImage mask,
            Color fill, Color border, int frameWidth) {
        final int w = portrait.getWidth(), h = portrait.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        final boolean canRecolor = mask != null && fill != null
                && mask.getWidth() == w && mask.getHeight() == h;
        if (canRecolor) {
            final float[] house = Color.RGBtoHSB(fill.getRed(), fill.getGreen(), fill.getBlue(), null);
            final boolean chromatic = house[1] >= PORTRAIT_ACHROMATIC;
            //an achromatic house has only lightness to offer: 0.45x at pure black, ~2.05x at pure white
            final float valueScale = 0.45f + 1.6f * house[2];
            final float[] hsb = new float[3];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    final int src = portrait.getRGB(x, y);
                    final float amount = (mask.getRGB(x, y) & 0xFF) / 255f;
                    if (amount <= 0f) {
                        out.setRGB(x, y, src);
                        continue;
                    }
                    final int sr = (src >> 16) & 0xFF, sg = (src >> 8) & 0xFF, sb = src & 0xFF;
                    Color.RGBtoHSB(sr, sg, sb, hsb);
                    final int tinted;
                    if (chromatic) {
                        tinted = Color.HSBtoRGB(house[0],
                                Math.min(1f, Math.max(0.42f, hsb[1] * 0.95f)),
                                Math.max(0.02f, hsb[2] * 0.72f));
                    } else {
                        tinted = Color.HSBtoRGB(hsb[0], hsb[1] * 0.15f,
                                Math.min(1f, Math.max(0.02f, hsb[2] * valueScale)));
                    }
                    final int tr = (tinted >> 16) & 0xFF, tg = (tinted >> 8) & 0xFF, tb = tinted & 0xFF;
                    out.setRGB(x, y,
                            (Math.round(sr + (tr - sr) * amount) << 16)
                            | (Math.round(sg + (tg - sg) * amount) << 8)
                            | Math.round(sb + (tb - sb) * amount));
                }
            }
        } else {
            out.getGraphics().drawImage(portrait, 0, 0, null);
        }
        if (border != null && frameWidth > 0) {
            Graphics2D g = out.createGraphics();
            g.setColor(border);
            for (int i = 0; i < frameWidth; i++) {
                g.drawRect(i, i, w - 1 - 2 * i, h - 1 - 2 * i);
            }
            //A dark border on dark art is invisible - black is the most common border colour there is,
            //and 13 nations pair it with a black or white fill. One translucent light hairline just
            //inside the frame separates it from the portrait, so the team colour always reads without
            //repainting it into something it is not.
            g.setColor(PORTRAIT_FRAME_SEPARATOR);
            g.drawRect(frameWidth, frameWidth,
                    w - 1 - 2 * frameWidth, h - 1 - 2 * frameWidth);
            g.dispose();
        }
        return out;
    }

    public static Color getColorBd(String hexadecimal) {
        if (hexadecimal.isEmpty()) {
            return null;
        } else {
            try {
                return new Color(Integer.parseInt(hexadecimal, 16));
            } catch (NumberFormatException numberFormatException) {
                return Color.DARK_GRAY;
            }
        }
    }

    public static String getColorBd(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()).substring(1);
    }
}
