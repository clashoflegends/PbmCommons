/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package business;

import business.facade.ExercitoFacade;
import business.facade.LocalFacade;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import model.Cenario;
import model.Exercito;
import model.Habilidade;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SettingsManager;

/**
 *
 * @author jmoura
 */
public class ImageManager implements Serializable {

    public static final int HEX_SIZE = 61;
    private static final Log log = LogFactory.getLog(ImageManager.class);
    private Image[] desenhoExercito;
    private Image[] terrainImages;
    private final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private final LocalFacade localFacade = new LocalFacade();
    private JPanel form;
    private Cenario cenario;
    private final MediaTracker mt;
    private int mti = 0;
    private ImageIcon combat, combatBigArmy, combatBigNavy, explosion, blueBall, yellowBall, iconApp, caravanIcon, pieChart, barChart, areaChart;
    private final int[][] coordRastros = {{8, 12}, {53, 12}, {60, 30}, {39, 59}, {23, 59}, {0, 30}};
    private final SortedMap<String, ImageIcon> landmarks = new TreeMap<>();
    private static ImageManager instance;
    private final SortedMap<String, ImageIcon> portraitMap = new TreeMap<>();
    private final Color colorNpc = Color.GREEN;
    private final Color colorEnemy = new Color(204, 43, 51, 169);
    private final Color colorMine = Color.BLUE;
    private final Color colorAlly = Color.CYAN;
    private final Color colorMineOrdem = Color.BLUE.brighter();
    private final Color colorAllyOrdem = Color.CYAN.brighter();
    private final Color colorResourceTransportOrdem = new Color(255, 215, 0); //(253, 208, 23) 255, 215, 0)
    private final Color colorMineArmyOrdem = Color.BLUE.darker();
    private final Color colorAllyArmyOrdem = Color.CYAN.brighter().brighter();
    private final AlphaComposite alcom = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

    /**
     * to be used to draw rastros. don't need cenario.
     */
    private ImageManager() {
        doLoadIconsAll();
        this.form = new JPanel();
        this.mt = new MediaTracker(this.form);
        this.cenario = null;
        doLoadTerrainImages();
        waitForAll();
    }

    /**
     * use to load images
     *
     * @param form
     * @param aCenario
     */
    public ImageManager(JPanel form, Cenario aCenario) {
        doLoadIconsAll();
        this.form = form;
        this.mt = new MediaTracker(form);
        this.cenario = aCenario;
    }

    public synchronized static ImageManager getInstance() {
        if (ImageManager.instance == null) {
            ImageManager.instance = new ImageManager();
        }
        return ImageManager.instance;
    }

    private void doLoadIconsAll() {
        yellowBall = new ImageIcon(getClass().getResource("/images/piemenu/yellow_button.png"));
        blueBall = new ImageIcon(getClass().getResource("/images/piemenu/dark_blue_button.png"));
        combat = new ImageIcon(getClass().getResource("/images/combat.png"));
        combatBigNavy = new ImageIcon(getClass().getResource("/images/combat_blue.png"));
        combatBigArmy = new ImageIcon(getClass().getResource("/images/combat_green.png"));
        explosion = new ImageIcon(getClass().getResource("/images/explosion.png"));
        iconApp = new ImageIcon(getClass().getResource("/images/hex_wasteland.png"));
        caravanIcon = new ImageIcon(getClass().getResource("/images/mapa/hex_goldmine.gif"));
        pieChart = new ImageIcon(getClass().getResource("/images/pie-chart-icon.png"));
        barChart = new ImageIcon(getClass().getResource("/images/bargraph icon.png"));
        areaChart = new ImageIcon(getClass().getResource("/images/areagraph icon.png"));
        doLoadFeaturesAll();
    }

    private void doLoadFeaturesAll() {
        final SortedMap<String, String> featuresImage = localFacade.getTerrainLandmarksImage();
        for (String cdFeature : featuresImage.keySet()) {
            //todo: link feature to image vector
            landmarks.put(cdFeature, new ImageIcon(getClass().getResource(featuresImage.get(cdFeature))));
        }
    }

    public Image[] getTerrainImages() {
        return terrainImages;
    }

    private JPanel getForm() {
        return form;
    }

    public void setForm(JPanel form) {
        this.form = form;
    }

