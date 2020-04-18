/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import business.ArmyPath;
import business.MovimentoExercito;
import business.combat.ArmySim;
import business.converter.ConverterFactory;
import business.interfaces.IExercito;
import business.services.ComparatorFactory;
import business.services.TitleFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import model.Cenario;
import model.Exercito;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Personagem;
import model.Terreno;
import model.TipoTropa;
import msgs.BaseMsgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import utils.StringIntSortedCell;

/**
 *
 * @author gurgel
 */
public class ExercitoFacade implements Serializable {

    private static final Log log = LogFactory.getLog(ExercitoFacade.class);
    public static final int SHIP_CAPACITY = 250;
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final CenarioFacade cenarioFacade = new CenarioFacade();
    private static final LocalFacade localFacade = new LocalFacade();
    private static final CidadeFacade cidadeFacade = new CidadeFacade();
    private static final BattleSimFacade battleSimFacade = new BattleSimFacade();

    public String getClima(Exercito exercito) {
        return localFacade.getClima(this.getLocal(exercito));
    }

    /*
     * funcao a ser usada na fase de relatorios
     */
    public String getDescricaoTamanho(Exercito exercito) {
        String ret;
        if (isGuarnicao(exercito)) {
            if (isEsquadra(exercito) || exercito.getTamanhoEsquadra() > 0) {
                ret = BaseMsgs.guarnicaoTamanho[exercito.getTamanhoEsquadra()];
            } else {
                ret = BaseMsgs.guarnicaoTamanho[exercito.getTamanhoExercito()];
            }
        } else if (isEsquadra(exercito) || exercito.getTamanhoEsquadra() > 0) {
            ret = BaseMsgs.esquadraTamanho[exercito.getTamanhoEsquadra()];
        } else {
            ret = BaseMsgs.exercitoTamanho[exercito.getTamanhoExercito()];
        }
        return ret;
    }

    public StringIntSortedCell getTamanhoCell(Exercito exercito) {
        if (isGuarnicao(exercito)) {
            if (isEsquadra(exercito) || exercito.getTamanhoEsquadra() > 0) {
                return ConverterFactory.getArmySizeCell(exercito.getTamanhoEsquadra(), BaseMsgs.guarnicaoTamanho[exercito.getTamanhoEsquadra()]);
            } else {
                return ConverterFactory.getArmySizeCell(exercito.getTamanhoExercito(), BaseMsgs.guarnicaoTamanho[exercito.getTamanhoExercito()]);
            }
        } else if (isEsquadra(exercito) || exercito.getTamanhoEsquadra() > 0) {
            return ConverterFactory.getArmySizeCell(exercito.getTamanhoEsquadra(), BaseMsgs.esquadraTamanho[exercito.getTamanhoEsquadra()]);
        } else {
            return ConverterFactory.getArmySizeCell(exercito.getTamanhoExercito(), BaseMsgs.exercitoTamanho[exercito.getTamanhoExercito()]);
        }
    }

    public String getTerreno(Exercito exercito) {
        return localFacade.getTerrenoNome(this.getLocal(exercito));
    }

    public String getComandanteTitulo(Exercito exercito, Cenario cenario) {
        if (isGuarnicao(exercito)) {
            return String.format(labels.getString("GUARNICAO.NOME"),
                    labels.getString("GUARNICAO"),
                    getNacaoNome(exercito),
                    getNomeLocal(exercito));
        } else {
            try {
                return String.format("%s %s",
                        cenarioFacade.getTituloPericia(cenario,
                                CenarioFacade.COMANDANTE,
                                exercito.getComandante().getPericiaComandanteNatural()),
                        exercito.getComandante().getNome());
            } catch (NullPointerException ex) {
                return labels.getString("COMANDANTE.DESCONHECIDO");
            }
        }
    }

    public String getNomeTituloComandante(IExercito army) {
        return this.getTituloComandante(army) + " " + army.getComandanteNome();
    }

