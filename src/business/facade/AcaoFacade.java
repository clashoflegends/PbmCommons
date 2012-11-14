/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import baseLib.SysApoio;
import java.io.Serializable;
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
    //TODO: mover tipo de personagem para o Cenario
    private static final String[] tipoPersonagem = {labels.getString("QUALQUER"), labels.getString("COMANDANTE"),
        labels.getString("AGENTE"), labels.getString("EMISSARIO"), labels.getString("MAGO"),
        labels.getString("MILESTONE"), labels.getString("CIDADE")};

    public String getDificuldade(Ordem ordem) {
        if (ordem.getDificuldade().equals("Aut")) {
            return labels.getString("AUTOMATICA");
        } else if (ordem.getDificuldade().equals("Dif")) {
            return labels.getString("DIFICIL");
        } else if (ordem.getDificuldade().equals("Fac")) {
            return labels.getString("FACIL");
        } else if (ordem.getDificuldade().equals("Med")) {
            return labels.getString("MEDIA");
        } else if (ordem.getDificuldade().equals("Var")) {
            return labels.getString("VARIADA");
        } else {
            return "-";
        }
    }

    public String getTipoOrdem(Ordem ordem) {
        if (ordem.getTipo().equals("Misc")) {
            return labels.getString("LIVRE");
        } else if (ordem.getTipo().equals("Per")) {
            return labels.getString("PRINCIPAL");
        } else if (ordem.getTipo().equals("Mov")) {
            return labels.getString("MOVIMENTACAO");
        } else if (ordem.getTipo().equals("Cid")) {
            return labels.getString("CIDADE");
        } else if (ordem.getTipo().equals("Milestone")) {
            return labels.getString("MILESTONE");
        } else {
            return "-";
        }
    }

    public String getTipoPersonagem(Ordem ordem) {
        if (ordem.getTipoPersonagem().equals("X")) {
            return tipoPersonagem[0];
        } else if (ordem.getTipoPersonagem().equals("C")) {
            return tipoPersonagem[1];
        } else if (ordem.getTipoPersonagem().equals("A")) {
            return tipoPersonagem[2];
        } else if (ordem.getTipoPersonagem().equals("E")) {
            return tipoPersonagem[3];
        } else if (ordem.getTipoPersonagem().equals("M")) {
            return tipoPersonagem[4];
        } else if (ordem.getTipoPersonagem().equals("Z")) {
            return tipoPersonagem[5];
        } else if (ordem.getTipoPersonagem().equals("F")) {
            return tipoPersonagem[6];
        } else {
            return "-";
        }
    }

    public String[][] listTipoPersonagem() {
        String[][] ret = new String[tipoPersonagem.length + 1][2];
        int ii = 0;
        ret[ii][0] = labels.getString("TODOS"); //Display
        ret[ii++][1] = "Todos"; //Id
        for (String elem : tipoPersonagem) {
            ret[ii][0] = elem;
            ret[ii++][1] = elem;
        }
        return ret;
    }

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
}
