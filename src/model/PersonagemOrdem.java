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

    private BaseModel personagem; //mantem por compatibilidade com turnos velhos que jogadores podem ter salvo.
    private Ordem ordem;
    private int index;
    private List<String> parametrosId = new ArrayList();
    private List<String> parametrosDisplay = new ArrayList();

    public Ordem getOrdem() {
        return ordem;
    }

    public void setOrdem(Ordem ordem) {
        this.ordem = ordem;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public List<String> getParametrosId() {
        return parametrosId;
    }

    public void setParametrosId(List<String> parametrosId) {
        this.parametrosId = parametrosId;
    }

    public List<String> getParametrosDisplay() {
        return parametrosDisplay;
    }

    public void setParametrosDisplay(List<String> parametrosDisplay) {
        this.parametrosDisplay = parametrosDisplay;
    }

    @Override
    public String getNome() {
        if (super.getNome() == null) {
            //compatibilidade com turnos velhos.
            setNome(personagem.getNome());
        }
        return super.getNome();
    }

    public int compareToByNumber(Object o) {
        PersonagemOrdem outro = (PersonagemOrdem) o;
        try {
            return (this.getOrdem().getNumero() - outro.getOrdem().getNumero());
        } catch (NullPointerException ex) {
            //zzz: 497/498
            if (this.getOrdem() == null) {
                return (497 - outro.getOrdem().getNumero());
            } else {
                return (this.getOrdem().getNumero() - 497);
            }
        }
    }
}
