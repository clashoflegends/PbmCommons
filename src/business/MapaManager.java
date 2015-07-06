/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business;

import baseLib.SysApoio;
import baseLib.SysProperties;
import business.facade.*;
import business.services.TagManager;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import model.*;
import msgs.ColorFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author jmoura
 */
public class MapaManager implements Serializable {

    private static final Log log = LogFactory.getLog(MapaManager.class);
    private Image[] desenhoTerrenos;
    private Image[] desenhoTerrenoDetalhes;
    private Image[] desenhoDetalhes;
    private Image[] desenhoCidades;
    private final Cenario cenario;
    private final JPanel form;
    private ImageIcon tagImage;
    private static final int dtPersonagem = 0, dtNpc = 1, dtArtefato = 2, dtGoldmine = 3, dtNavio = 4, dtTag = 5, dtFogofwar = 6, dtPersonagemOutra = 7;
    private static final LocalFacade localFacade = new LocalFacade();
    private static final CidadeFacade cidadeFacade = new CidadeFacade();
    private static final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final JogadorFacade jogadorFacade = new JogadorFacade();
    private static final ArtefatoFacade artefatoFacade = new ArtefatoFacade();
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final ImageFactory imageFactory;

    public MapaManager(Cenario aCenario, JPanel form) {
        this.cenario = aCenario;
        this.form = form;
        imageFactory = new ImageFactory(form, cenario);
        this.carregaDesenhosDisponiveis();
    }

    private void carregaDesenhosDisponiveis() {
        log.debug("Carregando: Desenhos...");
        Image desenho = null;
        carregaTerrenos();

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
        String[] detalhes = {"personagem", "npc", "artefato", "goldmine", "navio", "tag", "fogofwar", "pers_other"};
        desenhoDetalhes = new Image[detalhes.length];
        for (int ii = 0; ii < detalhes.length; ii++) {
            desenho = form.getToolkit().getImage(getClass().getResource("/images/mapa/hex_" + detalhes[ii] + ".gif"));
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
        int row = 0, col = 0;
        for (Local local : listaLocal) {
            row = localFacade.getRow(local);
            col = localFacade.getCol(local);
            if (row > ret[0]) {
                ret[0] = row;
            }
            if (col > ret[1]) {
                ret[1] = col;
            }
        }
        return new Point(ret[1] * 60 + 30, ret[0] * 45 + 60);
    }

    public ImageIcon getTagImage() {
        if (tagImage == null) {
            this.tagImage = new ImageIcon(this.desenhoDetalhes[dtTag]);
        }
        return tagImage;
    }

    private void printHex(Graphics2D big, Local local, Jogador observer) {
        int x = 0, y = 0, row = 0, col = 0;
        //calcula coordenadas e posicao no grafico.
        col = localFacade.getCol(local) - 1;
        row = localFacade.getRow(local) - 1;
        if (row % 2 == 0) {
            x = col * 60;
            y = row * 45;
        } else {
            x = col * 60 + 30;
            y = row * 45;
        }
        //terreno basico
        big.drawImage(this.desenhoTerrenos[terrenoToIndice(localFacade.getTerrenoCodigo(local))], x, y, form);
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
            big.drawImage(fortificacao, x + (ImageFactory.HEX_SIZE - largura) / 2, y + 34 - altura, form);

            if (CenarioFacade.isPrintGoldMine(this.cenario, local)) {
                //imprime gold mine
                Image goldMine = this.desenhoDetalhes[dtGoldmine];
                largura = goldMine.getWidth(form);
                //altura = goldMine.getHeight(form);
                big.drawImage(goldMine, x + (ImageFactory.HEX_SIZE - largura) / 2, y + 30, form);
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
                big.drawImage(colorCp, x + (ImageFactory.HEX_SIZE - largura) / 2, y + 34 - altura, form);

                //desenha docas
                Image docas = this.desenhoCidades[cidadeFacade.getDocas(cidade) + 12];
                largura = docas.getWidth(form);
                altura = docas.getHeight(form);
                big.drawImage(docas, x + (ImageFactory.HEX_SIZE - largura) / 2, y + 2, form);

                //desenha capital
                if (cidadeFacade.isCapital(cidade)) {
                    Image capital = this.desenhoCidades[15];
                    largura = capital.getWidth(form);
                    altura = capital.getHeight(form);
                    big.drawImage(capital, x + (ImageFactory.HEX_SIZE - largura) / 2, y + 30, form);
                }
            }
            if (CenarioFacade.isPrintNome(this.cenario, cidade)) {
                //imprime nome da Nacao
                big.setColor(Color.BLACK);
                big.drawString(cidadeFacade.getNacaoNome(cidade), x + 10, y + 30);
            }
        }
        //imprime o fog of war
        if (!local.isVisible() && !SettingsManager.getInstance().isWorldBuilder()) {
            big.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .6f));
            big.drawImage(this.desenhoDetalhes[dtFogofwar], x, y, form);
            big.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        //navios presentes
        if (localFacade.isBarcos(local)) {
            int dx = 34;
            int dy = 30;
            big.drawImage(this.desenhoDetalhes[dtNavio], x + dx, y + dy, form);
        }
        //grava numero hex
        big.setColor(Color.BLACK);
        big.drawString(localFacade.getCoordenadas(local), x + 16, y + 18);

