/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author gurgel
 */
public class PersonagemOrdem extends BaseModel {

    private Personagem personagem;
    private Ordem ordem;
    private int index;
    private List<String> parametrosId = new ArrayList();
    private List<String> parametrosDisplay = new ArrayList();

    /**
     * @return the personagem
     */
    public Personagem getPersonagem() {
        return personagem;
    }

    /**
     * @param personagem the personagem to set
     */
    public void setPersonagem(Personagem personagem) {
        this.personagem = personagem;
    }

    /**
     * @return the ordem
     */
    public Ordem getOrdem() {
        return ordem;
    }

    /**
     * @param ordem the ordem to set
     */
    public void setOrdem(Ordem ordem) {
        this.ordem = ordem;
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * @return the parametrosId
     */
    public List<String> getParametrosId() {
        return parametrosId;
    }

    /**
     * @param parametrosId the parametrosId to set
     */
    public void setParametrosId(List<String> parametrosId) {
        this.parametrosId = parametrosId;
    }

    /**
     * @return the parametrosDisplay
     */
    public List<String> getParametrosDisplay() {
        return parametrosDisplay;
    }

    /**
     * @param parametrosDisplay the parametrosDisplay to set
     */
    public void setParametrosDisplay(List<String> parametrosDisplay) {
        this.parametrosDisplay = parametrosDisplay;
    }

    @Override
    public String getNome() {
        try {
            return String.format("%s - %s", this.personagem.getNome(), this.ordem.getNome());
        } catch (NullPointerException e) {
            try {
                return String.format("%s - N/A", this.personagem.getNome());
            } catch (NullPointerException ex) {
                return "N/A - N/A";
            }
        }
    }

    public int compareToByNumber(Object o) {
        PersonagemOrdem outro = (PersonagemOrdem) o;
        return (this.getOrdem().getNumero() - outro.getOrdem().getNumero());
    }
}
