/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business;

import business.converter.ConverterFactory;
import business.facade.AcaoFacade;
import business.facade.ArtefatoFacade;
import business.facade.CenarioFacade;
import business.facade.CidadeFacade;
import business.facade.ExercitoFacade;
import business.facade.JogadorFacade;
import business.facade.LocalFacade;
import business.facade.PersonagemFacade;
import business.converter.ColorFactory;
import business.services.TagManager;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import model.Artefato;
import model.Cenario;
import model.Cidade;
import model.Exercito;
import model.Habilidade;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Personagem;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 *
 * @author jmoura
 */
public class MapaManager implements Serializable {

    private static final Log log = LogFactory.getLog(MapaManager.class);
    private Image[] desenhoTerrenoDetalhes;
    private Image[] desenhoDetalhes;
    private Image[] desenhoCidades;
    private final Cenario cenario;
    private final JPanel form;
    private static final int dtPersonagem = 0, dtNpc = 1, dtArtefato = 2, dtGoldmine = 3, dtNavio = 4, dtTag = 5,
            dtFogofwar = 6, dtPersonagemOutra = 7, dtPersonagemAlly = 8, dtArtefatoKnown = 9;
    private static final LocalFacade localFacade = new LocalFacade();
    private static final CidadeFacade cidadeFacade = new CidadeFacade();
    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final JogadorFacade jogadorFacade = new JogadorFacade();
    private static final ArtefatoFacade artefatoFacade = new ArtefatoFacade();
    private static final AcaoFacade acaoFacade = new AcaoFacade();
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final ImageManager imageFactory = ImageManager.getInstance();
    private SortedMap<String, Local> locais;
    private Point farPoint;

    public MapaManager(Cenario aCenario, JPanel form) {
        this.cenario = aCenario;
        this.form = form;
        imageFactory.setCenario(cenario);
        imageFactory.setForm(form);
        this.carregaDesenhosDisponiveis();
    }

    /**
     * @return the locais
     */
    public SortedMap<String, Local> getLocais() {
        return locais;
    }

    /**
     * @param locais the locais to set
     */
    public void setLocais(SortedMap<String, Local> locais) {
        this.locais = locais;
    }

    private void carregaDesenhosDisponiveis() {
        log.debug("Carregando: Desenhos...");
        Image desenho;

        String[] detalhesTerreno = {
            "ponte_no", "ponte_ne", "ponte_l", "ponte_se", "ponte_so", "ponte_o",
            "riacho_no", "riacho_ne", "riacho_l", "riacho_se", "riacho_so", "riacho_o",
            "rio_no", "rio_ne", "rio_l", "rio_se", "rio_so", "rio_o",
            "road_no", "road_ne", "road_l", "road_se", "road_so", "road_o",
            "vau_no", "vau_ne", "vau_l", "vau_se", "vau_so", "vau_o",
            "water_NW", "water_NE", "water_E", "water_SE", "water_SW", "water_W"
        };
        desenhoTerrenoDetalhes = new Image[detalhesTerreno.length];
        for (int ii = 0; ii < detalhesTerreno.length; ii++) {
            desenho = form.getToolkit().getImage(getClass().getResource("/images/mapa/hex_" + detalhesTerreno[ii] + ".gif"));
            imageFactory.addImage(desenho);
            this.desenhoTerrenoDetalhes[ii] = desenho;
        }
        String[] detalhes = {"personagem", "npc", "artefato", "goldmine", "navio", "tag", "fogofwar", "pers_other", "pers_ally", "artefatoKnown"};
        desenhoDetalhes = new Image[detalhes.length];
        for (int ii = 0; ii < detalhes.length; ii++) {
            if (ii == dtFogofwar) {
                desenho = imageFactory.getDesenhoProperties(detalhes[ii]);
            } else {
                desenho = form.getToolkit().getImage(getClass().getResource("/images/mapa/hex_" + detalhes[ii] + ".gif"));
            }
            imageFactory.addImage(desenho);
            this.desenhoDetalhes[ii] = desenho;
        }
        String[] cps = {"vazio", "torre", "forte", "castelo", "fortaleza", "cidadela",
            "vazio", "acampamento", "aldeia", "vila", "burgo", "cidade",
            "vazio", "docas", "porto", "capital", "acampamento_hidden", "aldeia_hidden", "vila_hidden", "burgo_hidden", "cidade_hidden"};
        desenhoCidades = new Image[cps.length];
        for (int ii = 0; ii < cps.length; ii++) {
            desenho = form.getToolkit().getImage(getClass().getResource("/images/mapa/cp_" + cps[ii] + ".gif"));
            imageFactory.addImage(desenho);
            this.desenhoCidades[ii] = desenho;
        }
        imageFactory.carregaExercito();
        imageFactory.waitForAll();
    }

