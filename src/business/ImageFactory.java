/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business;

import business.facade.ExercitoFacade;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import model.Cenario;
import model.Exercito;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class ImageFactory implements Serializable {

    public static final int HEX_SIZE = 61;
    private static final Log log = LogFactory.getLog(MapaManager.class);
    private Image[] desenhoExercito;
    private final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private final JPanel form;
    private final Cenario cenario;
    private final MediaTracker mt;
    private int mti = 0;
    private final int[][] coordRastros = {{8, 12}, {53, 12}, {60, 30}, {39, 59}, {23, 59}, {0, 30}};
    private final ImageIcon blueBall;
    private final ImageIcon yellowBall;

    /**
     * to be used to draw rastros. don't need cenario, form or mt.
     */
    public ImageFactory() {
        yellowBall = new ImageIcon(getClass().getResource("/images/piemenu/yellow_button.png"));
        blueBall = new ImageIcon(getClass().getResource("/images/piemenu/dark_blue_button.png"));
        this.form = null;
        this.mt = null;
        this.cenario = null;
    }

    /**
     * use to load images
     *
     * @param form
     * @param aCenario
     */
    public ImageFactory(JPanel form, Cenario aCenario) {
        yellowBall = new ImageIcon(getClass().getResource("/images/piemenu/yellow_button.png"));
        blueBall = new ImageIcon(getClass().getResource("/images/piemenu/dark_blue_button.png"));
        this.form = form;
        this.mt = new MediaTracker(form);
        this.cenario = aCenario;
    }

    public void carregaExercito() {
        Image desenho;
        String[] exercitos = getExercitoStrings(false);
        desenhoExercito = new Image[exercitos.length];
        for (int ii = 0; ii < exercitos.length; ii++) {
            desenho = form.getToolkit().getImage(getClass().getResource("/images/armies/" + exercitos[ii]));
            mt.addImage(desenho, mti++);
            this.desenhoExercito[ii] = desenho;
        }
    }

    public Image[] getExercitosAll() {
        Image[] desenhoExercitos;
        Image desenho;
        String[] exercitos = getExercitoStrings(true);
        desenhoExercitos = new Image[exercitos.length];
        for (int ii = 0; ii < exercitos.length; ii++) {
            desenho = form.getToolkit().getImage(getClass().getResource("/images/armies/" + exercitos[ii]));
            mt.addImage(desenho, mti++);
            desenhoExercitos[ii] = desenho;
        }
        waitForAll();
        return desenhoExercitos;
    }

    private String[] getExercitoStrings(boolean all) {
        if (all) {
            return new String[]{"neutral.png", "KingsCourt.gif", "Arryn.png", "Baratheon.gif", "Greyjoy.gif", "Lannister.gif",
                "Martell.png", "Stark.gif", "Targaryen.gif", "Tully.png", "Tyrell.gif",
                "NightsWatch.png",
                "Bolton.gif",
                "Yronwood.gif",
                "Stormend.gif",
                "Frey.gif",
                "Hightower.gif",
                "Volantis.gif",
                "Pentos.gif",
                "Braavos.png",
                "FreeCities.png", "Wildlings.png", "neutral2.png", "neutral3.png",
                "Esparta.gif", "Atenas.gif", "Macedonia.gif", "Persia.gif",
                "Tracia.gif", "Milletus.gif", "Illyria.gif", "Epirus.gif",
                "Twainek.gif", "Frusodian.gif"
            };
        } else if (cenario.isGrecia()) {
            return new String[]{"neutral.png", "Esparta.gif", "Atenas.gif", "Macedonia.gif", "Persia.gif",
                "Tracia.gif", "Milletus.gif", "Illyria.gif", "Epirus.gif"};
        } else if (cenario.isArzhog()) {
            return new String[]{"neutral.png", "Twainek.gif", "Frusodian.gif"};
        } else if (cenario.isGot()) {
            return new String[]{"neutral.png", "KingsCourt.gif", "Arryn.png", "Baratheon.gif", "Greyjoy.gif", "Lannister.gif",
                "Martell.png", "Stark.gif", "Targaryen.gif", "Tully.png", "Tyrell.gif",
                "NightsWatch.png", "FreeCities.png", "Wildlings.png", "neutral2.png", "neutral3.png",
                "Bolton.gif",
                "Yronwood.gif",
                "Stormend.gif",
                "Frey.gif",
                "Hightower.gif",
                "Volantis.gif",
                "Pentos.gif"
            };
        } else {
            return new String[]{"Neutro.gif"};
        }
    }

    public Image getExercito(Exercito exercito) {
        Image img = null;
        try {
            img = this.getExercitos()[exercitoFacade.getNacaoNumero(exercito)];
        } catch (java.lang.ArrayIndexOutOfBoundsException e) {
            img = this.getExercitos()[0];
        }
        return img;
    }

    /**
     * @return the mt
     */
    public void addImage(Image desenho) {
        mt.addImage(desenho, mti++);
    }

    public void waitForAll() {
        try {
            mt.waitForAll();
        } catch (InterruptedException e) {
            log.error("problema na carga de imagens:", e);
        }
        log.debug("Carregados");
    }

    /**
     * @return the desenhoExercito
     */
    public Image[] getExercitos() {
        return desenhoExercito;
    }

    public void doDrawRastro(Graphics2D g, int direcao) {
        doDrawRastro(g, direcao, 0, 0, Color.RED);
    }

    public void doDrawRastro(Graphics2D g, int direcao, Color color) {
        doDrawRastro(g, direcao, 0, 0, color);
    }

    public void doDrawRastro(Graphics2D big, int direcao, int x, int y) {
        doDrawRastro(big, direcao, x, y, Color.RED);
    }

    private void doDrawRastro(Graphics2D big, int direcao, int x, int y, Color color) {
        //setup para os rastros
        big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        big.setStroke(new BasicStroke(
                2f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                1f,
                new float[]{5f},
                0f));
        big.setColor(color);

        //draw path
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x + 38, y + 38);
        path.lineTo(x + coordRastros[direcao - 1][0], y + coordRastros[direcao - 1][1]);

        //draw on graph
        big.draw(path);
    }

    public Icon getBlueBall() {
        return blueBall;
    }

    public Icon getYellowBall() {
        return yellowBall;
    }

    private Path2D.Double createArrow() {
        int length = this.getYellowBall().getIconWidth() - 5;
        int barb = this.getYellowBall().getIconWidth() / 5;
        double angle = Math.toRadians(20);
        Path2D.Double path = new Path2D.Double();
        path.moveTo(-length / 2, 0);
        path.lineTo(length / 2, 0);
        double x = length / 2 - barb * Math.cos(angle);
        double y = barb * Math.sin(angle);
        path.lineTo(x, y);
        x = length / 2 - barb * Math.cos(-angle);
        y = barb * Math.sin(-angle);
        path.moveTo(length / 2, 0);
        path.lineTo(x, y);
        return path;
    }

    public ImageIcon getArrow(double angle) {
        BufferedImage img = new BufferedImage(40, 40, BufferedImage.TRANSLUCENT);
        Graphics2D big = img.createGraphics();
        //setup para os rastros
        big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        big.setStroke(new BasicStroke(
                2f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                1f,
                new float[]{5f},
                0f));
        big.setColor(Color.red);

        int cx = this.getYellowBall().getIconWidth() / 2;
        int cy = this.getYellowBall().getIconHeight() / 2;
        AffineTransform at = AffineTransform.getTranslateInstance(cx, cy);
        at.rotate(Math.toRadians(angle));
//        at.scale(2.0, 2.0);
        Shape shape = at.createTransformedShape(createArrow());
        big.setPaint(Color.red);
        big.draw(shape);
        ImageIcon ret = new ImageIcon(img);
        return (ret);
//        tenta com o icone...angle.
    }
}
