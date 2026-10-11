/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import baseLib.BaseModel;
import business.converter.ConverterFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Cenario;
import model.Cidade;
import model.Habilidade;
import model.Local;
import model.Mercado;
import model.Nacao;
import model.Ordem;
import model.Personagem;
import model.PersonagemOrdem;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 *
 * @author gurgel
 */
public class AcaoFacade implements Serializable {

    private static final Log log = LogFactory.getLog(AcaoFacade.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final LocalFacade localFacade = new LocalFacade();
    private final NacaoFacade nacaoFacade = new NacaoFacade();

    public String getImproveRank(Ordem ordem) {
        try {
            return SysApoio.iif(ordem.isImprove(), labels.getString("SIM"), labels.getString("NAO"));
        } catch (NullPointerException e) {
            return "-";
        }
    }

    public boolean isPointsSetupUnderLimit(BaseModel actor, int nationPackagesLimit) {
        if (actor.isNacaoClass()) {
            Nacao nacao = (Nacao) actor;
            final int current = getPointsSetup(nacao);
            return current < nationPackagesLimit;
        }
        return false;
    }

    public int getPointsSetup(Ordem ordem) {
        try {
            return ordem.getHabilidadesPoints();
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public int getPointsSetup(Nacao nacao) {
        int points = 0;
        for (PersonagemOrdem po : nacao.getAcoes().values()) {
            try {
                points += getPointsSetup(po.getOrdem());
            } catch (NullPointerException ex) {
            }
        }
        return points;
    }

    public int getCusto(Ordem ordem) {
        try {
            return ordem.getCusto();
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public int getCusto(PersonagemOrdem order) {
        try {
            return order.getOrdem().getCusto();
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public int getCusto(PersonagemOrdem order, Nacao nation, Cenario cenario, Mercado market) {
        try {
            switch (order.getOrdem().getCodigo()) {
                case "411":
                    //find qt tropas
                    int qtTroops = SysApoio.parseInt(order.getParametrosId().get(0));
                    if (qtTroops == 0) {
                        //find actor
                        qtTroops = nacaoFacade.getCidadeRecruitingLimit(nation, order.getNome(), cenario);
                    }
                    //find cost of troops
                    TipoTropa troop = cenario.getTipoTropas().get(order.getParametrosId().get(1));
                    int vlCost = troop.getRecruitCostMoney();
                    //assume will recruit maximum allowed numbers
                    return qtTroops * vlCost;
                case "550":
                    return (nacaoFacade.getCidadeTamanho(nation, order.getNome()) + 1) * 2000;
                case "494":
                    final Cidade city = nacaoFacade.getCidade(nation, order.getNome());
                    return nacaoFacade.getCidadeFortificacaoCusto(city);
                case "315":
                    int productType = ConverterFactory.estoqueToInt(order.getParametrosId().get(0));
                    int qtGold = market.getProdutoVlCompra(ConverterFactory.intToProduto(productType, market.getProdutos()));
                    int qtProduct = SysApoio.parseInt(order.getParametrosId().get(1));
                    return qtProduct * qtGold;
                default:
                    return order.getOrdem().getCusto();
            }
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public Collection<Habilidade> getHabilidades(Ordem ordem) {
        return ordem.getHabilidades().values();
    }

    public boolean isSetup(Ordem ordem) {
        return ordem.isNacaoOrdem();
    }

    public String getSetupDescription(Ordem ordem) {
        String ret = "";
        for (Habilidade hab : ordem.getHabilidades().values()) {
            if (hab.isPackage()) {
                for (Habilidade habilidade : hab.getHabilidades().values()) {
                    ret += habilidade.getNome() + "\n";
                }
            }
        }
        return ret;
    }

    public boolean isScout(PersonagemOrdem po) {
        try {
            return po.getOrdem().hasHabilidade(";ASR;");
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * The orders that raise a city's SIZE, which is what the {@code ;SCC;} proximity cap governs.
     *
     * Keyed on the order code rather than on an order habilidade, because the code IS an order's
     * behaviour here: PbmJudge's {@code OrdemJudgeFactory} dispatches on {@code switch (getCdOrdem())}
     * and these six are exactly the codes it routes to {@code OrdemBaseImproveCity}, whose
     * {@code criticaRequisitos} applies the cap. A code outside the set never reaches that class, so
     * the set cannot drift from the server short of someone editing that switch. 548 is in it because
     * it raises the size as well as the fortification.
     */
    private static final Set<Integer> IMPROVE_CITY_SIZE_ORDERS =
            new HashSet<>(Arrays.asList(548, 550, 551, 552, 553, 554));

    /** @see #IMPROVE_CITY_SIZE_ORDERS */
    public boolean isImproveCitySize(PersonagemOrdem po) {
        try {
            return IMPROVE_CITY_SIZE_ORDERS.contains(SysApoio.parseInt(po.getOrdem().getCodigo()));
        } catch (NullPointerException e) {
            return false;
        }
    }

    public boolean isMovimento(PersonagemOrdem po) {
        try {
            return po.getOrdem().isTipoMovimentacao();
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * True when this order relocates the character issuing it AND the destination can be computed
     * from the parameters alone.
     *
     * <b>A movement-TYPED order is not necessarily a move.</b> Over the whole {@code ordem_cenario}
     * catalogue, {@code tp_ordem='Mov'} covers sixteen orders and exactly two of them leave the
     * character where it stands - {@code Bloq} (840), which sets a blockade facing and subtracts
     * morale, and {@code MovCity} (845), which moves a CITY. Those two are the only ones whose first
     * parameter is a bare {@code Direcao}; every real character move carries either a destination
     * {@code Coordenada} or a plotted {@code Direcao_Ex} path. So the test is on parameter SHAPE and
     * needs no list of codes, which is what keeps it true for a scenario that adds a movement order
     * tomorrow.
     * <p>
     * {@code Coordenada} is looked for in ANY position, not just the first: {@code ArtMov} (826,
     * "Use Movement Magic Item") carries it behind the item and {@code FeitMov} (825, "Cast Movement
     * Spell") behind the spell, and both land the character on that hex.
     *
     * @see #isStationaryMovement
     */
    public boolean movesTheCharacter(PersonagemOrdem po) {
        if (!isMovimento(po)) {
            return false;
        }
        for (int ii = 0; ii < po.getOrdem().getParametrosIdeQtd(); ii++) {
            if (po.getOrdem().getParametroIde(ii).contains("Coordenada")) {
                return true;
            }
        }
        return po.getOrdem().getParametroIde(0).contains("Direcao_Ex");
    }

    /**
     * True for a movement-typed order that is KNOWN not to relocate the character: a bare
     * {@code Direcao} first parameter, which across the catalogue is {@code Bloq} and {@code MovCity}
     * and nothing else.
     *
     * This is deliberately not the negation of {@link #movesTheCharacter}. A third case exists - a
     * movement order that does relocate the character to a hex the parameters do not name, such as
     * {@code ArtMov} 805, which carries only {@code Artefato_Carried}. For that one the honest answer
     * is "they move, I cannot say where", and a caller that must not guess needs to tell it apart
     * from "they stay put".
     */
    public boolean isStationaryMovement(PersonagemOrdem po) {
        if (!isMovimento(po)) {
            return false;
        }
        final String first = po.getOrdem().getParametroIde(0);
        return first.contains("Direcao") && !first.contains("Direcao_Ex");
    }

    public boolean isResourceTransport(PersonagemOrdem po) {
        try {
            return po.getOrdem().hasHabilidade(";ART;");
        } catch (NullPointerException e) {
            return false;
        }
    }

    public boolean isMovimentoDirection(PersonagemOrdem po) {
        try {
            return po.getOrdem().isTipoMovimentacao() && po.getOrdem().getParametroIde(0).contains("Direcao");
        } catch (NullPointerException e) {
            return false;
        }
    }

    public Local getLocalDestination(Personagem pers, PersonagemOrdem po, SortedMap<String, Local> locais) {
        for (int ii = 0; ii < po.getOrdem().getParametrosIdeQtd(); ii++) {
            if (!po.getOrdem().getParametroIde(ii).contains("Coordenada")) {
                continue;
            }
            final String coord = po.getParametrosId().get(ii);
            return locais.get(coord);
        }
        return null;
    }

    /**
     * Assumes that last city parameter is the destination
     *
     * @param pers
     * @param po
     * @param locais
     * @return
     */
    public SortedMap<Integer, Local> getCityDestination(Personagem pers, PersonagemOrdem po, SortedMap<String, Local> locais) {
        final SortedMap<Integer, Local> cities = new TreeMap<>();
        int sequence = 1;
        for (int ii = 0; ii < po.getOrdem().getParametrosIdeQtd(); ii++) {
            if (!po.getOrdem().getParametroIde(ii).contains("Cidade") && !po.getOrdem().getParametroIde(ii).contains("Coordenada")) {
                continue;
            }
            final String coord = po.getParametrosId().get(ii);
            cities.put(sequence++, locais.get(coord));
        }
        return cities;
    }

    public SortedMap<Integer, Local> getLocalDestinationPath(Personagem pers, PersonagemOrdem po, SortedMap<String, Local> locais) {
        //FIXME: Regrets not having hex list as opposed to directions list. Fixme someday?
        Local currentBase = pers.getLocal();
        final SortedMap<Integer, Local> pathArmyMov = new TreeMap<>();
        int idx = 0;
        final List<String> directions = new ArrayList<>();
        directions.addAll(ConverterFactory.armyPathToList(po.getParametrosId().get(0)));
        for (String direction : directions) {
            //TODO: If there's army movement, then needs to calculate final hex. 
            final String coord = localFacade.getIdentificacaoVizinho(currentBase, direction);
            currentBase = locais.get(coord);
            pathArmyMov.put(idx++, currentBase);
        }
        return pathArmyMov;
    }
}
