/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import baseLib.SysApoio;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import model.Artefato;
import model.Jogador;
import model.Nacao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author gurgel
 */
public class ArtefatoFacade implements Serializable {

    private static final Log log = LogFactory.getLog(ArtefatoFacade.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();

    public String getAlinhamento(Artefato artefato) {
        if (artefato.getAlinhamento() != null) {
            return labels.getString(artefato.getAlinhamento().toUpperCase());
        } else {
            return "?";
        }
    }

    public String getLocalCoordenadas(Artefato artefato) {
        if (artefato.getOwner() != null) {
            return personagemFacade.getCoordenadas(artefato.getOwner());
        } else if (artefato.getSabeAonde() != null) {
            return artefato.getLocal().getCoordenadas();
        } else {
            //FIXME: Artefato pode ter localizacao conhecida (por magia)
            return "-";
        }
    }

    public String getDescricao(Artefato artefato) {
        return artefato.getDescricao();
    }

    public String getNome(Artefato artefato) {
        return artefato.getNome();
    }

    public String getOwnerNacaoNome(Artefato artefato) {
        if (artefato.getOwner() != null) {
            return personagemFacade.getNacaoNome(artefato.getOwner());
        } else if (artefato.getSabeAonde() != null) {
            return personagemFacade.getNacaoNome(artefato.getSabeAonde());
        } else {
            //FIXME: Artefato pode ter localizacao conhecida (por magia)
            return "-";
        }
    }

    public String getOwnerNome(Artefato artefato) {
        if (artefato.getOwner() != null) {
            return personagemFacade.getNome(artefato.getOwner());
        } else if (artefato.getSabeAonde() != null) {
            return labels.getString("ESCONDIDO.POR") + personagemFacade.getNome(artefato.getSabeAonde());
        } else {
            //FIXME: Artefato pode estar em CP ou ter localizacao conhecida (por magia)
            return "-";
        }
    }

    public Jogador getJogador(Artefato artefato) {
        Jogador jogador = null;
        try {
            jogador = artefato.getOwner().getNacao().getOwner();
        } catch (NullPointerException ex) {
            //se owner is null, then verify if hidden
            try {
                jogador = artefato.getSabeAonde().getNacao().getOwner();
            } catch (NullPointerException en) {
                //faz nada, so retorna null
                //Lost arctifact
            }
        }
        return jogador;
    }

    public Nacao getNacao(Artefato artefato) {
        Nacao nacao = null;
        if (artefato.getOwner() != null) {
            nacao = artefato.getOwner().getNacao();
        } else if (artefato.getSabeAonde() != null) {
            nacao = artefato.getSabeAonde().getNacao();
        }
        return nacao;
    }

    public String getPrimario(Artefato artefato) {
        return artefato.getPrimario();
    }

    public int getValor(Artefato artefato) {
        return artefato.getValor();
    }

    public boolean isLatente(Artefato artefato) {
        return artefato.isLatente();
    }

    public boolean isPosse(Artefato artefato) {
        return artefato.isPosse();
    }

    public Integer getNumero(Artefato artefato) {
        int ret = 0;
        try {
            ret = SysApoio.parseInt(artefato.getComboId());
        } catch (NullPointerException ex) {
        }
        return ret;
    }

    public boolean isJogador(Artefato artefato, Jogador jativo) {
        return (jativo == this.getJogador(artefato));
    }

    /**
     * lista todos os artefatos da nacao, escondidos ou em posse de alguem.
     */
    public Collection<Artefato> getArtefatos(Collection<Artefato> listArtefatos, Nacao nacao) {
        List<Artefato> lista = new ArrayList();
        for (Artefato artefato : listArtefatos) {
            if (this.getNacao(artefato) == nacao) {
                lista.add(artefato);
            }
        }
        return lista;
    }

    public String getLatente(Artefato artefato) {
        return SysApoio.iif(isLatente(artefato), labels.getString("SIM"), labels.getString("NAO"));
    }

    public String getPoder(Artefato artefato) {
        return artefato.getPrimario() + " +" + artefato.getValor();
    }
}