    public String getTituloComandante(IExercito army) {
        /**
         * general mariola
         */
        try {
            final int nn = (int) Math.min((army.getComandantePericia() / 10) + 1, BaseMsgs.tituloPericiaComandante.length);
            //No passado, fiz um merge e tinha o nome do comandnate aqui. Talvez o nome do comandnate esteja faltando em algum lugar, dai use getNomeTituloComandante()
            /*
                return (String.format("%s %s",
                    BaseMsgs.tituloPericiaComandante[nn],
                    army.getComandanteNome()));

             */
            return BaseMsgs.tituloPericiaComandante[nn];
        } catch (NullPointerException e) {
            return labels.getString("UM.DESCONHECIDO");
        } catch (ArrayIndexOutOfBoundsException e) {
            return labels.getString("UM.DESCONHECIDO");
        }
    }

    public String getNomeTituloNacaoComandante(IExercito army) {
        String ret = "";
        if (army.isGarrison()) {
            ret += String.format("%s (%s)", labels.getString("GUARNICAO"), army.getLocal().getCoordenadas());
        } else {
            ret += getNomeTituloComandante(army);
        }
        ret += " " + army.getNacao().getNome();
        return ret;
    }

    public String getComandanteTitulo(ArmySim exercito, Cenario cenario) {
        try {
            return String.format("%s %s",
                    cenarioFacade.getTituloPericia(cenario,
                            CenarioFacade.COMANDANTE,
                            exercito.getComandantePericia()),
                    exercito.getComandanteNome());
        } catch (NullPointerException ex) {
            return String.format(labels.getString("GUARNICAO.NOME"),
                    labels.getString("GUARNICAO"),
                    getNacaoNome(exercito),
                    getNomeLocal(exercito));
        }
    }

    public int getPericiaComandante(IExercito exercito) {
        try {
            return exercito.getComandantePericia();
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public int getComida(Exercito exercito) {
        return exercito.getComida();
    }

    public int getUpkeepCost(Exercito exercito) {
        int ret = 0;
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            ret += pelotao.getQtd() * pelotao.getTipoTropa().getUpkeepMoney();
        }
        return ret;
    }

    public int getUpkeepFood(Exercito exercito) {
        int ret = 0;
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            ret += pelotao.getQtd() * pelotao.getTipoTropa().getUpkeepFood();
        }
        return ret;
    }

    public int getMoral(IExercito exercito) {
        return exercito.getMoral();
    }

    public Jogador getJogador(Exercito exercito) {
        try {
            return exercito.getNacao().getOwner();
        } catch (NullPointerException ex) {
            //faz nada, so retorna null
            return null;
        }
    }

