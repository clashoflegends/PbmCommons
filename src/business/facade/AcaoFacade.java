/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import baseLib.SysApoio;
import java.io.Serializable;
import java.util.Collection;
import model.Habilidade;
import model.Ordem;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

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
}
