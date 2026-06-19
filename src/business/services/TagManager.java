/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.services;

import business.ImageManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import javax.swing.ImageIcon;

/**
 *
 * @author jmoura
 */
public class TagManager implements Serializable {

    private static TagManager instance;
    private final ImageIcon tagImage;

    private TagManager() {
        tagImage = new ImageIcon(getClass().getResource("/images/mapa/hex_tag.gif"));
    }

    public synchronized static TagManager getInstance() {
        if (TagManager.instance == null) {
            TagManager.instance = new TagManager();
        }
        return TagManager.instance;
    }

    public ImageIcon drawTagStyle2(int dx, int dy) {
        return drawTagStyle2(dx, dy, 1.0);
    }

    /** scale > 1 renders the hexagon vector-crisp at the (zoomed) size instead of raster-upscaling. */
    public ImageIcon drawTagStyle2(int dx, int dy, double scale) {
        //create graphs at the target scale
        BufferedImage tagImg = new BufferedImage(
                Math.max(1, (int) Math.ceil((ImageManager.HEX_SIZE + 2 * dx) * scale)),
                Math.max(1, (int) Math.ceil((ImageManager.HEX_SIZE + 2 * dy) * scale)),
                BufferedImage.TRANSLUCENT);
        Graphics2D big = tagImg.createGraphics();
        big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        big.scale(scale, scale); // draw the 1x geometry under a scale transform -> crisp at any zoom
        //setup internal lines
        big.setStroke(new BasicStroke(
                dx,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_MITER));
//                new float[]{5f},
//                0f));
        big.setColor(Color.WHITE);
        Path2D.Double path = new Path2D.Double();
//        path.moveTo(0, 14);
//        int[][] cord = {{30, 0}, {60, 14}, {60, 44}, {30, 60}, {0, 44}, {0, 14}};
        path.moveTo(0 + dx, 15 + dy);
        int[][] cord = {{30 + dx, 0 + dy}, {60 + dx, 15 + dy},
        {60 + dx, 45 + dy}, {30 + dx, 60 + dy},
        {0 + dx, 45 + dy}, {0 + dx, 15 + dy}};

        for (int[] pos : cord) {
            path.lineTo(pos[0], pos[1]);
        }
        big.draw(path);

        //dismiss image to clear memory
        big.dispose();

        ImageIcon ret = new ImageIcon(tagImg);
        return ret;
    }

    public ImageIcon drawTagStyle3(int dx, int dy) {
        return drawTagStyle3(dx, dy, 1.0);
    }

    /** scale > 1 renders the hexagon vector-crisp at the (zoomed) size instead of raster-upscaling. */
    public ImageIcon drawTagStyle3(int dx, int dy, double scale) {
        final int t = 6;
        //creates graphs at the target scale
        BufferedImage tagImg = new BufferedImage(
                Math.max(1, (int) Math.ceil((ImageManager.HEX_SIZE + dx * 2) * scale)),
                Math.max(1, (int) Math.ceil((ImageManager.HEX_SIZE + dy * 2) * scale)),
                BufferedImage.TRANSLUCENT);
        Graphics2D big = tagImg.createGraphics();
        big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        big.scale(scale, scale); // draw the 1x geometry under a scale transform -> crisp at any zoom
        //setup internal lines
        big.setStroke(new BasicStroke(
                2f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_BEVEL,
                1f));
//                new float[]{5f},
//                0f));
        big.setColor(Color.black);
        Path2D.Float path = new Path2D.Float();
//        path.moveTo(0, 15);
//        int[][] cord = {{30, 0}, {60, 15}, {60, 45}, {30, 60}, {0, 45}, {0, 15}};
        path.moveTo(0 + dx, 15 + dy);
        int[][] cordInt = {{30 + dx, 0 + dy}, {60 + dx, 15 + dy},
        {60 + dx, 45 + dy}, {30 + dx, 60 + dy},
        {0 + dx, 45 + dy}, {0 + dx, 15 + dy}};
        for (int[] pos : cordInt) {
            path.lineTo(pos[0], pos[1]);
        }
        big.draw(path);
        //setup external lines
        big.setColor(Color.cyan);
        path = new Path2D.Float();
        big.setStroke(new BasicStroke(
                t - 2,
                BasicStroke.CAP_SQUARE,
                BasicStroke.JOIN_ROUND));
        path.moveTo(0 + dx - t, 15 + dy - t / 2);
        int[][] cordExt = {{30 + dx, 0 + dy - t}, {60 + dx + t, 15 + dy - t / 2},
        {60 + dx + t, 45 + dy + t / 2}, {30 + dx, 60 + dy + t},
        {0 + dx - t, 45 + dy + t / 2}, {0 + dx - t, 15 + dy - t / 2}};
        for (int[] pos : cordExt) {
            path.lineTo(pos[0], pos[1]);
        }
//        path.lineTo(40, 10-t);
//        path.lineTo(71, 24-t);
        big.draw(path);
        //grava numero hex
//        big.setColor(Color.BLACK);
//        big.setFont(new Font("Verdana", Font.PLAIN, 10));
//        big.drawString("Ohay!", 16, 18);

        //dispensa a imagem, liberando memoria
        big.dispose();

        ImageIcon ret = new ImageIcon(tagImg);
        return ret;
    }

    public ImageIcon getTagImage() {
        return tagImage;
    }
}
