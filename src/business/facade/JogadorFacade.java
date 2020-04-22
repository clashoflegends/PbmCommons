/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import java.io.Serializable;
import model.Cidade;
import model.Jogador;
import model.Nacao;
import model.Personagem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class JogadorFacade implements Serializable {

    private static final Log log = LogFactory.getLog(JogadorFacade.class);
    private static final BundleManager label = SettingsManager.getInstance().getBundleManager();

    public boolean isMine(Personagem personagem, Jogador jogador) {
        try {
            return (jogador.isNacao(personagem.getNacao()));
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean isMine(Cidade cidade, Jogador jogador) {
        try {
            return (jogador.isNacao(cidade.getNacao()));
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean isMine(Nacao nacao, Jogador jogador) {
        try {
            return (jogador.isNacao(nacao));
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean isAlly(Personagem personagem, Jogador jogador) {
        try {
            return (jogador.isJogadorAliado(personagem.getNacao()));
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean isAlly(Nacao nation, Jogador player) {
        try {
            return (player.isJogadorAliado(nation));
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean isReportCompact(Jogador jogador) {
        return jogador.hasHabilidade(";JRC;");
    }
}