        //exercitos presentes
        SortedMap<Nacao, Image> armyList = new TreeMap<Nacao, Image>();
        //monta a lista das nacoes com exercitos presentes no local
        for (Exercito exercito : localFacade.getExercitos(local).values()) {
            Image img = imageFactory.getExercito(exercito);
            Nacao nac = exercitoFacade.getNacao(exercito);
            armyList.put(nac, img);
        }
        int[][] posXY4 = {{7, 36}, {17, 42}, {34, 42}, {46, 36}};
        int[][] posXY6 = {{6, 36}, {12, 39}, {18, 42}, {36, 42}, {40, 39}, {46, 36}};
        int[][] posXY8 = {{6, 36}, {12, 39}, {18, 42}, {24, 45}, {30, 45}, {36, 42}, {40, 39}, {46, 36}};
        int[][] posXY;
        if (armyList.size() < 5) {
            posXY = posXY4;
        } else if (armyList.size() < 7) {
            posXY = posXY6;
        } else {
            posXY = posXY8;
        }
        //imprime de acordo com a quantidade
        int nn = 0;
        for (Image imgShield : armyList.values()) {
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
                big.drawImage(this.desenhoDetalhes[dtArtefato], x + 46, y + 22, form);
            }
        }
        //personagens presentes
        lista = localFacade.getPersonagens(local).values().iterator();
        while (lista.hasNext()) {
            Personagem pers = (Personagem) lista.next();
//            if (pers.getNome().contains("Ghost")) {
//                log.debug("AKI!!");
//            }
            if (personagemFacade.isNpc(pers)) {
                int dx = 04 + 8;
                int dy = 22 - 3;
                big.drawImage(this.desenhoDetalhes[dtNpc], x + dx, y + dy, form);
            } else if (!jogadorFacade.isMine(pers, observer)) {
                int dx = 04 + 8;
                int dy = 22 - 3 + 4;
                big.drawImage(this.desenhoDetalhes[dtPersonagemOutra], x + dx, y + dy, form);
            } else {
                int dx = 04;
                int dy = 22;
                big.drawImage(this.desenhoDetalhes[dtPersonagem], x + dx, y + dy, form);
            }
        }
    }

    public BufferedImage printMapaGeral(Collection<Local> listaLocal, Jogador observer) {
        //FIXME: imprimir em layers, permitindo visao do terreno, visao dos personagens, duplo clique para posicionar coisas, etc...
        log.debug("Escrevendo: MapaGeral...");
        Point farPoint = getMapMaxSize(listaLocal);
        int legendaW = 0, legendaH = 0;
        //cria a imagem base
        BufferedImage megaMap = new BufferedImage(farPoint.x + legendaW, farPoint.y + legendaH, BufferedImage.TRANSLUCENT);
        Graphics2D big;
        big = megaMap.createGraphics();
        //desenhando box para o mapa
        big.setBackground(Color.WHITE);
        big.clearRect(0, 0, farPoint.x, farPoint.y);
        big.setColor(Color.BLACK);
        big.setFont(new Font("Verdana", Font.PLAIN, 10));
        for (Local local : listaLocal) {
            printHex(big, local, observer);
        }
        big.dispose(); //libera memoria
        return megaMap;
    }

    private int[] exercitoToIndice(Exercito exercito) {
        int[] ret = new int[3];
        if (CenarioFacade.isGrecia(cenario)) {
            int[] posX = {7, 17, 34, 46, 7, 17, 34, 46};
            int[] posY = {36, 42, 42, 36, 36, 42, 42, 36};
            ret[0] = exercitoFacade.getNacaoNumero(exercito);
            ret[1] = posX[ret[0] - 1];
            ret[2] = posY[ret[0] - 1];
        } else {
            int[] posX = {44, 24, 44, 10};
            int[] posY = {36, 46, 36, 36};
            //FIXME: criar logica para escolher o icone da alianca.
            //ret[0] = exercitoFacade.getNacaoNumero(exercito);
            ret[0] = exercitoFacade.getNacaoNumero(exercito) + 2;
            ret[1] = posX[ret[0] - 1];
            ret[2] = posY[ret[0] - 1];
        }
        return ret;
    }

    private static int terrenoToIndice(String codigoTerreno) {
        //FIXME: listar os tipos...
        /*
         * 1 'E', '0101' alto mar<br> 2 'C', '0102' costa<br> 3 'L', '0203'
         * litoral<br> 4 'F', '0206' floresta<br> 5 'P', '0308' planicie<br> 6
         * 'M', '0604' montanha<br> 7 'H', '0710' colinas<br> 8 'S', '1509'
         * pantano<br> 9 'D', '2538' deserto<br>
         */
        int ret = 0;
        try {
            switch (codigoTerreno.charAt(0)) {
                case 'E':
                    //this.setEmTerra(false);
                    ret = 1;
                    break;
                case 'C':
                    //this.setEmTerra(false);
                    ret = 2;
                    break;
                case 'L':
                    //this.setEmTerra(true);
                    ret = 3;
                    //this.custoMovimento[0] = 3;
                    //this.custoMovimento[1] = 2;
                    //this.custoMovimento[2] = 2;
                    //this.custoMovimento[3] = 1;
                    break;
                case 'F':
                    //this.setEmTerra(true);
                    ret = 4;
                    //this.custoMovimento[0] = 5;
                    //this.custoMovimento[1] = 3;
                    //this.custoMovimento[2] = 5;
                    //this.custoMovimento[3] = 2;
                    break;
                case 'P':
                    //this.setEmTerra(true);
                    ret = 5;
                    //this.custoMovimento[0] = 3;
                    //this.custoMovimento[1] = 2;
                    //this.custoMovimento[2] = 2;
                    //this.custoMovimento[3] = 1;
                    break;
                case 'M':
                    //this.setEmTerra(true);
                    ret = 6;
                    //this.custoMovimento[0] = 12;
                    //this.custoMovimento[1] = 6;
                    //this.custoMovimento[2] = 12;
                    //this.custoMovimento[3] = 3;
                    break;
                case 'H':
                    //this.setEmTerra(true);
                    ret = 7;
                    //this.custoMovimento[0] = 5;
                    //this.custoMovimento[1] = 3;
                    //this.custoMovimento[2] = 3;
                    //this.custoMovimento[3] = 1;
                    break;
                case 'S':
                    //this.setEmTerra(true);
                    ret = 8;
                    //this.custoMovimento[0] = 6;
                    //this.custoMovimento[1] = 3;
                    //this.custoMovimento[2] = 5;
                    //this.custoMovimento[3] = 2;
                    break;
                case 'D':
                    //this.setEmTerra(true);
                    ret = 9;
                    //this.custoMovimento[0] = 4;
                    //this.custoMovimento[1] = 2;
                    //this.custoMovimento[2] = 2;
                    //this.custoMovimento[3] = 1;
                    break;
            }
        } catch (NullPointerException npe) {
            //this.setEmTerra(false);
            ret = 0;
        }
        return ret;
    }

    public void printLegenda(String dirName) {

        int legendaCounter = 0;
        int x = 0, y = 0, ih = 800, iw = 1200, gap = 12;
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
            "TERRENO.MONTANHA", "TERRENO.COLINA", "TERRENO.PANTANO", "TERRENO.DESERTO"
        };
        legendaCounter = 0;
        x = 10 + gap;
        y = 50;
        for (Image img : desenhoTerrenos) {
            big.drawImage(img, x, y, form);
            big.drawString(labels.getString(legendas[legendaCounter++]), x + gap * 2 + img.getWidth(form), y + img.getHeight(form) / 2);
            y += img.getWidth(form) + gap;
        }
        //detalhe de terrenos
        legendas = new String[]{
            "TERRENO.PONTE", "TERRENO.RIACHO", "TERRENO.RIO", "TERRENO.ESTRADA", "TERRENO.VAU", "TERRENO.LANDING",
            "TAG", "LOCAL.VISIVEL", "LOCAL.FOGOFWAR"
        };
        x += 200;
        y = 50;
        legendaCounter = 0;
        int ii = 0;
        big.drawImage(desenhoTerrenos[3], x, y, form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + desenhoTerrenos[3].getWidth(form), y + desenhoTerrenos[3].getHeight(form) / 2);
        for (Image img : desenhoTerrenoDetalhes) {
            big.drawImage(img, x, y, form);
            ii++;
            if (ii % 6 == 0) {
                y += img.getWidth(form) + gap;
                if (ii < desenhoTerrenoDetalhes.length) {
                    big.drawImage(desenhoTerrenos[3], x, y, form);
                    big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + desenhoTerrenos[3].getWidth(form), y + desenhoTerrenos[3].getHeight(form) / 2);
                }
            }
        }

        image = desenhoDetalhes[dtFogofwar];
        big.drawImage(desenhoTerrenos[5], x, y, form);
        big.drawImage(desenhoCidades[3], x + (ImageFactory.HEX_SIZE - desenhoCidades[3].getWidth(form)) / 2, y + 34 - desenhoCidades[3].getHeight(form), form);
        big.drawImage(this.desenhoCidades[11], x + (ImageFactory.HEX_SIZE - this.desenhoCidades[11].getWidth(form)) / 2, y + 34 - this.desenhoCidades[11].getHeight(form), form);
        big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + image.getWidth(form), y + image.getHeight(form) / 2);
        y += image.getWidth(form) + gap;

        big.drawImage(desenhoTerrenos[5], x, y, form);
        big.drawImage(desenhoCidades[3], x + (ImageFactory.HEX_SIZE - desenhoCidades[3].getWidth(form)) / 2, y + 34 - desenhoCidades[3].getHeight(form), form);
        big.drawImage(this.desenhoCidades[11], x + (ImageFactory.HEX_SIZE - this.desenhoCidades[11].getWidth(form)) / 2, y + 34 - this.desenhoCidades[11].getHeight(form), form);
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
            "PERSONAGEM", "PERSONAGEM.OUTRA.NACAO", "PERSONAGEM.NPC", "DB.ORDEM.PARAMETRO.ARTEFATO", "ZONA.ECONOMICA", "NAVIOS.PRESENTES"
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
        big.setColor(Color.RED);
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
            "DB.NACAO.NOME.FRUSODIAN"
        };
        legendaCounter = 0;
        for (Image img : imageFactory.getExercitosAll()) {
            if (legendaCounter + breakLine == 28) {
                x += 250;
                y = 50;
            }
            big.drawImage(img, x, y, form);
            big.drawString(labels.getString(legendas[legendaCounter++]), x + gap + img.getWidth(form), y + 2 + img.getHeight(form) / 2);
            y += img.getWidth(form) + gap;
        }

        //finaliza
        big.dispose();
        //salva em disco pra debug...
        try {
            // Save image
            File file = new File(dirName + "maplegend.png");
            ImageIO.write(megaMap, "png", file);
//            ImageIO.write(rendImage, "jpg", file);
        } catch (IOException ex) {
            log.fatal("Problem", ex);
        }
    }

    public int[] doCoordToPosition(Local destino) {
        int x = 0, y = 0, row = 0, col = 0;
        //calcula posicao no grafico a partir da coordenada do local
        col = localFacade.getCol(destino) - 1;
        row = localFacade.getRow(destino) - 1;
        if (row % 2 == 0) {
            x = col * 60;
            y = row * 45;
        } else {
            x = col * 60 + 30;
            y = row * 45;
        }
        return new int[]{x, y};
    }

    public Local doPositionToCoord(Point click, SortedMap<String, Local> locais) {
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

    private void carregaTerrenos() {
        Image desenho = null;
        String[] terrenos = {"vazio", "mar", "costa", "litoral", "floresta", "planicie",
            "montanha", "colinas", "pantano", "deserto"
        };
        desenhoTerrenos = new Image[terrenos.length];
        for (int ii = 0; ii < terrenos.length; ii++) {
            //FIXME: Buscar imagens de um JAR. colocar imagens num jar em separado, para facilitar downloads.
            if (terrenos[ii].equals("mar") && !SysProperties.getProps("ImagemMar", "-").equals("-")) {
                desenho = form.getToolkit().getImage(SysProperties.getProps("ImagemMar", "-"));
            } else if (terrenos[ii].equals("costa") && !SysProperties.getProps("ImagemCosta", "-").equals("-")) {
                desenho = form.getToolkit().getImage(SysProperties.getProps("ImagemCosta", "-"));
            } else if (terrenos[ii].equals("litoral") && !SysProperties.getProps("ImagemLitoral", "-").equals("-")) {
                desenho = form.getToolkit().getImage(SysProperties.getProps("ImagemLitoral", "-"));
            } else if (terrenos[ii].equals("floresta") && !SysProperties.getProps("ImagemFloresta", "-").equals("-")) {
                desenho = form.getToolkit().getImage(SysProperties.getProps("ImagemFloresta", "-"));
            } else if (terrenos[ii].equals("planicie") && !SysProperties.getProps("ImagemPlanicie", "-").equals("-")) {
                desenho = form.getToolkit().getImage(SysProperties.getProps("ImagemPlanicie", "-"));
            } else if (terrenos[ii].equals("montanha") && !SysProperties.getProps("ImagemMontanha", "-").equals("-")) {
                desenho = form.getToolkit().getImage(SysProperties.getProps("ImagemMontanha", "-"));
            } else if (terrenos[ii].equals("colinas") && !SysProperties.getProps("ImagemColinas", "-").equals("-")) {
                desenho = form.getToolkit().getImage(SysProperties.getProps("ImagemColinas", "-"));
            } else if (terrenos[ii].equals("pantano") && !SysProperties.getProps("ImagemPantano", "-").equals("-")) {
                desenho = form.getToolkit().getImage(SysProperties.getProps("ImagemPantano", "-"));
            } else if (terrenos[ii].equals("deserto") && !SysProperties.getProps("ImagemDeserto", "-").equals("-")) {
                desenho = form.getToolkit().getImage(SysProperties.getProps("ImagemDeserto", "-"));
            } else if (SysProperties.getProps("MapTiles", "2d").equalsIgnoreCase("2a")) {
                //feralonso
                desenho = form.getToolkit().getImage(getClass().getResource("/images/mapa/hex_2a_" + terrenos[ii] + ".png"));
            } else if (SysProperties.getProps("MapTiles", "2d").equalsIgnoreCase("3d")) {
                desenho = form.getToolkit().getImage(getClass().getResource("/images/mapa/hex_" + terrenos[ii] + ".png"));
            } else {
                desenho = form.getToolkit().getImage(getClass().getResource("/images/mapa/hex_" + terrenos[ii] + ".gif"));
            }
            imageFactory.addImage(desenho);
            this.desenhoTerrenos[ii] = desenho;
        }
    }
}