    public SortedMap<String, Personagem> getLiderados(Exercito exercito) {
        try {
            return exercito.getComandante().getLiderados();
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public Local getLocal(Exercito exercito) {
        try {
            return exercito.getLocal();
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public Nacao getNacao(Exercito exercito) {
        try {
            return exercito.getNacao();
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public String getNacaoNome(IExercito exercito) {
        try {
            return exercito.getNacao().getNome();
        } catch (NullPointerException ex) {
            return "-";
        }
    }

    public int getNacaoNumero(Exercito exercito) {
        return Integer.valueOf(exercito.getNacao().getCodigo());
    }

    /**
     * to be deprecated, no more troops per race renaming...
     *
     * @param exercito
     * @param pelotao
     * @return
     */
    public String getNomeRaca(IExercito exercito, Pelotao pelotao) {
//        return exercito.getNacao().getRaca().getTropaDescricao(pelotao.getCodigo());
        return pelotao.getTipoTropa().getNome();
    }

    public boolean isGuarnicao(IExercito exercito) {
        return exercito.isGarrison();
    }

    public boolean isComida(Exercito exercito) {
        try {
            int foodAvailable = exercito.getComida();
            try {
                if (localFacade.isCidade(exercito.getLocal()) && exercito.getLocal().getCidade().getNacao() == exercito.getNacao()) {
                    foodAvailable += cidadeFacade.getFoodGiven(exercito.getLocal().getCidade());
                }
            } catch (NullPointerException ex) {
                //just ignore.
            }
            return (foodAvailable >= getComidaConsumo(exercito));
        } catch (NullPointerException e) {
            return false;
        }
    }

    private int getComidaConsumo(Exercito exercito) {
        int ret = 0;
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            ret += pelotao.getQtd() * pelotao.getTipoTropa().getUpkeepFood();
        }
        return ret;
    }

    private boolean isHabilidade(IExercito exercito, String habilidade) {
        boolean ret = false;
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            if (pelotao.getTipoTropa().hasHabilidade(habilidade) && pelotao.getQtd() > 0) {
                ret = true;
                //so precisa ter uma...
                break;
            }
        }
        return ret;
    }

    private int getQtHabilidade(Exercito exercito, String habilidade) {
        int ret = 0;
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            if (pelotao.getTipoTropa().hasHabilidade(habilidade) && pelotao.getQtd() > 0) {
                ret += pelotao.getQtd();
            }
        }
        return ret;
    }

    private int getQtHabilidade(Exercito exercito, String habYes, String habNot) {
        int ret = 0;
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            if (pelotao.getQtd() > 0
                    && pelotao.getTipoTropa().hasHabilidade(habYes)
                    && !pelotao.getTipoTropa().hasHabilidade(habNot)) {
                ret += pelotao.getQtd();
            }
        }
        return ret;
    }

    /*
     * Lista habilidades C = Cavalaria, usa tabela demovimento de cavalaria T =
     * Transporte A = Ataque N = Navy, anda na agua, usa tabela de movimento de
     * esquadra W = War Machines E = explorar gratis F = anda em qualquer
     * terreno I = infantaria (soldados a pe, usa tabela demovimentod e
     * infantaria)
     */
    public boolean hasExplorar(Exercito exercito) {
        return isHabilidade(exercito, ";TTE;");
    }

    public boolean isEsquadra(IExercito exercito) {
        return isHabilidade(exercito, ";TTN;");
    }

    public int getEsquadra(Exercito exercito) {
        return getQtHabilidade(exercito, ";TTN;");
    }

    public int getSiege(Exercito exercito) {
        return getQtHabilidade(exercito, ";TTS;");
    }

    public int getTransportesMinimo(Exercito exercito) {
        return (int) Math.ceil(this.getTransportesCargoUsed(exercito.getPelotoes()) / ExercitoFacade.SHIP_CAPACITY);
    }

    public int getTransportesMinimo(SortedMap<String, Pelotao> pelotoes) {
        return (int) Math.ceil(this.getTransportesCargoUsed(pelotoes) / ExercitoFacade.SHIP_CAPACITY);
    }

    public int getTransportesMinimo(Pelotao pelotao) {
        return (int) Math.ceil(this.getTransportesCargoUsed(pelotao) / ExercitoFacade.SHIP_CAPACITY);
    }

    public int getTransportesAvailable(SortedMap<String, Pelotao> pelotoes) {
        float capacidade = 0 - ExercitoFacade.this.getTransportesCargoUsed(pelotoes);
        for (Pelotao pelotao : pelotoes.values()) {
            capacidade += getTransportesAvailable(pelotao);
        }
        return (int) capacidade;
    }

    public int getTransportesAvailable(Exercito exercito) {
        return getTransportesAvailable(exercito.getPelotoes());
    }

    public int getTransportesAvailable(Pelotao pelotao) {
        if (pelotao.getTipoTropa().hasHabilidade(";TTT;")) {
            //conta capacidade de carga
            return pelotao.getQtd() * ExercitoFacade.SHIP_CAPACITY;
        } else {
            return 0;
        }
    }

    public int getTransportesCapacity(SortedMap<String, Pelotao> pelotoes) {
        float capacidade = 0F;
        for (Pelotao pelotao : pelotoes.values()) {
            capacidade += getTransportesCapacity(pelotao);
        }
        return (int) capacidade;
    }

    public int getTransportesCapacity(Exercito exercito) {
        return getTransportesCapacity(exercito.getPelotoes());
    }

    public int getTransportesCapacity(Pelotao pelotao) {
        if (pelotao.getTipoTropa().hasHabilidade(";TTT;")) {
            //conta capacidade de carga
            return pelotao.getQtd() * ExercitoFacade.SHIP_CAPACITY;
        } else {
            return 0;
        }
    }

    public float getTransportesCargoUsed(SortedMap<String, Pelotao> pelotoes) {
        float carga = 0F;
        for (Pelotao pelotao : pelotoes.values()) {
            carga += ExercitoFacade.this.getTransportesCargoUsed(pelotao);
        }
        return carga;
    }

    public float getTransportesCargoUsed(Pelotao pelotao) {
        return getTransportesCargoUsed(pelotao.getTipoTropa(), pelotao.getQtd());
    }

    public float getTransportesCargoUsed(TipoTropa tropa, int qtd) {
        if (tropa.hasHabilidade(";TTT;")) {
            //conta capacidade de carga
            //capacidade += pelotao.getQtd() * SHIP_CAPACITY;
            return 0;
        } else if (tropa.isBarcos()) {
            //nao conta outros navios...
            return 0;
        } else if (tropa.hasHabilidade(";TT2;")) {
            //se cavalaria, conta dobrado
            return qtd * 2f;
        } else {
            return qtd;
        }
    }

    /**
     * Count cavalary - ships
     *
     * @param exercito
     * @return
     */
    public int getQtTropasCavalaria(Exercito exercito) {
        return getQtHabilidade(exercito, ";TTF;", ";TTN;");
    }

    /**
     * Count infantry - ships
     *
     * @param exercito
     * @return
     */
    public int getQtTropasInfantaria(Exercito exercito) {
        return getQtHabilidade(exercito, ";TTR;", ";TTN;");
    }

    /**
     * count how many troops total
     *
     * @param exercito
     * @return
     */
    public int getQtTropasTotal(IExercito exercito) {
        int ret = 0;
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            ret += pelotao.getQtd();
        }
        return ret;
    }

    public int getTamanhoEsquadra(Exercito exercito) {
        return exercito.getTamanhoEsquadra();
    }

    public SortedMap<String, Pelotao> getPelotoes(Exercito exercito) {
        return exercito.getPelotoes();
    }

    public Pelotao getPlatoon(IExercito army, TipoTropa tpTrop) {
        return army.getPelotoes().get(tpTrop.getCodigo());
    }

    public int getCustoMovimentoBase(List<TipoTropa> tropas, Terreno destino, boolean estrada, boolean agua) {
        double ret = 1;
        try {
            for (TipoTropa tpTropa : tropas) {
                double base = tpTropa.getMovimentoTerreno().get(destino);
                if (base == 0) {
                    base = 999;
                }
                //modifica custo basico pelo valor da estrada
                if (agua && !tpTropa.isBarcos()) {
                    base = -1;
                } else if (estrada) {
                    //se rapido ou regular
                    if (tpTropa.hasHabilidade(";TTR;")) {
                        base = Math.ceil(base / 2);
                    } else if (tpTropa.hasHabilidade(";TTF;")) {
                        base = Math.floor(base / 2);
                    } else {
                    }
                    if (tpTropa.hasHabilidade(";TM2;") && destino.isMontanha()) {
                        //half cost on mountain roads
                        base = Math.ceil(base / 2);
                    }
                } else if (tpTropa.hasHabilidade(";T2M;") && destino.isMontanha()) {
                    //half cost on mountain 
                    base = Math.ceil(base / 2);
                }

                ret = Math.max(ret, base);
            }
        } catch (NullPointerException e) {
        }
        return (int) ret;
    }

    public boolean isAgua(TipoTropa tpTropa) {
        return tpTropa.isBarcos();
    }

    public String getNomeLocal(IExercito exercito) {
        try {
            if (exercito.getLocal().getCidade().getNome() != null) {
                return exercito.getLocal().getCidade().getNome();
            } else {
                //se nao tem nome da cidade, imprime coordenadas do hex.
                return exercito.getLocal().getCoordenadas();
            }
        } catch (NullPointerException ex) {
            //se nao tem cidade, imprime coordenadas do hex.
            return exercito.getLocal().getCoordenadas();
        }
    }

    public Exercito getGuarnicao(Nacao nacao, Local local) {
        for (Exercito exercito : local.getExercitos().values()) {
            if (isGuarnicao(exercito) && nacao == getNacao(exercito)) {
                return exercito;
            }
        }
        return null;
    }

    public int getAtaquePelotao(Pelotao pelotao, IExercito exercito) {
        return (int) battleSimFacade.getPlatoonAttack(pelotao, exercito);
    }

    public int getDefesaPelotao(Pelotao pelotao, IExercito exercito) {
        return (int) battleSimFacade.getPlatoonDefense(exercito, pelotao);
    }

    public int getAtaqueExercito(IExercito exercito, boolean naval) {
        return (int) battleSimFacade.getArmyAttack(exercito, naval);
    }

    public int getDefesaExercito(IExercito exercito, boolean naval) {
        return battleSimFacade.getArmyDefense(exercito, naval);
    }

    public int getAtaqueBonusExercito(IExercito exercito) {
        return (int) battleSimFacade.getArmyAttackBonus(exercito);
    }

    public int getDefesaBonusExercito(IExercito exercito) {
        return battleSimFacade.getArmyDefenseBonus(exercito);
    }

    public List<TipoTropa> getTipoTropas(Exercito exercito) {
        List<TipoTropa> tropas = new ArrayList<TipoTropa>();
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            tropas.add(pelotao.getTipoTropa());
        }
        return tropas;
    }