    public Point getMapMaxSize(Collection<Local> listaLocal) {
        int[] ret = {0, 0};
        int row, col;
        for (Local local : listaLocal) {
            row = LocalFacade.getRow(local);
            col = LocalFacade.getCol(local);
            if (row > ret[0]) {
                ret[0] = row;
            }
            if (col > ret[1]) {
                ret[1] = col;
            }
        }
        return new Point(ret[1] * 60 + 30, ret[0] * 45 + 60);
    }

    private void printHex(Graphics2D big, Local local, Jogador observer) {
        //calcula coordenadas e posicao no grafico.
        final Point point = ConverterFactory.localToPoint(local);
        final int x = (int) point.getX(), y = (int) point.getY();
        //terreno basico
        big.drawImage(imageFactory.getTerrainImages(localFacade.getTerrenoCodigo(local)), x, y, form);
        //detalhes do terreno
        for (int direcao = 1; direcao < 7; direcao++) {
            //detalhe estrada
            if (localFacade.isEstrada(local, direcao)) {
                big.drawImage(this.desenhoTerrenoDetalhes[direcao + 17], x, y, form);
            }
            //detalhe rio
            if (localFacade.isRio(local, direcao)) {
                big.drawImage(this.desenhoTerrenoDetalhes[direcao + 11], x, y, form);
            }
            //detalhe riacho
            if (localFacade.isRiacho(local, direcao)) {
                big.drawImage(this.desenhoTerrenoDetalhes[direcao + 5], x, y, form);
            }
            //grava rastro exercito
            if (localFacade.isRastroExercito(local, direcao) && local.isVisible()) {
                imageFactory.doDrawRastro(big, direcao, x, y);
            }
            //detalhe ponte
            if (localFacade.isPonte(local, direcao)) {
                big.drawImage(this.desenhoTerrenoDetalhes[direcao - 1], x, y, form);
            }
            //detalhe landing
            if (localFacade.isLanding(local, direcao)) {
                big.drawImage(this.desenhoTerrenoDetalhes[direcao + 29], x, y, form);
            }
            //detalhe vau
            if (localFacade.isVau(local, direcao)) {
                big.drawImage(this.desenhoTerrenoDetalhes[direcao + 23], x, y, form);
            }
        }
        //centro populacional
        if (localFacade.isCidade(local)) {
            Cidade cidade = localFacade.getCidade(local);
            int largura, altura;
            //desenha fortificacao
            Image fortificacao = this.desenhoCidades[cidadeFacade.getFortificacao(cidade)];
            largura = fortificacao.getWidth(form);
            altura = fortificacao.getHeight(form);
            big.drawImage(fortificacao, x + (ImageManager.HEX_SIZE - largura) / 2, y + 34 - altura, form);

            if (CenarioFacade.isPrintGoldMine(this.cenario, local)) {
                //imprime gold mine
                Image goldMine = this.desenhoDetalhes[dtGoldmine];
                largura = goldMine.getWidth(form);
                //altura = goldMine.getHeight(form);
                big.drawImage(goldMine, x + (ImageManager.HEX_SIZE - largura) / 2, y + 30, form);
            } else {
                //verifica se cidade eh escondida
                int cpEscondido = 0;
                if (cidadeFacade.isOculto(cidade)) {
                    cpEscondido = 9;
                }
                //desenha cidade
                Image colorCp = ColorFactory.setNacaoColor(
                        this.desenhoCidades[cidadeFacade.getTamanho(cidade) + 6 + cpEscondido],
                        cidadeFacade.getNacaoColorFill(cidade),
                        cidadeFacade.getNacaoColorBorder(cidade),
                        form);
                largura = colorCp.getWidth(form);
                altura = colorCp.getHeight(form);
                big.drawImage(colorCp, x + (ImageManager.HEX_SIZE - largura) / 2, y + 34 - altura, form);

                //desenha docas
                Image docas = this.desenhoCidades[cidadeFacade.getDocas(cidade) + 12];
                largura = docas.getWidth(form);
                big.drawImage(docas, x + (ImageManager.HEX_SIZE - largura) / 2, y + 2, form);

                //desenha capital
                if (cidadeFacade.isCapital(cidade)) {
                    Image capital = this.desenhoCidades[15];
                    largura = capital.getWidth(form);
                    big.drawImage(capital, x + (ImageManager.HEX_SIZE - largura) / 2, y + 30, form);
                }
            }
            if (CenarioFacade.isPrintNome(this.cenario, cidade)) {
                //imprime nome da Nacao
                big.setColor(Color.BLACK);
                big.drawString(cidadeFacade.getNacaoNome(cidade), x + 10, y + 30);
            }
        }
        if (local.isVisible() && localFacade.isTerrainLandmark(local)) {
            //terrain features
            for (Habilidade feature : localFacade.getTerrainLandmark(local)) {
                //imprime gold mine
                Image imgFeature = imageFactory.getFeature(feature);
                big.drawImage(imgFeature, x + (ImageManager.HEX_SIZE - imgFeature.getWidth(form)) / 2, y + (ImageManager.HEX_SIZE - imgFeature.getHeight(form)) / 2, form);
            }
        }
        //imprime o fog of war
        if (!local.isVisible() && !SettingsManager.getInstance().isWorldBuilder() && !SettingsManager.getInstance().isConfig("fogOfWarType", "0", "1")) {
            if (SettingsManager.getInstance().isConfig("fogOfWarType", "1", "1")) {
                //print fog
                big.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .6f));
                big.drawImage(this.desenhoDetalhes[dtFogofwar], x, y, form);
                big.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }
        }
        //navios presentes
        if (localFacade.isBarcos(local)) {
            int dx = 34;
            int dy = 30;
            big.drawImage(this.desenhoDetalhes[dtNavio], x + dx, y + dy, form);
        }
        //took combat?
        if (local.isVisible() && localFacade.isCombatTookPlace(local)) {
            int dx = 46;
            int dy = 30;
            //what type of combat?
            if (localFacade.isCombatTookPlaceBigNavy(local)) {
                big.drawImage(imageFactory.doDrawCombatBigNavy(), x + dx, y + dy, form);
            } else if (localFacade.isCombatTookPlaceBigArmy(local)) {
                big.drawImage(imageFactory.doDrawCombatBigArmy(), x + dx, y + dy, form);
            } else {
                //none of the above
                big.drawImage(imageFactory.doDrawCombat(), x + dx, y + dy, form);
            }
        }
        //has overrun?
        if (local.isVisible() && localFacade.isOverrunTookPlace(local)) {
            int dx = 40;
            int dy = 36;
            big.drawImage(imageFactory.doDrawExplosion(), x + dx, y + dy, form);
        }
        //grava numero hex
        big.setColor(Color.BLACK);
        big.drawString(LocalFacade.getCoordenadas(local), x + 16, y + 18);

        //exercitos presentes
        final SortedMap<Nacao, Image> armyListNoDups = new TreeMap<>();
        final List<Image> armyList = new ArrayList<>();

        //monta a lista das nacoes com exercitos presentes no local
        final boolean armyIconDrawType = SettingsManager.getInstance().isConfig("DrawAllArmyIcons", "1", "1");
        for (Exercito exercito : localFacade.getExercitos(local).values()) {
            Image img = imageFactory.getExercito(exercito);
            Nacao nac = exercitoFacade.getNacao(exercito);
            armyListNoDups.put(nac, img);
            armyList.add(img);
        }
        //decide which list to use
        final Collection<Image> values;
        if (armyList.size() < 9 && armyIconDrawType) {
            values = armyList;
        } else {
            values = armyListNoDups.values();
        }
        //possible templates
        int[][] posXY4 = {{7, 36}, {17, 42}, {34, 42}, {46, 36}};
        int[][] posXY6 = {{6, 36}, {12, 39}, {18, 42}, {36, 42}, {40, 39}, {46, 36}};
        int[][] posXY8 = {{6, 36}, {12, 39}, {18, 42}, {24, 45}, {30, 45}, {36, 42}, {40, 39}, {46, 36}};
        int[][] posXY;
        if (values.size() < 5) {
            posXY = posXY4;
        } else if (values.size() < 7) {
            posXY = posXY6;
        } else {
            posXY = posXY8;
        }
        //imprime de acordo com a quantidade
        int nn = 0;
        for (Image imgShield : values) {
            //FIXME: imprimir exercito de nacao desconhecida???
            big.drawImage(imgShield, x + posXY[nn][0], y + posXY[nn][1], form);
            nn++;
        }
        Iterator lista;
        //artefatoes presentes
        lista = localFacade.getArtefatos(local).values().iterator();
        while (lista.hasNext()) {
            Artefato artefato = (Artefato) lista.next();
            if (artefatoFacade.isPosse(artefato)) {
                big.drawImage(this.desenhoDetalhes[dtArtefato], x + 11, y + 22, form);
            } else {
                big.drawImage(this.desenhoDetalhes[dtArtefatoKnown], x + 46, y + 22, form);
            }
        }
        //personagens presentes
        lista = localFacade.getPersonagens(local).values().iterator();
        while (lista.hasNext()) {
            Personagem pers = (Personagem) lista.next();
            if (pers.getNome().equals("Silverwing")) {
                log.debug("AKI!");
            }
            if (personagemFacade.isNpc(pers)) {
                int dx = 04 + 8;
                int dy = 22 - 3;
                big.drawImage(this.desenhoDetalhes[dtNpc], x + dx, y + dy, form);
            } else if (jogadorFacade.isMine(pers, observer)) {
                int dx = 04;
                int dy = 22;
                big.drawImage(this.desenhoDetalhes[dtPersonagem], x + dx, y + dy, form);
            } else if (jogadorFacade.isAlly(pers, observer)) {
                int dx = 04 + 3;
                int dy = 22 - 3 + 7;
                big.drawImage(this.desenhoDetalhes[dtPersonagemAlly], x + dx, y + dy, form);
            } else {
                int dx = 04 + 8;
                int dy = 22 - 3 + 4;
                big.drawImage(this.desenhoDetalhes[dtPersonagemOutra], x + dx, y + dy, form);
            }
        }
    }

    /**
     * Draw past movements
     *
     * @param big
     * @param listaPers
     * @param observer
     */
    private void drawMapaMovPath(Graphics2D big, Collection<Personagem> listaPers, Jogador observer) {
        if (!SettingsManager.getInstance().isConfig("drawPcPath", "1", "1") && !SettingsManager.getInstance().isConfig("drawPcPath", "2", "1")) {
            //don't draw
            return;
        }
        for (Personagem pers : listaPers) {
            if (pers.getLocal() == null || pers.getLocalOrigem() == null) {
                continue;
            }
            if (pers.getLocal().equals(pers.getLocalOrigem())) {
                continue;
            }
            //calculate points;
            final Point ori = ConverterFactory.localToPoint(pers.getLocalOrigem());
            final Point dest = ConverterFactory.localToPoint(pers.getLocal());
            if (personagemFacade.isNpc(pers)) {
                imageFactory.doDrawPathNpc(big, ori, dest);
            } else if (jogadorFacade.isMine(pers, observer)) {
                imageFactory.doDrawPathPc(big, ori, dest);
            } else if (jogadorFacade.isAlly(pers, observer)) {
                imageFactory.doDrawPathPcAlly(big, ori, dest);
            } else {
                imageFactory.doDrawPathPcEnemy(big, ori, dest);
            }
        }
    }

    private void drawPcActionsOnMap(Graphics2D big, Collection<Personagem> listaPers, Jogador observer) {
        for (Personagem pers : listaPers) {
            if (pers.getLocal() == null) {
                continue;
            }
            for (PersonagemOrdem po : pers.getAcoes().values()) {
                if (acaoFacade.isScout(po)) {
                    drawScoutOnMap(po, pers, observer, big);
                } else if (acaoFacade.isMovimentoDirection(po)) {
                    drawMovPathArmy(po, pers, observer, big);
                } else if (acaoFacade.isMovimento(po)) {
                    drawMovPathPc(po, pers, observer, big);
                }
            }
        }
    }

    private void drawMovPathArmy(PersonagemOrdem po, Personagem pers, Jogador observer, Graphics2D big) {
        if (SettingsManager.getInstance().isConfig("drawArmyMovPath", "0", "1")) {
            //don't draw
            return;
        }
        final SortedMap<Integer, Local> pathMov = acaoFacade.getLocalDestinationPath(pers, po, getLocais());
        if (pathMov.isEmpty()) {
            return;
        }
        Local baseLocal = pers.getLocal();
        for (Local nextLocal : pathMov.values()) {
            if (nextLocal == null) {
                break;
            }
            if (baseLocal.equals(nextLocal)) {
                continue;
            }
            //draw all movement paths.
            final Point dest = ConverterFactory.localToPoint(nextLocal);
            final Point ori = ConverterFactory.localToPoint(baseLocal);
            if (jogadorFacade.isMine(pers, observer)) {
                imageFactory.doDrawPathArmyOrder(big, ori, dest);
            } else if (jogadorFacade.isAlly(pers, observer)) {
                imageFactory.doDrawPathArmyAllyOrder(big, ori, dest);
            }
            baseLocal = nextLocal;
        }
    }

    /**
     * Draw future movements
     *
     * @param po
     * @param pers
     * @param observer
     * @param big
     */
    private void drawMovPathPc(PersonagemOrdem po, Personagem pers, Jogador observer, Graphics2D big) {
        if (!SettingsManager.getInstance().isConfig("drawPcPath", "1", "1") && !SettingsManager.getInstance().isConfig("drawPcPath", "3", "1")) {
            //don't draw
            return;
        }

        final Local localDestination = acaoFacade.getLocalDestination(pers, po, getLocais());
        if (localDestination == null) {
            return;
        }
        if (pers.getLocal().equals(localDestination)) {
            return;
        }
        //draw all movement paths.
        final Point dest = ConverterFactory.localToPoint(localDestination);
        final Point ori = ConverterFactory.localToPoint(pers.getLocal());
        if (jogadorFacade.isMine(pers, observer)) {
            imageFactory.doDrawPathPcOrder(big, ori, dest);
        } else if (jogadorFacade.isAlly(pers, observer)) {
            imageFactory.doDrawPathPcAllyOrder(big, ori, dest);
        }
    }

    /**
     * Draw the center of all scout actions (;ASR;) on map
     *
     * @param po
     * @param pers
     * @param observer
     * @param big
     */
    private void drawScoutOnMap(PersonagemOrdem po, Personagem pers, Jogador observer, Graphics2D big) {
        if (SettingsManager.getInstance().isConfig("drawScoutOnMap", "0", "1")) {
            //don't draw
            return;
        }

        //find target location of order or item
        Local localDestination = acaoFacade.getLocalDestination(pers, po, getLocais());
        if (localDestination == null) {
            //find location according to order sequence i.e. recon after movement
            localDestination = personagemFacade.getLocalDestination(pers, getLocais());
        }
        if (localDestination == null) {
            //can't find local, don't do anything
            return;
        }
        final Point dest = ConverterFactory.localToPoint(localDestination);
        if (jogadorFacade.isMine(pers, observer)) {
            imageFactory.doDrawScout(big, dest);
        } else if (jogadorFacade.isAlly(pers, observer)) {
            imageFactory.doDrawScoutAlly(big, dest);
        }
    }

    public BufferedImage redrawMapaGeral(Collection<Local> listaLocal, Collection<Personagem> listaPers, Jogador observer) {
        ImageManager.getInstance().doLoadTerrainImages();
        this.carregaDesenhosDisponiveis();
        return printMapaGeral(listaLocal, listaPers, observer);
    }

    public BufferedImage printMapaGeral(Collection<Local> listaLocal, Collection<Personagem> listaPers, Jogador observer) {
        if (farPoint == null) {
            this.farPoint = getMapMaxSize(listaLocal);
        }

        //FIXME: imprimir em layers, permitindo visao do terreno, visao dos personagens, duplo clique para posicionar coisas, etc...
        log.debug("Escrevendo: MapaGeral...");
        int legendaW = 0, legendaH = 0;
        //cria a imagem base
        BufferedImage megaMap = new BufferedImage(farPoint.x + legendaW, farPoint.y + legendaH, BufferedImage.TRANSLUCENT);
        final Graphics2D big = megaMap.createGraphics();

        //TODO: set scale for graph
        //big.scale(2.0, 2.0);
        //TODO: outra opcao
        double percent = 1.5;
//            Graphics2D g2d = outputImage.createGraphics();
//        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
//        g2d.dispose();
        //desenhando box para o mapa
        big.setBackground(Color.WHITE);
        big.clearRect(0, 0, farPoint.x, farPoint.y);
        big.setColor(Color.BLACK);
        big.setFont(new Font("Verdana", Font.PLAIN, 10));
        for (Local local : listaLocal) {
            printHex(big, local, observer);
        }
        drawMapaMovPath(big, listaPers, observer);
        big.dispose(); //libera memoria
        return megaMap;
    }

    public BufferedImage printActionsOnMap(Collection<Local> listaLocal, Collection<Personagem> listaPers, Jogador observer) {
        if (farPoint == null) {
            this.farPoint = getMapMaxSize(listaLocal);
        }
        //cria a imagem base
        BufferedImage megaMap = new BufferedImage(farPoint.x, farPoint.y, BufferedImage.TRANSLUCENT);
        final Graphics2D big = megaMap.createGraphics();
        //desenhando box para o mapa
        drawPcActionsOnMap(big, listaPers, observer);
        big.dispose(); //libera memoria
        return megaMap;
    }

    public void printLegenda(String dirName) {

        int legendaCounter;
        int x, y, ih = 800, iw = 1200, gap = 12;
        String[] legendas;
        BufferedImage megaMap = new BufferedImage(iw, ih, BufferedImage.TRANSLUCENT);
        Image image;
        Graphics2D big;
        big = megaMap.createGraphics();
        big.setBackground(Color.WHITE);
        big.clearRect(0, 0, iw, ih);
        big.setColor(Color.BLUE);
        big.drawRect(0 + gap, 0 + gap, iw - gap * 2, ih - gap * 2);
        big.setFont(new Font("Verdana", Font.BOLD, 14));
        x = 10 + gap;
        y = 30;
        big.drawString(labels.getString("LEGENDA"), x, y);
        //terrenos
        legendas = new String[]{"TERRENO.DESCONHECIDO", "TERRENO.ALTO.MAR", "TERRENO.COSTA", "TERRENO.LITORAL", "TERRENO.FLORESTA", "TERRENO.PLANICIE",
            "TERRENO.MONTANHA", "TERRENO.COLINA", "TERRENO.PANTANO", "TERRENO.DESERTO", "TERRENO.WASTELAND", "TERRENO.LAGO"
        };
        legendaCounter = 0;
        x = 10 + gap;
        y = 50;
        for (Image img : imageFactory.getTerrainImages()) {
            big.drawImage(img, x, y, form);
            big.drawString(labels.getString(legendas[legendaCounter++]), x + gap * 2 + img.getWidth(form), y + img.getHeight(form) / 2);
            y += img.getWidth(form) + gap;
        }
        //detalhe de terrenos
        legendas = new String[]{
            "TERRENO.PONTE", "TERRENO.RIACHO", "TERRENO.RIO", "TERRENO.ESTRADA", "TERRENO.VAU", "TERRENO.LANDING",
            "LOCAL.VISIVEL", "LOCAL.FOGOFWAR", "TAG"
        };
        x += 200;
        y = 50;
        legendaCounter = 0;
        int ii = 0;
        final Image terrainShore = imageFactory.getTerrainImages("L");
        final Image terrainPlains = imageFactory.getTerrainImages("P");
        big.drawImage(terrainShore, x, y, form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + terrainShore.getWidth(form), y + terrainShore.getHeight(form) / 2);
        for (Image img : desenhoTerrenoDetalhes) {
            big.drawImage(img, x, y, form);
            ii++;
            if (ii % 6 == 0) {
                y += img.getWidth(form) + gap;
                if (ii < desenhoTerrenoDetalhes.length) {
                    big.drawImage(terrainShore, x, y, form);
                    big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + terrainShore.getWidth(form), y + terrainShore.getHeight(form) / 2);
                }
            }
        }

        image = desenhoDetalhes[dtFogofwar];
        big.drawImage(terrainPlains, x, y, form);
        big.drawImage(desenhoCidades[3], x + (ImageManager.HEX_SIZE - desenhoCidades[3].getWidth(form)) / 2, y + 34 - desenhoCidades[3].getHeight(form), form);
        big.drawImage(this.desenhoCidades[11], x + (ImageManager.HEX_SIZE - this.desenhoCidades[11].getWidth(form)) / 2, y + 34 - this.desenhoCidades[11].getHeight(form), form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + image.getWidth(form), y + image.getHeight(form) / 2);
        y += image.getWidth(form) + gap;

        big.drawImage(terrainPlains, x, y, form);
        big.drawImage(desenhoCidades[3], x + (ImageManager.HEX_SIZE - desenhoCidades[3].getWidth(form)) / 2, y + 34 - desenhoCidades[3].getHeight(form), form);
        big.drawImage(this.desenhoCidades[11], x + (ImageManager.HEX_SIZE - this.desenhoCidades[11].getWidth(form)) / 2, y + 34 - this.desenhoCidades[11].getHeight(form), form);
        big.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .6f));
        big.drawImage(this.desenhoDetalhes[dtFogofwar], x, y, form);
        big.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + image.getWidth(form), y + image.getHeight(form) / 2);
        y += image.getWidth(form) + gap;

        image = TagManager.getInstance().drawTagStyle3(10, 10).getImage();
        big.drawImage(image, x, y, form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + image.getWidth(form), y + image.getHeight(form) / 2);
        y += image.getWidth(form) + gap;

        //cidades
        legendas = new String[]{
            "CIDADE.FORTIFICACAO0", "CIDADE.FORTIFICACAO1", "CIDADE.FORTIFICACAO2", "CIDADE.FORTIFICACAO3", "CIDADE.FORTIFICACAO4", "CIDADE.FORTIFICACAO5",
            "CIDADE.TAMANHO0", "CIDADE.TAMANHO1", "CIDADE.TAMANHO2", "CIDADE.TAMANHO3", "CIDADE.TAMANHO4", "CIDADE.TAMANHO5",
            "STRING.VAZIA", "CIDADE.DOCAS", "CIDADE.PORTO", "CIDADE.CAPITAL",
            "CIDADE.TAMANHO1.HIDDEN", "CIDADE.TAMANHO2.HIDDEN", "CIDADE.TAMANHO3.HIDDEN", "CIDADE.TAMANHO4.HIDDEN", "CIDADE.TAMANHO5.HIDDEN"
        };
        legendaCounter = 0;
        x += 200;
        y = 50;
        for (Image img : desenhoCidades) {
            big.drawImage(img, x, y, form);
            big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + img.getWidth(form), y + img.getHeight(form) / 2);
            y += img.getWidth(form) + gap;
        }

        //detalhes
        x += 250;
        y = 50;
        legendaCounter = 0;
        legendas = new String[]{
            "EXERCITO.RASTRO",
            "PERSONAGEM", "PERSONAGEM.OUTRA.NACAO", "PERSONAGEM.OUTRA.ALLY", "PERSONAGEM.NPC", "DB.ORDEM.PARAMETRO.ARTEFATO", "ZONA.ECONOMICA", "NAVIOS.PRESENTES", "COMBAT.HERE", "EXPLOSION.HERE"
        };
        //imprime rastro do exercito
        big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        big.setStroke(new BasicStroke(
                2f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                1f,
                new float[]{5f},
                0f));
        big.setColor(Color.PINK);
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x, y);
        path.lineTo(x + 10, y);
        path.lineTo(x + 20, y + 10);
        path.lineTo(x + 30, y + 15);
        big.draw(path);
        big.setColor(Color.BLUE);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + (int) path.getBounds().getWidth(), y + (int) path.getBounds().getHeight() / 2);
        y += path.getBounds().getHeight() + gap;

        //imprime detalhes
        image = desenhoDetalhes[dtPersonagem];
        big.drawImage(image, x, y, form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + image.getWidth(form), y + image.getHeight(form) / 2);
        y += image.getWidth(form) + gap;

        image = desenhoDetalhes[dtPersonagemOutra];
        big.drawImage(image, x, y, form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + image.getWidth(form), y + image.getHeight(form) / 2);
        y += image.getWidth(form) + gap;

        image = desenhoDetalhes[dtPersonagemAlly];
        big.drawImage(image, x, y, form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + image.getWidth(form), y + image.getHeight(form) / 2);
        y += image.getWidth(form) + gap;

        image = desenhoDetalhes[dtNpc];
        big.drawImage(image, x, y, form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + image.getWidth(form), y + image.getHeight(form) / 2);
        y += image.getWidth(form) + gap;

        image = desenhoDetalhes[dtArtefato];
        big.drawImage(image, x, y, form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + image.getWidth(form), y + image.getHeight(form) / 2);
        y += image.getWidth(form) + gap;

        image = desenhoDetalhes[dtGoldmine];
        big.drawImage(image, x, y, form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + image.getWidth(form), y + image.getHeight(form) / 2);
        y += image.getWidth(form) + gap;

        image = desenhoDetalhes[dtNavio];
        big.drawImage(image, x, y, form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + image.getWidth(form), y + image.getHeight(form) / 2);
        y += image.getWidth(form) + gap;

        image = imageFactory.doDrawCombat();
        big.drawImage(image, x, y, form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + image.getWidth(form), y + image.getHeight(form) / 2);
        y += image.getWidth(form) + gap;

        image = imageFactory.doDrawExplosion();
        big.drawImage(image, x, y, form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + image.getWidth(form), y + image.getHeight(form) / 2);
        y += image.getWidth(form) + gap;

        int breakLine = legendaCounter;

        //exercito
        legendas = new String[]{
            "DB.NACAO.NOME.NONE",
            "DB.NACAO.NOME.KINGSCOURT",
            "DB.NACAO.NOME.KINGSCOURT",
            "DB.NACAO.NOME.ARRYN",
            "DB.NACAO.NOME.BARATHEON",
            "DB.NACAO.NOME.GREYJOY",
            "DB.NACAO.NOME.LANNISTER",
            "DB.NACAO.NOME.MARTELL",
            "DB.NACAO.NOME.STARK",
            "DB.NACAO.NOME.TARGARYEN",
            "DB.NACAO.NOME.TULLY",
            "DB.NACAO.NOME.TYRELL",
            "DB.NACAO.NOME.NIGHTWATCH",
            "DB.NACAO.NOME.BOLTON",
            "DB.NACAO.NOME.YRONWOOD",
            "DB.NACAO.NOME.STORMEND",
            "DB.NACAO.NOME.FREY",
            "DB.NACAO.NOME.HIGHTOWER",
            "DB.NACAO.NOME.VOLANTIS",
            "DB.NACAO.NOME.PENTOS",
            "DB.NACAO.NOME.BRAAVOS",
            "DB.NACAO.NOME.FREECITIES",
            "DB.NACAO.NOME.WILDLINGS",
            "DB.NACAO.NOME.NEUTRAL1",
            "DB.NACAO.NOME.WHITEWALKERS",
            "DB.NACAO.NOME.ESPARTA",
            "DB.NACAO.NOME.ATENAS",
            "DB.NACAO.NOME.MACEDONIA",
            "DB.NACAO.NOME.PERSIA",
            "DB.NACAO.NOME.TRACIA",
            "DB.NACAO.NOME.MILLETUS",
            "DB.NACAO.NOME.ILLYRIA",
            "DB.NACAO.NOME.EPIRUS",
            "DB.NACAO.NOME.TWAINEK",
            "DB.NACAO.NOME.FRUSODIAN",
            "DB.NACAO.NOME.BALTOCH",
            "DB.NACAO.NOME.BEORNINGS",
            "DB.NACAO.NOME.BLACKHAMMER",
            "DB.NACAO.NOME.BROADBEAMS",
            "DB.NACAO.NOME.CARNDUN",
            "DB.NACAO.NOME.DOLGULDUR",
            "DB.NACAO.NOME.DORWINION",
            "DB.NACAO.NOME.DRUEDAIN",
            "DB.NACAO.NOME.FOROCHEL",
            "DB.NACAO.NOME.GRAM",
            "DB.NACAO.NOME.GREY",
            "DB.NACAO.NOME.GUNDABAD",
            "DB.NACAO.NOME.HALFLING",
            "DB.NACAO.NOME.HIGHPASS",
            "DB.NACAO.NOME.IRONHELM",
            "DB.NACAO.NOME.LINDON",
            "DB.NACAO.NOME.LONGBEARD",
            "DB.NACAO.NOME.LORIEN",
            "DB.NACAO.NOME.LOSSOTH",
            "DB.NACAO.NOME.MENLAKE",
            "DB.NACAO.NOME.METHEDRAS",
            "DB.NACAO.NOME.MISTYGOBLIN",
            "DB.NACAO.NOME.MORANNON",
            "DB.NACAO.NOME.MORIA",
            "DB.NACAO.NOME.NEUTRAL1",
            "DB.NACAO.NOME.NORTHMEN",
            "DB.NACAO.NOME.NORTHORCS",
            "DB.NACAO.NOME.RAIDERS",
            "DB.NACAO.NOME.RANGERS",
            "DB.NACAO.NOME.RIVENDELL",
            "DB.NACAO.NOME.ROHAN",
            "DB.NACAO.NOME.SILVAN",
            "DB.NACAO.NOME.STONEFIST",
            "DB.NACAO.NOME.VARIAGS",
            "DB.NACAO.NOME.WAINRIDERS",
            "DB.NACAO.NOME.WARGRIDERS",
            "DB.NACAO.NOME.WOODMEN",
            "DB.NACAO.NOME.WOSES"
        };
        legendaCounter = 0;
        for (Image img : imageFactory.getExercitosAll()) {
            if (legendaCounter + breakLine == 28) {
                x += 250;
                y = 50;
            }
            big.drawImage(img, x, y, form);
            big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + img.getWidth(form), y + 2 + img.getHeight(form) / 2);
//            log.info(String.format("Legenda:%s %s %s", legendaCounter-1, legendas[legendaCounter], labels.getString(legendas[legendaCounter])));
            y += img.getWidth(form) + gap;
        }

        //finaliza
        big.dispose();
        //salva em disco pra debug...
        try {
            // Save image
            File file = new File(dirName + "map - legend.png");
            ImageIO.write(megaMap, "png", file);
//            ImageIO.write(rendImage, "jpg", file);
        } catch (IOException ex) {
            log.fatal("Problem", ex);
        }
    }

    public int[] doCoordToPosition(Local destino) {
        final Point p = ConverterFactory.localToPoint(destino);
        return new int[]{(int) p.getX(), (int) p.getY()};
    }

    public Local doPositionToCoord(Point click) {
        int row, col;
        row = click.y / 45;
        if (row % 2 == 0) {
            col = click.x / 60;
        } else {
            col = (click.x - 30) / 60;
        }
        Local ret = locais.get(SysApoio.pointToCoord(col + 1, row + 1));
        return ret;
    }
}
