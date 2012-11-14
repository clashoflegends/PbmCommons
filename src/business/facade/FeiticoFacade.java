/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import java.io.Serializable;
import model.Feitico;
import msgs.Msgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author gurgel
 */
public class FeiticoFacade implements Serializable {

    private static final Log log = LogFactory.getLog(FeiticoFacade.class);
    private static final BundleManager label = SettingsManager.getInstance().getBundleManager();

    public String getDificuldadeDisplay(Feitico feitico) {
        try {
            return Msgs.dificuldade[feitico.getDificuldade()];
        } catch (NullPointerException ex) {
            return "-";
        }
    }

    public String getNome(Feitico feitico) {
        return feitico.getNome();
    }

    public String getLivroFeitico(Feitico feitico) {
        return feitico.getLivroFeitico();
    }

    public String getOrdemNome(Feitico feitico) {
        return feitico.getOrdem().getDescricao();
    }

    public String getProibidoDisplay(Feitico feitico) {
        if (feitico.isProibido()) {
            return label.getString("PROIBIDO");
        } else {
            return label.getString("LIVRE");
        }
    }

    public boolean hasRequisito(Feitico feiticoAlvo, Feitico[] listFeiticos) {
        boolean ret = false;
        if (feiticoAlvo.isProibido()) {
            //verifica se eh um tomo proibido
            //nunca conta pre requisitos.
            //personagemFacade checa por NSP e artefatos.
            ret = false;
        } else if (feiticoAlvo.getDificuldade() < 2) {
            //verifica se precisa de requisito
            //se auto ou facil nao precisa
            ret = true;
        } else {
            //varre a lista de feiticos para ver se pelo menos um feitico de requisito esta la.
            for (Feitico feiticoPre : listFeiticos) {
                if (feiticoAlvo.getLivroFeitico().equals(feiticoPre.getLivroFeitico())) {
                    //confirma que eh do mesmo livro
                    if (feiticoPre.getDificuldade() >= feiticoAlvo.getDificuldade() - 1) {
                        //feiticoPre eh um nivel menor que feitico alvo
                        ret = true;
                        //tem pelo menos um, nao precisa continuar a lista
                        break;
                    }
                }
            }
        }
        return ret;
    }

    public boolean isProibido(Feitico feitico) {
        return feitico.isProibido();
    }
}
