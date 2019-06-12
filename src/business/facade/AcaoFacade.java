/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import baseLib.BaseModel;
import business.converter.ConverterFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import model.Cenario;
import model.Habilidade;
import model.Local;
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
        if (actor.isNacao()) {
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

    public int getCusto(PersonagemOrdem order, Nacao nation, Cenario cenario) {
        try {
            if (order.getOrdem().getCodigo().equals("411")) {
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
            } else if (order.getOrdem().getCodigo().equals("550")) {
                return (nacaoFacade.getCidadeTamanho(nation, order.getNome()) + 1) * 2000;
            } else {
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

    public boolean isMovimento(PersonagemOrdem po) {
        try {
            return po.getOrdem().isTipoMovimentacao();
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
        return pers.getLocal();
    }

    public List<Local> getLocalDestinationPath(Personagem pers, PersonagemOrdem po, SortedMap<String, Local> locais) {
        //FIXME: Regrets not having hex list as opposed to directions list. Fixme someday?
        Local currentBase = pers.getLocal();
        final List<Local> pathArmyMov = new ArrayList<Local>();
        final List<String> directions = new ArrayList<String>();
        directions.addAll(ConverterFactory.armyPathToList(po.getParametrosId().get(0)));
        for (String direction : directions) {
            //TODO: If there's army movement, then needs to calculate final hex. 
            final String coord = localFacade.getIdentificacaoVizinho(currentBase, direction);
            currentBase = locais.get(coord);
            pathArmyMov.add(currentBase);
        }
        return pathArmyMov;
    }
}