    private Cenario getCenario() {
        return cenario;
    }

    public void setCenario(Cenario cenario) {
        this.cenario = cenario;
    }

    public void carregaExercito() {
        Image desenho;
        String[] exercitos = getExercitoStrings(false);
        desenhoExercito = new Image[exercitos.length];
        for (int ii = 0; ii < exercitos.length; ii++) {
            try {
                desenho = getForm().getToolkit().getImage(getClass().getResource("/images/armies/" + exercitos[ii]));
                mt.addImage(desenho, mti++);
                this.desenhoExercito[ii] = desenho;
            } catch (NullPointerException ex) {
                log.fatal("Fail to load image: /images/armies/" + exercitos[ii]);
                throw new UnsupportedOperationException("Fail to load image: /images/armies/" + exercitos[ii], ex);
            }
        }
    }

    public Image[] getExercitosAll() {
        Image[] desenhoExercitos;
        Image desenho;
        String[] exercitos = getExercitoStrings(true);
        desenhoExercitos = new Image[exercitos.length];
        for (int ii = 0; ii < exercitos.length; ii++) {
            desenho = getForm().getToolkit().getImage(getClass().getResource("/images/armies/" + exercitos[ii]));
//            log.info(String.format("ArmyImg: %s %s", ii, exercitos[ii]));
            mt.addImage(desenho, mti++);
            desenhoExercitos[ii] = desenho;
        }
        waitForAll();
        return desenhoExercitos;
    }