    public List<TipoTropa> getTipoTropasTerra(Exercito exercito) {
        List<TipoTropa> tropas = new ArrayList<TipoTropa>();
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            if (!pelotao.getTipoTropa().isBarcos()) {
                tropas.add(pelotao.getTipoTropa());
            }
        }
        return tropas;
    }

    public List<TipoTropa> getTipoTropasAgua(Exercito exercito) {
        List<TipoTropa> tropas = new ArrayList<TipoTropa>();
        for (Pelotao pelotao : exercito.getPelotoes().values()) {
            if (pelotao.getTipoTropa().isBarcos()) {
                tropas.add(pelotao.getTipoTropa());
            }
        }
        return tropas;
    }

    public HashMap<Local, ArmyPath> calculateArmyRangeHexes(SortedMap<String, Local> locais, MovimentoExercito movEx) {
        //range com custo para o hex
        HashMap<Local, ArmyPath> rangeHexes = new HashMap(); //hex, path to get here
        Local startHex = movEx.getOrigem();
        if (!movEx.isPorAgua() && localFacade.isAgua(startHex)) {
            //exercito na agua nao pode mover como terra....
            return rangeHexes;
        }
        //custo zero para o atual
        //rangeHexes.put(startHex, 0);
        //apoio para processartodos hexes no alcance
        LinkedList<Local> hexesToProcess = new LinkedList();
        //comeca pelo hex atual...
        hexesToProcess.add(startHex);

        int prevCost;
        while (hexesToProcess.size() > 0) {
            Local atual = hexesToProcess.remove(0);
            for (int direcao = 1; direcao < 7; direcao++) {
                Local vizinho = locais.get(localFacade.getIdentificacaoVizinho(atual, direcao));
                if (vizinho == null) {
                    //off map
                    continue;
                }
                if (vizinho == startHex) {
                    //ignora hex inicial
                    continue;
                }
                //pega custo de mover para vizinho
                int cost = calculateMovementCostForArmy(movEx, atual, vizinho, direcao);
                if (cost > 99) {
                    //movimento invalido, skip
                    continue;
                }
                //pega custo acumulado do hex atual
                try {
                    prevCost = rangeHexes.get(atual).getCost();
                } catch (NullPointerException ex) {
                    //not found, new path
                    prevCost = 0;
                }
                if (movEx.isPorAgua() && !localFacade.isAgua(vizinho)) {
                    //overwrite cost to end water movement. consuming all remaining points for the move, minimum of 1.
                    cost = Math.max(1, movEx.getLimiteMovimento() - prevCost);
                }
                final int totalCost = cost + prevCost;
                if (totalCost > 0 && totalCost <= movEx.getLimiteMovimento()) {
                    ArmyPath path;
                    if (rangeHexes.containsKey(vizinho)) {
                        ArmyPath pathOld = rangeHexes.get(vizinho);
                        int currentCost = pathOld.getCost();
                        if (currentCost <= totalCost) {
                            //ja tem um path mais barato ou igual
                            continue;
                        }
                    }
                    if (rangeHexes.containsKey(atual)) {
                        //pega o path ate o atual
                        ArmyPath pathOld = rangeHexes.get(atual);
                        try {
                            path = pathOld.clone();
                            if (path == pathOld) {
                                log.info("igual!");
                            }
                        } catch (CloneNotSupportedException ex) {
                            throw new UnsupportedOperationException(ex);
                        }
                    } else {
                        //path novo
                        path = new ArmyPath(atual);
                    }
                    //atualiza custo total, se menor
                    path.setCost(totalCost);
                    path.addStep(movEx);
                    //adiciona ao range 
                    rangeHexes.put(vizinho, path);
//                    log.info(vizinho.getCoordenadas() + " - " + totalCost);
                    //tem como melhorar aki???
                    if (!hexesToProcess.contains(vizinho)) {
                        hexesToProcess.add(vizinho);
                    }
                }
            }
//            log.info("atual processado: " + atual.getCoordenadas());
        }
        return rangeHexes;
    }

    private int calculateMovementCostForArmy(MovimentoExercito movEx, Local atual, Local destino, int direcao) {
        //cria objeto com todas as informacoes;
        movEx.setOrigem(atual);
        movEx.setDestino(destino);
        movEx.setDirecao(direcao);
        //FIXME: tem que lembrar cada movimento para identificar se mudando de corpo dagua.
        //movEx.setDirecaoAnterior(this.anterior);
        return movEx.getCustoMovimento();
    }

    public String getTacticNameSelected(IExercito exercito) {
        return TitleFactory.getTaticaNome(exercito.getTatica());
    }

    public boolean isTropaHabilidadeUma(IExercito army, String habilidade) {
        for (Pelotao platoon : army.getPelotoes().values()) {
            if (platoon.getQtd() > 0 && platoon.getTipoTropa().hasHabilidade(habilidade)) {
                //so precisa ter uma...
                return true;
            }
        }
        return false;
    }

    public boolean isSiege(IExercito army) {
        return isTropaHabilidadeUma(army, ";TTS;");
    }

    public boolean isHero(IExercito army) {
        try {
            return army.getComandanteModel().isHero();
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public List<Pelotao> listaTropasTerra(IExercito army) {
        //prepara lista de Land
        List<Pelotao> tropas = new ArrayList<Pelotao>();
        for (Pelotao pelotao : army.getPelotoes().values()) {
            if (!pelotao.getTipoTropa().isBarcos()) {
                tropas.add(pelotao);
            }
        }
        //ordena a lista de pelotoes pelo Id
//        Collections.sort(tropas, ComparatorFactory.getComparatorCasualtiesSorter(getTatica(), getHexagono().getTerrenoCenario().getTerreno()));
        ComparatorFactory.getComparatorCasualtiesPelotaoSorter(tropas, army.getTatica(), army.getTerreno());
        return tropas;
    }

    public void subTropaQt(IExercito army, TipoTropa troopType, int qtTroops) {
        if (qtTroops <= 0) {
            return;
        }
        try {
            final SortedMap<String, Pelotao> platoons = army.getPelotoes();
            Pelotao pelotao = platoons.get(troopType.getCodigo());
            int temp = pelotao.getQtd() - qtTroops;
            if (temp > 0) {
                pelotao.setQtd(temp);
            } else {
                pelotao.setQtd(0);
                platoons.remove(pelotao.getCodigo());
            }
            if (this.getQtTropasTotal(army) < 1) {
                //debanda exercito
                army.setDisband(true);
            }
        } catch (NullPointerException e) {
            //Pelotao nao existe, logo nao precisa subtrair.
            //deve ocorrer apenas com Barcos e maquinas de guerra. um dia bola uma coisa melhor
        }
        this.isPrecisaDebandar(army);
    }

    public boolean isPrecisaDebandar(IExercito army) {
        if (this.getQtTropasTotal(army) >= 1 && !army.isDisband()) {
            //no need to disband
            return false;
        }

        army.doDisbandWithMsg();
        return true;
    }
}
