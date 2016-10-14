/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import baseLib.BaseModel;
import persistenceCommons.SysApoio;
import java.io.Serializable;
import java.util.Collection;
import model.Habilidade;
import model.Nacao;
import model.Ordem;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class AcaoFacade implements Serializable {

    private static final Log log = LogFactory.getLog(AcaoFacade.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

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

    public int getCusto(PersonagemOrdem ordem) {
        try {
            return ordem.getOrdem().getCusto();
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
}
