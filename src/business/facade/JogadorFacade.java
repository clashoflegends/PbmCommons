/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import java.io.Serializable;
import model.Jogador;
import model.Personagem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author gurgel
 */
public class JogadorFacade implements Serializable {

    private static final Log log = LogFactory.getLog(JogadorFacade.class);
    private static final BundleManager label = SettingsManager.getInstance().getBundleManager();

    public boolean isMine(Personagem personagem, Jogador jogador) {
        try {
            return (personagem.getNacao().getOwner() == jogador);
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean isReportCompact(Jogador jogador) {
        return jogador.hasHabilidade(";JRC;");
    }
}
