/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.Serializable;

/**
 *
 * @author jmoura
 */
public class ExtratoDetail implements Serializable {

    private int sequencial;
    private int valor;
    private int saldoAtual;
    private String descricao;

    public ExtratoDetail(int id, String dsOperacao, int vlOperacao, int vlSaldo) {
        this.setSequencial(id);
        this.setValor(vlOperacao);
        this.setDescricao(dsOperacao);
        this.setSaldoAtual(vlSaldo);
    }

    /**
     * @return the valor
     */
    public int getValor() {
        return valor;
    }

    /**
     * @param valor the valor to set
     */
    public void setValor(int valor) {
        this.valor = valor;
    }

    /**
     * @return the descricao
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * @param descricao the descricao to set
     */
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    /**
     * @return the id
     */
    public int getSequencial() {
        return sequencial;
    }

    /**
     * @param id the id to set
     */
    public void setSequencial(int id) {
        this.sequencial = id;
    }

    /**
     * @return the saldoAtual
     */
    public int getSaldoAtual() {
        return saldoAtual;
    }

    /**
     * @param saldoAtual the saldoAtual to set
     */
    public void setSaldoAtual(int saldoAtual) {
        this.saldoAtual = saldoAtual;
    }
}
