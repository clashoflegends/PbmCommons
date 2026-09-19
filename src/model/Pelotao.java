/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;

/**
 *
 * @author gurgel
 */
public class Pelotao extends BaseModel implements Cloneable {
//Tropas 	Treino 	Arma 	Armadura 	Qtd 	Tipo de Tropa
//Hoplitas 	100 	60 	60 	600 	Infantaria Pesada

    private int treino = 0, modAtaque = 0, modDefesa = 0, qtd = 0;
    private TipoTropa tipoTropa;
//    private Raca raca;

    public int getTreino() {
        return treino;
    }

    public void setTreino(int treino) {
        if (treino < 0) {
            this.treino = 0;
        } else if (treino > 100) {
            this.treino = 100;
        } else {
            this.treino = treino;
        }
    }

    public int getModAtaque() {
        return modAtaque;
    }

    public void setModAtaque(int arma) {
        if (arma < 0) {
            this.modAtaque = 0;
        } else if (arma > 100) {
            this.modAtaque = 100;
        } else {
            this.modAtaque = arma;
        }
    }

    public void sumModAtaque(int arma) {
        setModAtaque(arma + this.getModAtaque());
    }

    public int getModDefesa() {
        return modDefesa;
    }

    public void setModDefesa(int armadura) {
        if (armadura < 0) {
            this.modDefesa = 0;
        } else if (armadura > 100) {
            this.modDefesa = 100;
        } else {
            this.modDefesa = armadura;
        }
    }

    public void sumModDefesa(int armadura) {
        setModDefesa(armadura + this.getModDefesa());
    }

    public int getQtd() {
        return qtd;
    }

    public void setQtd(int qtd) {
        this.qtd = qtd;
    }

    public void addQtd(int qtd) {
        this.qtd += qtd;
    }

    public void subQtd(int qtd) {
        this.qtd -= qtd;
    }

    public TipoTropa getTipoTropa() {
        return tipoTropa;
    }

    public void setTipoTropa(TipoTropa tipoTropa) {
        this.tipoTropa = tipoTropa;
    }

    @Override
    public String getNome() {
        return this.getTipoTropa().getNome();
    }

    @Override
    public String getCodigo() {
        return getTipoTropa().getCodigo();
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", this.getTipoTropa().getNome(), this.getQtd());
    }

    /**
     * A SHALLOW copy, and that is the contract rather than an omission.
     *
     * {@code treino}, {@code modAtaque}, {@code modDefesa} and {@code qtd} are primitives and come
     * across by value, so the copy can be re-quantified and re-trained without touching the original.
     * {@code tipoTropa} is deliberately SHARED: it is the scenario's read-only troop catalogue, callers
     * repoint a platoon at a different entry but never edit an entry, and copying it would both waste
     * memory and break identity comparisons against the catalogue.
     *
     * Two callers depend on exactly this: {@code ExercitoControlFacade.getTropasTerraSortedCloned}
     * takes a per-round casualty snapshot in the Judge's hot path, and {@code ArmySim} gives the
     * BattleSim its own platoons so editing one cannot reach the army loaded from the EGF.
     *
     * The {@code BaseModel} collections ({@code habilidades}, {@code acao}, {@code acaoExecutadas})
     * are shared by this copy. That is safe today because a Pelotao never carries its own abilities:
     * every lookup goes through {@code getTipoTropa().hasHabilidade(...)}. Give a Pelotao ability or
     * order state of its own and this method has to copy those maps too.
     */
    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public Pelotao clone() {
        try {
            return (Pelotao) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }
}
