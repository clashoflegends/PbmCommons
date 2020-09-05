/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import business.converter.ConverterFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import model.Artefato;
import model.Cidade;
import model.Exercito;
import model.Habilidade;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Partida;
import model.Personagem;
import model.Terreno;
import model.World;
import msgs.BaseMsgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SysApoio;

/**
 *
 * @author gurgel
 */
public final class LocalFacade implements Serializable {

    private static final Log log = LogFactory.getLog(LocalFacade.class);
    private final SortedMap<String, String> landmarkImage = new TreeMap<>();

    public LocalFacade() {
        doLoadLandmarksImage();
    }

    private void doLoadLandmarksImage() {
        landmarkImage.put(";LFC;", "/images/mapa/feature_cave.gif");
        landmarkImage.put(";LFH;", "/images/mapa/feature_henges.gif");
        landmarkImage.put(";LFI;", "/images/mapa/feature_igloo.gif");
        landmarkImage.put(";LFK;", "/images/mapa/feature_lake.gif");
        landmarkImage.put(";LFL;", "/images/mapa/feature_liths.gif");
        landmarkImage.put(";LFE;", "/images/mapa/feature_temple.gif");
        landmarkImage.put(";LFR;", "/images/mapa/feature_ruins.gif");
        landmarkImage.put(";LFT;", "/images/mapa/feature_tower.gif");
    }

    public SortedMap<String, Artefato> getArtefatos(Local local) {
        return local.getArtefatos();
    }

    public static int getRow(Local local) {
        return Integer.parseInt(local.getCodigo().substring(2));
    }

    public static int getCol(Local local) {
        return Integer.parseInt(local.getCodigo().substring(0, 2));
    }

    public static int getRow(String coordinates) {
        return Integer.parseInt(coordinates.substring(2));
    }

    public static int getCol(String coordinates) {
        return Integer.parseInt(coordinates.substring(0, 2));
    }

    public static String getCoordenadas(Local local) {
        if (local == null) {
            return "-";
        } else {
            return local.getCoordenadas();
        }
    }

    public SortedMap<String, Exercito> getExercitos(Local local) {
        return local.getExercitos();
    }

    /**
     * List alive chars at the hex
     *
     * @param local
     * @return
     */
    public SortedMap<String, Personagem> getPersonagens(Local local) {
        PersonagemFacade personagemFacade = new PersonagemFacade();
        final SortedMap<String, Personagem> ret = new TreeMap<>();
        for (Personagem personagem : local.getPersonagens().values()) {
            if (personagemFacade.isLocalConhecido(personagem)) {
                ret.put(personagem.getCodigo(), personagem);
            }
        }
        return ret;
    }

    public String getTerrenoNome(Local local) {
        try {
            return local.getTerreno().getNome();
        } catch (NullPointerException ex) {
            return "-";
        }
    }

    public String getClima(Local local) {
        try {
            return BaseMsgs.localClima[local.getClima()];
        } catch (NullPointerException ex) {
            return "-";
        }
    }

    public Cidade getCidade(Local local) {
        return local.getCidade();
    }

    public String getTerrenoCodigo(Local local) {
        return local.getTerreno().getCodigo();
    }

    /**
     * Deve ser utilizado quando importa o observador, no caso, se cidade é oculta.
     *
     * @param local
     * @param observer
     * @return
     */
    public boolean isCidade(Local local, Nacao observer) {
        try {
            final Cidade cidade = local.getCidade();
            return (cidade != null && (!cidade.isOculto() || cidade.getNacao() == observer));
        } catch (NullPointerException ex) {
            return false;
        }
    }

    /**
     * Este deve ser usado para verificar a existencia da cidade, sem importar se é oculta.
     *
     * @param local
     * @return
     */
    public boolean isCidade(Local local) {
        return (local.getCidade() != null);
    }

    public boolean isEstrada(Local local, int direcao) {
        return local.isEstrada(direcao);
    }

    public boolean isPonte(Local local, int direcao) {
        return local.isPonte(direcao);
    }

    public boolean isLanding(Local local, int direcao) {
        return local.isLanding(direcao);
    }