    private String[] getExercitoStrings(boolean all) {
        if (all || this.getCenario() == null) {
            return new String[]{"neutral.png", "KingsCourt.gif", "Jofrey.png",
                "Arryn.png", "Baratheon.gif", "Greyjoy.gif", "Lannister.gif",
                "Martell.png", "Stark.gif", "Targaryen.gif", "Tully.png", "Tyrell.gif",
                "NightsWatch.png",
                "Bolton.png",
                "Yronwood.png",
                "Stannis.gif",
                "Frey.png",
                "Hightower.gif",
                "Volantis.png",
                "Pentos.png",
                "Braavos.png",
                "FreeCities.png", "Wildlings.png", "neutral2.png", "neutral3.png",
                "Esparta.gif", "Atenas.gif", "Macedonia.gif", "Persia.gif",
                "Tracia.gif", "Milletus.gif", "Illyria.gif", "Epirus.gif",
                "Twainek.gif", "Frusodian.gif",
                "lom_corelay.png",
                "lom_ashimar.png",
                "lom_gloom.png",
                "lom_dregrim.png",
                "lom_despair.png",
                "lom_korkithdodrak.png",
                "lom_valethor.png",
                "lom_kor.png"
            };
        } else if (getCenario().isGrecia()) {
            return new String[]{"neutral.png", "Esparta.gif", "Atenas.gif", "Macedonia.gif", "Persia.gif",
                "Tracia.gif", "Milletus.gif", "Illyria.gif", "Epirus.gif"};
        } else if (getCenario().isArzhog()) {
            return new String[]{"neutral.png", "Twainek.gif", "Frusodian.gif"};
        } else if (getCenario().isGot()) {
            return new String[]{"neutral.png", "KingsCourt.gif", "Arryn.png", "Baratheon.gif", "Greyjoy.gif", "Lannister.gif",
                "Martell.png", "Stark.gif", "Targaryen.gif", "Tully.png", "Tyrell.gif",
                "NightsWatch.png", "FreeCities.png", "Wildlings.png", "neutral2.png", "neutral3.png",
                "Bolton.png",
                "Yronwood.png",
                "Stannis.gif",
                "Frey.png",
                "Hightower.gif",
                "Volantis.png",
                "Pentos.png",
                "Jofrey.png"
            };
        } else if (getCenario().isFirstAge()) {
            return new String[]{"neutral.png",
                "wdor_gundabad.png",
                "wdor_methedras.gif",
                "wdor_carndun.gif",
                "wdor_morannon.png",
                "wdob_rivendell.png",
                "wdob_dorwinion.gif",
                "wdob_halfling.png",
                "wdob_lorien.gif",
                "wdob_silvan.png",
                "wdob_blackhammer.png",
                "wdob_woodmen.png",
                "wdob_beornings.png",
                "wdon_neutral1.gif"
            };
        } else if (getCenario().isWdo()) {
            return new String[]{"neutral.png", "wdor_carndun.gif", "wdor_gram.gif",
                "wdor_gundabad.png", "wdor_highpass.gif", "wdor_moria.png", "wdor_methedras.gif",
                "wdor_morannon.png", "wdor_dolguldur.png", "wdor_wargriders.gif", "wdor_grey.png", "wdob_rangers.png",
                "wdob_rivendell.png",
                "wdob_beornings.png",
                "wdob_silvan.png",
                "wdob_menlake.png",
                "wdob_longbeard.gif",
                "wdob_broadbeams.png",
                "wdob_stonefist.png",
                "wdob_ironhelm.png",
                "wdob_blackhammer.png",
                "wdob_dorwinion.gif",
                "wdob_halfling.png",
                "wdob_lindon.png",
                "wdob_lorien.gif",
                "wdob_lossoth.png", "wdob_northmen.png", "wdob_rohan.gif", "wdob_woodmen.png",
                "wdob_woses.png", "wdor_baltoch.gif", "wdor_druedain.gif", "wdor_forochel.gif",
                "wdor_mistygoblin.gif", "wdor_northorcs.png", "wdor_raiders.gif", "wdor_variags.gif",
                "wdor_wainriders.gif", "wdon_neutral1.gif"
            };
        } else if (getCenario().isLom()) {
            return new String[]{"neutral.png",
                "lom_corelay.png",
                "lom_ashimar.png",
                "lom_gloom.png",
                "lom_dregrim.png",
                "lom_despair.png",
                "lom_korkithdodrak.png",
                "lom_valethor.png",
                "lom_kor.png",
                "neutral2.png",
                "neutral3.png",
                "lom_wolf.png",
                "lom_skulkrin.png",
                "lom_icetrolls.png",
                "lom_dragon.png"
            };
        } else {
            return new String[]{"neutral.png", "KingsCourt.gif", "Arryn.png", "Baratheon.gif", "Greyjoy.gif", "Lannister.gif",
                "Martell.png", "Stark.gif", "Targaryen.gif", "Tully.png", "Tyrell.gif",
                "NightsWatch.png", "FreeCities.png", "Wildlings.png", "neutral2.png", "neutral3.png",
                "Bolton.png",
                "Yronwood.png",
                "Stannis.gif",
                "Frey.png",
                "Hightower.gif",
                "Volantis.png",
                "Pentos.png",
                "Jofrey.png",
                "army1.png",
                "army2.png",
                "army3.png",
                "Esparta.gif", "Atenas.gif", "Macedonia.gif", "Persia.gif",
                "Tracia.gif", "Milletus.gif", "Illyria.gif", "Epirus.gif",
                "Twainek.gif", "Frusodian.gif",
                "shield.jpg"
            };
//            return new String[]{"neutral.png"};
        }
    }

    public Image getExercito(Exercito exercito) {
        Image img;
        try {
            img = this.getExercitos()[exercitoFacade.getNacaoNumero(exercito)];
        } catch (java.lang.ArrayIndexOutOfBoundsException e) {
            img = this.getExercitos()[0];
        }
        return img;
    }

    public void addImage(Image desenho) {
        mt.addImage(desenho, mti++);
    }

