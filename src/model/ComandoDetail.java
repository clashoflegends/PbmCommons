/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author gurgel
 */
public class ComandoDetail implements Serializable {

    //nacao,personagem, ordem, parametros
    private String nacaoCodigo, personagemCodigo, ordemCodigo, ordemNome;
    private List<String> parametroId;
    private List<String> parametroDisplay;

    public ComandoDetail(Personagem personagem, Ordem ordem, List<String> parametrosId, List<String> parametrosDisplay) {
        this.nacaoCodigo = personagem.getNacao().getId() + "";
        this.personagemCodigo = personagem.getCodigo();
        try {
            this.ordemCodigo = ordem.getCodigo() + "";
            this.ordemNome = ordem.getNome();
        } catch (NullPointerException ex) {
            //ordem vazia. just skipping
        }
        this.parametroId = parametrosId;
        this.parametroDisplay = parametrosDisplay;
    }

//    public ComandoDetail(int nacaoId, String persId, String nuOrdem, String nmOrdem, List<String> parametrosId, List<String> parametrosDisplay) {
//        this.nacaoCodigo = nacaoId + "";
//        this.personagemCodigo = persId;
//        try {
//            this.ordemCodigo = nuOrdem;
//            this.ordemNome = nmOrdem;
//        } catch (NullPointerException ex) {
//            //ordem vazia. just skipping
//        }
//        this.parametroId = parametrosId;
//        this.parametroDisplay = parametrosDisplay;
//    }

    /**
     * @return the nacaoCodigo
     */
    public String getNacaoCodigo() {
        return nacaoCodigo;
    }

    /**
     * @return the personagemCodigo
     */
    public String getPersonagemCodigo() {
        return personagemCodigo;
    }

    /**
     * @return the ordemCodigo
     */
    public String getOrdemCodigo() {
        return ordemCodigo;
    }

    /**
     * @return the ordemNome
     */
    public String getOrdemNome() {
        return ordemNome;
    }

    /**
     * @return the parametroId
     */
    public List<String> getParametroId() {
        return parametroId;
    }

    /**
     * @return the parametroDisplay
     */
    public List<String> getParametroDisplay() {
        return parametroDisplay;
    }

    @Override
    public String toString() {
        return this.personagemCodigo + "-" + this.getOrdemNome() + "-" + getClass().getName() + "@" + Integer.toHexString(hashCode());
    }
}