    public boolean isRiacho(Local local, int direcao) {
        return local.isRiacho(direcao);
    }

    public boolean isRio(Local local, int direcao) {
        return local.isRio(direcao);
    }

    public boolean isVau(Local local, int direcao) {
        return local.isVau(direcao);
    }

    public boolean isAncoravel(Local local) {
        return local.getTerreno().isAncoravel() || this.isDocas(local);
    }

    public boolean isDocas(Local local) {
        try {
            return local.getCidade().getDocas() > 0;
        } catch (NullPointerException ex) {
            return false;
        }
    }

    /**
     * Testa o lado em que esta cruzando mais o dois adjacentes por um rio.
     *
     * @param destino
     * @param direcao
     * @return
     */
    public boolean isRioLado(Local local, int direcao) {
        boolean ret = false;
        for (int ii = -1; !ret && ii <= 1; ii++) {
            int temp = ConverterFactory.getDirecao(direcao + ii);
            if (!ret && local.isRio(temp)) {
                ret = true;
            }
        }
        return ret;
    }

    public boolean isBarcos(Local local) {
        ExercitoFacade exercitoFacade = new ExercitoFacade();
        for (Exercito exercito : local.getExercitos().values()) {
            if (exercitoFacade.isEsquadra(exercito)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRastroExercito(Local local, int direcao) {
        return local.isRastro(direcao);
    }

    public Local getLocalVizinho(Local local, String direcao, SortedMap<String, Local> lista) {
        return lista.get(getIdentificacaoVizinho(local, direcao));
    }

    public Local getLocalVizinho(Local local, int direcao, SortedMap<String, Local> lista) {
        return lista.get(getIdentificacaoVizinho(local, direcao));
    }

    public String getIdentificacaoVizinho(Local atual, String nmDirecao) {
        return getIdentificacaoVizinho(atual, ConverterFactory.direcaoToInt(nmDirecao));
    }

    public String getIdentificacaoVizinho(Local atual, int direcao) {
        if (direcao == 0) {
            //short circuit response
            return atual.getCodigo();
        }
        //int newHex[] = this.identificacaoToInt();
        Integer lCol = LocalFacade.getCol(atual);
        Integer lRow = LocalFacade.getRow(atual);
        //calcula row & col
        switch (direcao) {
            case 0: //C 0 0
                break;
            case 1: //NO 0 -1
                if ((lRow % 2) == 1) {
                    lCol--;
                }
                lRow--;
                break;
            case 2: //NE +1 -1
                if ((lRow % 2) == 0) {
                    lCol++;
                }
                lRow--;
                break;
            case 3: //L +1 0
                lCol++;
                break;
            case 4: //SE +1 +1
                if ((lRow % 2) == 0) {
                    lCol++;
                }
                lRow++;
                break;
            case 5: //SO 0 +1
                if ((lRow % 2) == 1) {
                    lCol--;
                }
                lRow++;
                break;
            case 6: //O -1 0
                lCol--;
                break;
            default:
                //direcao inválida
                lCol = -1;
                lRow = -1;
                break;
        }
        //converte de volta para string e retorna o Hex
        return SysApoio.pointToCoord(lCol, lRow);
    }

    public int getDistancia(Local origem, Local destino) {
        if (origem == destino) {
            return 0;
        } else {
            int rowOrigem = LocalFacade.getRow(origem);
            int colOrigem = LocalFacade.getCol(origem);
            int rowDestino = LocalFacade.getRow(destino);
            int colDestino = LocalFacade.getCol(destino);
            return SysApoio.getDistancia(colOrigem, colDestino, rowOrigem, rowDestino);
        }
    }

    /**
     * Return highValue if there isn't a capital
     *
     * @param nation
     * @param destino
     * @return
     */
    public int getDistanciaToCapital(Nacao nation, Local destino) {
        Local capital;
        try {
            capital = nation.getCapital().getLocal();
        } catch (NullPointerException e) {
            //Return highValue if there isn't a capital
            return 9999;
        }
        return this.getDistancia(capital, destino);
    }

    public HashMap<Local, Integer> getLocalRange(Local local, int range, boolean borderOnly, SortedMap<String, Local> listLocais) {
        HashMap<Local, Integer> list = new HashMap<>();
        for (Local target : listLocais.values()) {
            final int distancia = getDistancia(local, target);
            if (borderOnly && distancia == range) {
                //no filling, border only
                list.put(target, distancia);
            } else if (distancia <= range) {
                //with filling
                list.put(target, distancia);
            }
        }
        return list;
    }

    public boolean isAgua(Local local) {
        return local.getTerreno().isAgua();
    }

    public boolean isVisible(Local local) {
        return local.isVisible();
    }

    public boolean isVisible(Local local, Jogador observer, World world) {
        final String flags = local.getVisibilidadeNacao();
        if (flags == null || flags.trim().equals("")) {
            //no one has visibility
            return false;
        }
        String[] elem = flags.split(";");
        for (String elem1 : elem) {
            String[] nat = elem1.split("=");
            if (SysApoio.parseInt(nat[1]) <= 0) {
                continue;
            }
            for (Nacao nation : world.getNacoes().values()) {
                if (SysApoio.parseInt(nat[0]) != nation.getId()) {
                    continue;
                }
                if (observer.isJogadorAliado(nation)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param personagem
     * @param tipo
     * @param tipo 0 = todos - self
     * @param tipo 1 = mesma nacao - self
     * @param tipo 2 = outras nacoes - self
     * @param tipo 3 = em exercito e com pericia de comandante, qualquer nacao - self + Null (optional)
     * @param tipo 4 = todos + self
     * @return
     */
    public Personagem[] listPersonagemLocal(Local local, Personagem personagem, int tipo) {
        List<Personagem> ret = new ArrayList();
        switch (tipo) {
            case 0:
                ret.addAll(local.getPersonagens().values());
                ret.remove(personagem);
                break;
            case 1:
                for (Personagem pers : local.getPersonagens().values()) {
                    if (personagem != pers && personagem.getNacao() == pers.getNacao()) {
                        ret.add(pers);
                    }
                }
                ret.remove(personagem);
                break;
            case 2:
                ret.addAll(local.getPersonagens().values());
                ret.removeAll(personagem.getNacao().getPersonagens());
                ret.remove(personagem);
                break;
            case 3:
                for (Personagem pers : local.getPersonagens().values()) {
                    if (pers.isComandante()) {
                        ret.add(pers);
                    }
                }
                if (personagem != null && personagem.isComandaExercito()) {
                    ret.remove(personagem);
                }
                break;
            case 4:
                ret.addAll(local.getPersonagens().values());
                break;
            default:
                break;
        }
        return (Personagem[]) ret.toArray(new Personagem[0]);
    }

    public Personagem[] listPersonagemLocal(Local local, Nacao nacao) {
        List<Personagem> ret = new ArrayList();
        for (Personagem pers : local.getPersonagens().values()) {
            if (nacao == pers.getNacao() && !pers.isHero()) {
                //check if hero
                ret.add(pers);
            }
        }
        return (Personagem[]) ret.toArray(new Personagem[0]);
    }

    public void doClearVisibility(Local local, Partida game) {
        if (!local.isVisible()) {
            local.setRastro("");
            //remove all terrain landmarks
            this.remTerrainLandmark(local);
            //remove volatile
            local.remHabilidade(";LHO;");
            //remove battle sites
            local.remHabilidade(";LHC;");
            if (!game.isInformationNetwork()) {
                //battle sites visible if GIN is enabled for the game
                local.remHabilidade(";LHCA;");
                local.remHabilidade(";LHCN;");
            }
        }
        if (!local.isProducaoInfo()) {
            local.clearProducao();
        }
        local.setVisibilidadeNacao("");
    }

    public boolean isCombatTookPlace(Local local) {
        return local.hasHabilidade(";LHC;");
    }

    public boolean isCombatTookPlaceBigNavy(Local local) {
        return local.hasHabilidade(";LHCN;");
    }

    public boolean isCombatTookPlaceBigArmy(Local local) {
        return local.hasHabilidade(";LHCA;");
    }

    public boolean isOverrunTookPlace(Local local) {
        return local.hasHabilidade(";LHO;");
    }

    public boolean isTerrenoMontanhaColina(Terreno terreno) {
        return terreno.isMontanha() || terreno.isColina();
    }

    public boolean hasTerrainLandmark(Local local) {
        for (String landmark : getTerrainLandmarksImage().keySet()) {
            if (local.hasHabilidade(landmark)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTerrainLandmark(Habilidade landmark) {
        return landmark.hasHabilidade(";FFL;");
    }

    public boolean isTerrainLandmarkSpire(Local local) {
        return local.hasHabilidade(";LFT;");
    }

    public boolean isTerrainLandmarkLake(Local local) {
        return local.hasHabilidade(";LFK;");
    }

    public boolean isTerrainLandmarkLith(Local local) {
        return local.hasHabilidade(";LFL;");
    }

    public boolean isTerrainLandmark(Local local) {
        for (Habilidade landmark : local.getHabilidades().values()) {
            if (isTerrainLandmark(landmark)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTerrainLandmarkCave(Local local) {
        return local.hasHabilidade(";LFC;");
    }

    public boolean isTerrainLandmarkHenge(Local local) {
        return local.hasHabilidade(";LFH;");
    }

    public boolean isTerrainLandmarkIgloo(Local local) {
        return local.hasHabilidade(";LFI;");
    }

    public boolean isTerrainLandmarkRuin(Local local) {
        return local.hasHabilidade(";LFR;");
    }

    public boolean isTerrainLandmarkTemple(Local local) {
        return local.hasHabilidade(";LFE;");
    }

    public boolean isTerrainLandmarkSpent(Local local) {
        return local.hasHabilidade(";LFX;");
    }

    public List<Habilidade> getTerrainLandmark(Local local) {
        List<Habilidade> ret = new ArrayList<>();
        for (Habilidade landmark : local.getHabilidades().values()) {
            if (isTerrainLandmark(landmark)) {
                ret.add(landmark);
            }
        }
        return ret;
    }

    public SortedMap<String, String> getTerrainLandmarksImage() {
        return landmarkImage;
    }

    public Set<String> getTerrainLandmarksCodigo() {
        return landmarkImage.keySet();
    }

    public boolean remTerrainLandmark(Local local) {
        boolean ret = false;
        final List<Habilidade> list = new ArrayList<>(local.getHabilidades().values());
        for (Habilidade hab : list) {
            if (hab.hasHabilidade(";FFL;")) {
                local.remHabilidade(hab);
                ret = true;
            }
        }
        return ret;
    }

    public void addHabilidade(Local local, Habilidade hab) {
        local.addHabilidade(hab);
    }

    /**
     *
     * @param baseLocal the value of baseHex
     * @param range the value of range
     */
    public Set<Local> listLocalRange(Local baseLocal, int range, SortedMap<String, Local> lista) {
        final Set<Local> visibleHexes = new TreeSet<>();
        final Set<Local> borderHexes = new TreeSet<>();
        //cant mutate borderHex while in a loop.
        final Set<Local> newBorderHexes = new TreeSet<>();

        //add self
        visibleHexes.add(baseLocal);
        //initial seed for border
        borderHexes.add(baseLocal);
        for (int radius = 1; radius <= range; radius++) {
            for (Local border : borderHexes) {
                for (int ii = 1; ii < 7; ii++) {
                    Local localVizinho = this.getLocalVizinho(border, ii, lista);
                    if (localVizinho == null) {
                        //edge of map
                        continue;
                    }
                    if (visibleHexes.contains(localVizinho)) {
                        //Already done, inside the border
                        continue;
                    }
                    //list border for the next loop, overlap removed by Set.
                    newBorderHexes.add(localVizinho);
                }
            }
            //clear list for a new hex
            borderHexes.clear();
            borderHexes.addAll(newBorderHexes);
            //add all visible to final list. Set will remove intersection
            visibleHexes.addAll(newBorderHexes);
            //clear new border
            newBorderHexes.clear();
        }
        return visibleHexes;
    }
}