    public final void waitForAll() {
        try {
            mt.waitForAll();
        } catch (NullPointerException | InterruptedException e) {
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

    public void doDrawScout(Graphics2D big, Point dest) {
        doDrawCircle(big, dest, colorMine);
    }

    public void doDrawScoutAlly(Graphics2D big, Point dest) {
        doDrawCircle(big, dest, colorMine);
    }

    public void doDrawCircle(Graphics2D big, Point dest, Color color) {
        big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        big.setStroke(new BasicStroke(
                1.75f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                5f,
                new float[]{3f, 5f, 7f, 5f, 11f, 5f, 15f, 5f, 21f, 5f, 27f, 5f, 33f, 5f},
                0f));
        big.setColor(color);

        //draw on graph
        big.drawOval(dest.x, dest.y, HEX_SIZE, HEX_SIZE);
        big.drawOval(dest.x - HEX_SIZE / 4, dest.y - HEX_SIZE / 4, HEX_SIZE * 3 / 2, HEX_SIZE * 3 / 2);
    }

    public void doDrawCircle(Graphics2D big, int x, int y, Color color) {
        //setup para os rastros
        big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        big.setStroke(new BasicStroke());
        big.setColor(color);

        //draw on graph
        big.drawOval(x, x, y, y);
    }

    public void doDrawPathNpc(Graphics2D big, Point ori, Point dest) {
        final int x = 04 + 7 / 2 + 8;
        final int y = 22 + 13 / 2 - 2;
        doDrawPath(big,
                new Point((int) ori.getX() + x, (int) ori.getY() + y),
                new Point((int) dest.getX() + x, (int) dest.getY() + y),
                colorNpc);
    }

    public void doDrawPathPcEnemy(Graphics2D big, Point ori, Point dest) {
        final int x = 04 + 7 / 2 + 6;
        final int y = 22 + 13 / 2 - 3 + 4;
        doDrawPath(big,
                new Point((int) ori.getX() + x, (int) ori.getY() + y),
                new Point((int) dest.getX() + x, (int) dest.getY() + y),
                colorEnemy);
    }

    public void doDrawPathPc(Graphics2D big, Point ori, Point dest) {
        final int x = 04 + 7 / 2;
        final int y = 22 + 13 / 2;
        doDrawPath(big,
                new Point((int) ori.getX() + x, (int) ori.getY() + y),
                new Point((int) dest.getX() + x, (int) dest.getY() + y),
                colorMine);
    }

    public void doDrawPathPcAlly(Graphics2D big, Point ori, Point dest) {
        final int x = 04 + 7 / 2 + 4;
        final int y = 22 + 13 / 2 - 3 + 2;
        doDrawPath(big,
                new Point((int) ori.getX() + x, (int) ori.getY() + y),
                new Point((int) dest.getX() + x, (int) dest.getY() + y),
                colorAlly);
    }

    public void doDrawPathPcOrder(Graphics2D big, Point ori, Point dest) {
        final int x = 04 + 7 / 2 - 4;
        final int y = 22 + 13 / 2 + 2;
        doDrawPathOrdem(big,
                new Point((int) ori.getX() + x, (int) ori.getY() + y),
                new Point((int) dest.getX() + x, (int) dest.getY() + y),
                colorMineOrdem);
    }

    public void doDrawPathPcAllyOrder(Graphics2D big, Point ori, Point dest) {
        final int x = 04 + 7 / 2 + 4 + 4;
        final int y = 22 + 13 / 2 - 3 - 2;
        doDrawPathOrdem(big,
                new Point((int) ori.getX() + x, (int) ori.getY() + y),
                new Point((int) dest.getX() + x, (int) dest.getY() + y),
                colorAllyOrdem);
    }

    public void doDrawPathResourceTransportOrder(Graphics2D big, Point ori, Point dest, Color nationColor) {
        final int x = 04 + 7 / 2 + 4 + 4;
        final int y = 22 + 13 / 2 - 3 - 2;
        doDrawLineOrdem(big,
                new Point((int) ori.getX() + x, (int) ori.getY() + y),
                new Point((int) dest.getX() + x, (int) dest.getY() + y),
                colorResourceTransportOrdem, nationColor);
    }

    public void doDrawPathArmyOrder(Graphics2D big, Point ori, Point dest) {
        final int x = 30;
        final int y = 30;
        doDrawPathOrdemArmy(big,
                new Point((int) ori.getX() + x, (int) ori.getY() + y),
                new Point((int) dest.getX() + x, (int) dest.getY() + y),
                colorMineArmyOrdem);
    }

    public void doDrawPathArmyAllyOrder(Graphics2D big, Point ori, Point dest) {
        final int x = 45;
        final int y = 45;
        doDrawPathOrdemArmy(big,
                new Point((int) ori.getX() + x, (int) ori.getY() + y),
                new Point((int) dest.getX() + x, (int) dest.getY() + y),
                colorAllyArmyOrdem);
    }

    private void doDrawPathOrdemArmy(Graphics2D big, Point ori, Point dest, Color color) {
        //setup para os rastros
        big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        big.setComposite(alcom);
        big.setStroke(new BasicStroke(
                3f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                1f,
                new float[]{3f, 5f, 7f, 5f, 11f, 5f, 15f, 5f, 21f, 5f, 27f, 5f, 33f, 5f},
                0f));
        big.setColor(color);
        //draw path
        Path2D.Double path = new Path2D.Double();
        path.moveTo(ori.getX(), ori.getY());
        path.curveTo(dest.getX() + 10, dest.getY() - 10, dest.getX() - 10, dest.getY() + 10, dest.getX(), dest.getY());

        //draw on graph
        big.draw(path);
    }

    private void doDrawPathOrdem(Graphics2D big, Point ori, Point dest, Color color) {
        //setup para os rastros
        big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        big.setStroke(new BasicStroke(
                1.75f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                1f,
                new float[]{3f, 5f, 7f, 5f, 11f, 5f, 15f, 5f, 21f, 5f, 27f, 5f, 33f, 5f},
                0f));
        big.setColor(color);
        //draw path
        Path2D.Double path = new Path2D.Double();
        path.moveTo(ori.getX(), ori.getY());
        path.curveTo(dest.getX() - 20, dest.getY() + 20, dest.getX() + 20, dest.getY() - 20, dest.getX() + 12, dest.getY());
        //path.lineTo(dest.getX(), dest.getY());

        //draw on graph
        big.draw(path);
    }

    private void doDrawLineOrdem(Graphics2D big, Point ori, Point dest, Color color, Color nationColor) {
        //setup the line pattern
        big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        BasicStroke basicStroke = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f);
        big.setStroke(basicStroke);
        big.setColor(color);

        //find size of the box for a smooth s-cruve style
        double xLength = dest.getX() - ori.getX();
        double yLength = (dest.getY() - ori.getY());
        //increase exageration for a nicer curve when distances are small
        double exageration = 10d;
        if (yLength / xLength > 5d) {
            xLength *= exageration;
        } else if (xLength / yLength > 5d) {
            yLength *= exageration;
        } else if (xLength < 50d) {
            xLength = yLength / 5d;
        } else if (Math.abs(yLength) < 50d) {
            yLength = xLength / 5d;
        }

        //draw S-curve
        // Define the points for the s-curve using a GeneralPath
        final GeneralPath sCurve = new GeneralPath();

        // Starting point of the curve
        sCurve.moveTo(ori.getX(), ori.getY());

        // Add a cubic Bézier curve segment to form an 'S' shape.
        // The method signature is curveTo(controlPt1X, controlPt1Y, controlPt2X, controlPt2Y, endPtX, endPtY)
        // 
        // For a smooth S-curve:
        // Control Point 1 pulls the curve down and right
        // Control Point 2 pulls the curve up and right
        // End Point is the final destination
        sCurve.curveTo(ori.getX() + xLength / 3, ori.getY() - yLength / 3,
                dest.getX() - xLength / 3, dest.getY() + yLength / 3,
                dest.getX() + 30, dest.getY() + 20);
        big.draw(sCurve);

        //draw 2nd S-curve to overlay colors
        basicStroke = new BasicStroke(5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[]{3f, 4f, 5f}, 50f);
        big.setStroke(basicStroke);
        big.setColor(nationColor);
        sCurve.moveTo(ori.getX(), ori.getY());
        sCurve.curveTo(ori.getX() + xLength / 3, ori.getY() - yLength / 3,
                dest.getX() - xLength / 3, dest.getY() + yLength / 3,
                dest.getX() + 30, dest.getY() + 20);
        big.draw(sCurve);
    }

    private void doDrawPath(Graphics2D big, Point ori, Point dest, Color color) {
        //setup para os rastros
        big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        big.setStroke(new BasicStroke(
                0.75f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                1f,
                new float[]{3f, 5f, 7f, 5f, 11f, 5f, 15f, 5f, 21f, 5f, 27f, 5f, 33f, 5f},
                0f));
        big.setColor(color);
        //draw path
        Path2D.Double path = new Path2D.Double();
        path.moveTo(ori.getX(), ori.getY());
        path.lineTo(dest.getX(), dest.getY());

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

    public ImageIcon getTeaser() {
        List<String> listTeaser = new ArrayList<>();
        listTeaser.add("/images/teaser/Walker_image.png");
        listTeaser.add("/images/teaser/Esparta_image.png");
        listTeaser.add("/images/teaser/Hobbit_image.png");
        listTeaser.add("/images/teaser/All3_image.png");
        Collections.shuffle(listTeaser);
        final ImageIcon ret = new ImageIcon(getClass().getResource(listTeaser.get(0)));
        return (ret);
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

    public void doDrawCaravanIcon(Graphics2D big, Point dest) {
        final int x = (int) dest.getX(), y = (int) dest.getY();
        int dx = 40;
        int dy = 36;
        big.drawImage(this.getCaravanIcon(), x + dx, y + dy, form);
    }

    public Image doDrawCombat() {
        return this.combat.getImage();
    }

    public Image doDrawCombatBigNavy() {
        return this.combatBigNavy.getImage();
    }

    public Image getCaravanIcon() {
        return this.caravanIcon.getImage();
    }

    public Image doDrawCombatBigArmy() {
        return this.combatBigArmy.getImage();
    }

    public Image doDrawExplosion() {
        return this.explosion.getImage();
    }

    public Image getIconApp() {
        return this.iconApp.getImage();
    }

    public Image getPieChartIcon() {
        return this.pieChart.getImage();
    }

    public Image getAreaChartIcon() {
        return this.areaChart.getImage();
    }

    public Image getBarChartIcon() {
        return this.barChart.getImage();
    }

    public Image getFeature(Habilidade feature) {
        try {
            return this.landmarks.get(feature.getCodigo()).getImage();
        } catch (NullPointerException ex) {
            log.error("Feature not found, add it to LocalFacade: " + feature.getCodigo());
            throw new UnsupportedOperationException(ex);
        }
    }

    public final Image[] doLoadTerrainImages() {
        Image desenho;
        String[] terrenos = {"vazio", "mar", "costa", "litoral", "floresta", "planicie",
            "montanha", "colinas", "pantano", "deserto", "wasteland", "lago"
        };

        terrainImages = new Image[terrenos.length];
        for (int ii = 0; ii < terrenos.length; ii++) {
            if (terrenos[ii].equals("mar") && SettingsManager.getInstance().isKeyExist("ImagemMar")) {
                desenho = getForm().getToolkit().getImage(SettingsManager.getInstance().getConfig("ImagemMar"));
            } else if (terrenos[ii].equals("costa") && SettingsManager.getInstance().isKeyExist("ImagemCosta")) {
                desenho = getForm().getToolkit().getImage(SettingsManager.getInstance().getConfig("ImagemCosta"));
            } else if (terrenos[ii].equals("litoral") && SettingsManager.getInstance().isKeyExist("ImagemLitoral")) {
                desenho = getForm().getToolkit().getImage(SettingsManager.getInstance().getConfig("ImagemLitoral"));
            } else if (terrenos[ii].equals("floresta") && SettingsManager.getInstance().isKeyExist("ImagemFloresta")) {
                desenho = getForm().getToolkit().getImage(SettingsManager.getInstance().getConfig("ImagemFloresta"));
            } else if (terrenos[ii].equals("planicie") && SettingsManager.getInstance().isKeyExist("ImagemPlanicie")) {
                desenho = getForm().getToolkit().getImage(SettingsManager.getInstance().getConfig("ImagemPlanicie"));
            } else if (terrenos[ii].equals("montanha") && SettingsManager.getInstance().isKeyExist("ImagemMontanha")) {
                desenho = getForm().getToolkit().getImage(SettingsManager.getInstance().getConfig("ImagemMontanha"));
            } else if (terrenos[ii].equals("colinas") && SettingsManager.getInstance().isKeyExist("ImagemColinas")) {
                desenho = getForm().getToolkit().getImage(SettingsManager.getInstance().getConfig("ImagemColinas"));
            } else if (terrenos[ii].equals("pantano") && SettingsManager.getInstance().isKeyExist("ImagemPantano")) {
                desenho = getForm().getToolkit().getImage(SettingsManager.getInstance().getConfig("ImagemPantano"));
            } else if (terrenos[ii].equals("deserto") && SettingsManager.getInstance().isKeyExist("ImagemDeserto")) {
                desenho = getForm().getToolkit().getImage(SettingsManager.getInstance().getConfig("ImagemDeserto"));
            } else if (terrenos[ii].equals("wasteland") && SettingsManager.getInstance().isKeyExist("ImagemWasteland")) {
                desenho = getForm().getToolkit().getImage(SettingsManager.getInstance().getConfig("ImagemWasteland"));
            } else if (terrenos[ii].equals("lago") && SettingsManager.getInstance().isKeyExist("ImagemLago")) {
                desenho = getForm().getToolkit().getImage(SettingsManager.getInstance().getConfig("ImagemLago"));
            } else {
                desenho = getDesenhoProperties(terrenos[ii]);
            }
            this.addImage(desenho);
            terrainImages[ii] = desenho;
        }
        return getTerrainImages();
    }

    public Image getDesenhoProperties(String filename) {
        if (SettingsManager.getInstance().isConfig("MapTiles", "2a", "2b")) {
            //feralonso bordless
            return getForm().getToolkit().getImage(getClass().getResource("/images/mapa/hex_2a_" + filename + ".png"));
        } else if (SettingsManager.getInstance().isConfig("MapTiles", "2b", "2b")) {
            //bordless meppa
            return getForm().getToolkit().getImage(getClass().getResource("/images/mapa/hex_2b_" + filename + ".gif"));
        } else if (SettingsManager.getInstance().isConfig("MapTiles", "2d", "2b")) {
            //bord meppa
            return getForm().getToolkit().getImage(getClass().getResource("/images/mapa/hex_" + filename + ".gif"));
        } else if (SettingsManager.getInstance().isConfig("MapTiles", "3d", "2b")) {
            //3d from joao bordless
            return getForm().getToolkit().getImage(getClass().getResource("/images/mapa/hex_" + filename + ".png"));
        } else {
            //bordless meppa
            return getForm().getToolkit().getImage(getClass().getResource("/images/mapa/hex_2b_" + filename + ".gif"));
        }
    }

    private static int terrenoToIndice(String cdTerrain) {
        /*
         * 1 'E', '0101' alto mar<br> 2 'C', '0102' costa<br> 3 'L', '0203'
         * litoral<br> 4 'F', '0206' floresta<br> 5 'P', '0308' planicie<br> 6
         * 'M', '0604' montanha<br> 7 'H', '0710' colinas<br> 8 'S', '1509'
         * pantano<br> 9 'D', '2538' deserto<br>
         */
        try {
            switch (cdTerrain.charAt(0)) {
                case 'E':
                    return 1;
                case 'C':
                    return 2;
                case 'L':
                    return 3;
                case 'F':
                    return 4;
                case 'P':
                    return 5;
                case 'M':
                    return 6;
                case 'H':
                    return 7;
                case 'S':
                    return 8;
                case 'D':
                    return 9;
                case 'W':
                    return 10;
                case 'K':
                    return 11;
                default:
                    return 0;
            }
        } catch (NullPointerException npe) {
            return 0;
        }
    }

    public Image getTerrainImages(String terrenoCodigo) {
        return this.terrainImages[terrenoToIndice(terrenoCodigo)];
    }

    public void doLoadPortraits() {
//        boolean showPortrait = SettingsManager.getInstance().isConfig("ShowCharacterPortraits", "1", "0");
//
//        if (!showPortrait ) {
//            return;
//        }
        if (!portraitMap.isEmpty()) {
            return;
        }
        String portraitsPath = SettingsManager.getInstance().getConfig("PortraitsFolder", "");
        File portraitsFolder = new File(portraitsPath);
        if (!portraitsFolder.exists() || portraitsFolder.list().length <= 0) {
            log.debug("Folder '" + portraitsPath + "' not found.");
            return;
        }
        log.debug("Folder '" + portraitsPath + "' found.");
        File[] portraitsFile = portraitsFolder.listFiles();
        for (File portraitFile : portraitsFile) {
            portraitFile.toURI();
            ImageIcon portraitIcon = new ImageIcon(portraitFile.getAbsolutePath());
            portraitIcon.getIconWidth();
            portraitMap.put(portraitFile.getName(), portraitIcon);
        }
    }

    public ImageIcon getPortrait(String portraitName) {
        if (this.portraitMap.isEmpty()) {
            doLoadPortraits();
        }
        ImageIcon portrait = this.portraitMap.get(portraitName);
        if (portrait == null) {
            log.debug("Portrait image name " + portraitName + " not found.");
            portrait = this.portraitMap.get("blank.jpg");
        }
        return portrait;
    }

}
