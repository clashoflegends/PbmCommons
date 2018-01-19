/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author gurgel Remember to keep aligned with JogadorPontos
 */
public class Jogador extends BaseModel {

    private String login, senha, email;
    private final SortedMap<String, Nacao> nacoesOwned = new TreeMap<String, Nacao>();
    private boolean reportAll; //don't remove for backwards compatibility

    public void addNacao(Nacao nacao) {
        this.nacoesOwned.put(nacao.getCodigo(), nacao);
    }

    public SortedMap<String, Nacao> getNacoes() {
        return this.nacoesOwned;
    }

    /**
     * Retorna se a nacao pertence ao jogador
     *
     * @param nacao
     * @return
     */
    public boolean isNacao(Nacao nacao) {
        return this.nacoesOwned.containsValue(nacao);
    }

    /**
     * Retorna se a nacao pertence ao jogador
     *
     * @param idNacao
     * @return
     */
    public boolean isNacao(int idNacao) {
        boolean ret = false;
        for (Nacao nacao : this.nacoesOwned.values()) {
            if (nacao.getId() == idNacao) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    /*
     * Nacao alvo considera jogador como aliado
     */
    public boolean isJogadorAliado(Nacao nacaoAlvo) {
        //FIXME: Mover para NacaoFacade
        final boolean ret = this.isNacao(nacaoAlvo);
        //se nao eh do mesmo jogador
        if (ret) {
            return true;
        }
        //compara as nacoes do jogador com o alvo
        for (Nacao nacaoJogador : this.getNacoes().values()) {
            try {
                if (nacaoAlvo.getRelacionamento(nacaoJogador) > 1) {
                    //se relacionamento da nacao com o jogador eh amigavel ou >
                    //so precisa de um para confirmar
                    return true;
                }
            } catch (NullPointerException e) {
                //just ignore
            }
        }
        return false;
    }

    public String getEmail() {
        return email;
    }

    public String getLogin() {
        return login;
    }

    public String getSenha() {
        return senha;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    /**
     * @return the reportAll
     */
    public boolean isReportAll() {
        return hasHabilidade(";JRA;");
    }

    public boolean isNpc() {
        return hasHabilidade(";JAI;");
    }
}
