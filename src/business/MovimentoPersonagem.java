/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business;

import baseLib.IBaseModel;
import business.facade.LocalFacade;
import java.io.Serializable;
import model.Local;

/**
 *
 * @author jmoura
 */
public class MovimentoPersonagem implements Serializable, IBaseModel {

    private Local origem;
    private Local destino;
    private boolean porAgua;
    private int limiteMovimento;

    public MovimentoPersonagem(Local origem, Local destino, int range, boolean water) {
        this.setOrigem(origem);
        this.setDestino(destino);
        this.setLimiteMovimento(range);
        this.setPorAgua(water);
    }

    @Override
    public String toString() {
        try {
            return this.getOrigem().getCoordenadas() + " -> " + this.getDestino() + " range: " + getLimiteMovimento();
        } catch (NullPointerException e) {
            return "N/A -> N/A";
        }
    }

    public int getId() {
        return this.getDestino().getId();
    }

    public String getCodigo() {
        return this.getDestino().getCodigo();
    }

    public String getComboId() {
        return this.getDestino().getComboId();
    }

    public String getComboDisplay() {
        return this.getDestino().getComboDisplay();
    }

    public String getNome() {
        return this.getDestino().getNome();
    }

    /**
     * @return the origem
     */
    public Local getOrigem() {
        return origem;
    }

    /**
     * @param origem the origem to set
     */
    public void setOrigem(Local origem) {
        this.origem = origem;
    }

    /**
     * @return the destino
     */
    public Local getDestino() {
        return destino;
    }

    /**
     * @param destino the destino to set
     */
    public void setDestino(Local destino) {
        this.destino = destino;
    }

    /**
     * @return the porAgua
     */
    public boolean isPorAgua() {
        return porAgua;
    }

    /**
     * @param porAgua the porAgua to set
     */
    public void setPorAgua(boolean porAgua) {
        this.porAgua = porAgua;
    }

    /**
     * @return the limiteMovimento
     */
    public int getLimiteMovimento() {
        return limiteMovimento;
    }

    /**
     * @param limiteMovimento the limiteMovimento to set
     */
    public void setLimiteMovimento(int limiteMovimento) {
        this.limiteMovimento = limiteMovimento;
    }

    public int getDistancia() {
        LocalFacade localFacade = new LocalFacade();
        return localFacade.getDistancia(getOrigem(), getDestino());
    }
}
