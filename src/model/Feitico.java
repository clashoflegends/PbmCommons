/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import msgs.Msgs;

/**
 *
 * @author gurgel
 */
public class Feitico extends BaseModel {

    private int numero;
    private int valor;
    private int dificuldade;
    private boolean proibido = false;
    private String livroFeitico, ajuda;
    private Ordem ordem;
    private String parametroChave, parametroLabel;

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public Ordem getOrdem() {
        return ordem;
    }

    public void setOrdem(Ordem ordem) {
        this.ordem = ordem;
    }

    public int getValor() {
        return valor;
    }

    public void setValor(int valor) {
        this.valor = valor;
    }

    public int getDificuldade() {
        return dificuldade;
    }

    public void setDificuldade(int dificuldade) {
        this.dificuldade = dificuldade;
    }

    public String getLivroFeitico() {
        return livroFeitico;
    }

    public void setLivroFeitico(String livroFeitico) {
        this.livroFeitico = livroFeitico;
    }

    public boolean isProibido() {
        return proibido;
    }

    public void setProibido(boolean proibido) {
        this.proibido = proibido;
    }

    @Override
    public String getComboDisplay() {
        return String.format("%s [%s] (%s)", this.getNome(), this.getLivroFeitico(), Msgs.dificuldade[this.getDificuldade()]);
    }

    @Override
    public String getComboId() {
        return String.format("%d", this.getNumero());
    }

    public String getAjuda() {
        return ajuda;
    }

    public void setAjuda(String ajuda) {
        this.ajuda = ajuda;
    }

    /**
     * @return the parametroChave
     */
    public String getParametroChave() {
        return parametroChave;
    }

    /**
     * @param parametroChave the parametroChave to set
     */
    public void setParametroChave(String parametroChave) {
        this.parametroChave = parametroChave;
    }

    /**
     * @return the parametroLabel
     */
    public String getParametroLabel() {
        return parametroLabel;
    }

    /**
     * @param parametroLabel the parametroLabel to set
     */
    public void setParametroLabel(String parametroLabel) {
        this.parametroLabel = parametroLabel;
    }
}
