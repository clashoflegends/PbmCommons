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
public class Artefato extends BaseModel {

    private int valor;
    private int valorPoder;
    private boolean comandante = false;
    private boolean agente = false;
    private boolean emissario = false;
    private boolean mago = false;
    private boolean furtividade = false;
    private boolean combate = false;
    private boolean ocultamento = false;
    private boolean exploracao = false;
    private boolean movimentacao = false;
    private boolean outros = false;
    private String primario;
    private String secundario;
    private String tipo;
    private String alinhamento;
    private String descricao;
    private String tomo;
    private String historia;
    private Personagem owner;
    private Personagem sabeAonde;
    private Local local;

    public boolean isLatente() {
        return (isComandante() || isAgente() || isEmissario() || isMago() || isFurtividade());
    }

    public boolean isPosse() {
        return (this.getOwner() != null);
    }

    public String getPrimario() {
        return primario;
    }

    public void setPrimario(String primario) {
        this.primario = primario;
    }

    public String getSecundario() {
        return secundario;
    }

    public void setSecundario(String secundario) {
        this.secundario = secundario;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getValor() {
        return valor;
    }

    public void setValor(int valor) {
        this.valor = valor;
    }

    public int getValorPoder() {
        return valorPoder;
    }

    public void setValorPoder(int valorPoder) {
        this.valorPoder = valorPoder;
    }

    public String getAlinhamento() {
        return alinhamento;
    }

    public void setAlinhamento(String alinhamento) {
        this.alinhamento = alinhamento;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTomo() {
        return tomo;
    }

    public void setTomo(String tomo) {
        this.tomo = tomo;
    }

    public String getHistoria() {
        return historia;
    }

    public void setHistoria(String historia) {
        this.historia = historia;
    }

    public Personagem getOwner() {
        return owner;
    }

    public void setOwner(Personagem owner) {
        if (getOwner() != null) {
            //remove proprietario anterior
            getOwner().remArtefato(this);
        }
        this.owner = owner;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        //remove o artefato do local anterior
        if (this.local != null) {
            this.local.removeArtefato(this);
        }
        this.local = local;
        //adiciona o artefato no local atual
        if (this.local != null) {
            this.local.addArtefato(this);
        }
    }

    public boolean isAgente() {
        return agente;
    }

    public boolean isComandante() {
        return comandante;
    }

    public void setComandante(boolean comandante) {
        this.comandante = comandante;
    }

    public void setAgente(boolean agente) {
        this.agente = agente;
    }

    public boolean isEmissario() {
        return emissario;
    }

    public void setEmissario(boolean emissario) {
        this.emissario = emissario;
    }

    public boolean isMago() {
        return mago;
    }

    public void setMago(boolean mago) {
        this.mago = mago;
    }

    public boolean isFurtividade() {
        return furtividade;
    }

    public void setFurtividade(boolean furtividade) {
        this.furtividade = furtividade;
    }

    public boolean isCombate() {
        return combate;
    }

    public void setCombate(boolean combate) {
        this.combate = combate;
    }

    public boolean isOcultamento() {
        return ocultamento;
    }

    public void setOcultamento(boolean ocultamento) {
        this.ocultamento = ocultamento;
    }

    public boolean isExploracao() {
        return exploracao;
    }

    public void setExploracao(boolean exploracao) {
        this.exploracao = exploracao;
    }

    public boolean isMovimentacao() {
        return movimentacao;
    }

    public void setMovimentacao(boolean movimentacao) {
        this.movimentacao = movimentacao;
    }

    public boolean isOutros() {
        return outros;
    }

    public void setOutros(boolean outros) {
        this.outros = outros;
    }

    /**
     * @return the sabeAonde
     */
    public Personagem getSabeAonde() {
        return sabeAonde;
    }

    /**
     * @param sabeAonde the sabeAonde to set
     */
    public void setSabeAonde(Personagem sabeAonde) {
        this.sabeAonde = sabeAonde;
    }

    @Override
    public int compareTo(Object o) {
        Artefato outro = (Artefato) o;
        return (this.getNome().compareTo(outro.getNome()));
    }

    public boolean isDragonEgg() {
        return tipo.equals("DRAGONEGG");
    }

    public boolean isSummon() {
        return tipo.equals("SUMMON");
    }
}
