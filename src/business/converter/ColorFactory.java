/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.converter;

import java.awt.Color;
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
        new Color(180,180,180), //0
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
        new Color(Integer.parseInt("0F0F0F", 16)) ///25 Future 3
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
        new Color(Integer.parseInt("000000", 16)) ///25 Future 3
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
        new Color(Integer.parseInt("ED1B23", 16)) ///25 Future 3
    };
//    public static final javafx.scene.paint.Color[] colorFillFx = {
//        new Color(180, 180, 180), //0
//        new Color(255, 0, 0), //1    red A
//        new Color(0, 0, 255), //2    azul B
//        new Color(255, 255, 0), //3  amarelo A
//        new Color(0, 128, 0), //4    verde B
//        new Color(0, 255, 255), //5  azul claro B
//        new Color(255, 110, 110), //6    pink A
//        new Color(255, 128, 0), //7  laranja
//        new Color(128, 255, 0), //8  verde amarelado
//        new Color(128, 128, 0), //9  olive
//        new Color(0, 255, 128), //10 verde claro B
//        new Color(255, 255, 255), //11   branco
//        new Color(128, 0, 255), //12 roxo
//        new Color(0, 128, 255), //13 azul medio
//        new Color(0, 0, 0), //14 preto
//        new Color(25, 25, 25), //15  dark gray
//        new Color(255, 0, 255), //16 Fuchsia
//        new Color(0, 128, 128), //17 teal
//        new Color(255, 215, 32), //18 Gold
//        new Color(218, 165, 32), //19 
//        new Color(180, 0, 128), //20
//        new Color(0, 180, 128), //21
//        new Color(175, 238, 238), //22 paleturquoise
//        new Color(255, 0, 0) //23
//    };
//    public static final javafx.scene.paint.Color[] colorBorderFx = {
//        new Color(Integer.parseInt("999999", 16)), //00 unknown
//        new Color(Integer.parseInt("000000", 16)), //01 KC
//        new Color(Integer.parseInt("000000", 16)), //02 arryn
//        new Color(Integer.parseInt("000000", 16)), //03 baratheon
//        new Color(Integer.parseInt("000000", 16)), //04 greyjoy
//        new Color(Integer.parseInt("000000", 16)), //05 lannister
//        new Color(Integer.parseInt("000000", 16)), //06 martell
//        new Color(Integer.parseInt("000000", 16)), //07 stark
//        new Color(Integer.parseInt("000000", 16)), //08 targaryen
//        new Color(Integer.parseInt("000000", 16)), //09 tully
//        new Color(Integer.parseInt("000000", 16)), //10 tyrell
//        new Color(Integer.parseInt("000000", 16)), //11 nightwatch
//        new Color(Integer.parseInt("000000", 16)), //12 braavos/FC
//        new Color(Integer.parseInt("000000", 16)), //13 wildlings
//        new Color(Integer.parseInt("000000", 16)), //14 Barbarians
//        new Color(Integer.parseInt("000000", 16)), //15 WhiteWalker
//        new Color(Integer.parseInt("000000", 16)), //16 Bolton
//        new Color(Integer.parseInt("000000", 16)), //17 Yronwood
//        new Color(Integer.parseInt("000000", 16)), //18 StormEnd
//        new Color(Integer.parseInt("000000", 16)), //19 Frey
//        new Color(Integer.parseInt("000000", 16)), //20 Hightower
//        new Color(Integer.parseInt("000000", 16)), //21 Volantis
//        new Color(Integer.parseInt("000000", 16)), //22 Pentos
//        new Color(Integer.parseInt("000000", 16)), //23 Future 1
//        new Color(Integer.parseInt("000000", 16)), //24 Future 2
//        new Color(Integer.parseInt("000000", 16)) ///25 Future 3
//    };

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

    public static Color getColorBd(String hexadecimal) {
        if (hexadecimal.isEmpty()) {
            return null;
        } else {
            return new Color(Integer.parseInt(hexadecimal, 16));
        }
    }

    public static String getColorBd(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()).substring(1);
    }
}
