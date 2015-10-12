/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import msgs.BaseMsgs;

/**
 *
 * @author gurgel
 */
public class Cenario extends BaseModel {

    //PENDING: mover atributos de (para o) cenario que estao em World.
    private Produto money;
    private List<Feitico> feiticos = new ArrayList();
    private SortedMap<String, Produto> produtos = new TreeMap();
    private SortedMap<String, Terreno> terrenos = new TreeMap();
    private SortedMap<String, Ordem> ordens = new TreeMap(); //ordem.getCodigo(), ordem
    private final SortedMap<String, TipoTropa> tipoTropa = new TreeMap(); //cd, tipoTropa
    private final String[][] tituloPericia = new String[4][12];
    private int numMaxArtefatos;
    private int numMaxPersonagem;
    private int numMaxMovimento;
    private int numMaxOrdens;
    private int numOrdens;

    public Cenario() {
        // PENDING: mover titulos para properties ou banco de dados
        this.tituloPericia[0] = BaseMsgs.tituloPericiaComandante;
        this.tituloPericia[1] = BaseMsgs.tituloPericiaAgente;
        this.tituloPericia[2] = BaseMsgs.tituloPericiaEmissario;
        this.tituloPericia[3] = BaseMsgs.tituloPericiaMago;
    }

    public SortedMap<String, Ordem> getOrdens() {
        return this.ordens;
    }

    public void setOrdens(SortedMap<String, Ordem> list) {
        this.ordens = list;
    }

    public List<Feitico> getFeiticos() {
        return feiticos;
    }

    public void setFeiticos(List feiticos) {
        this.feiticos = feiticos;
    }

    public Produto getMoney() {
        return money;
    }

    public void setMoney(Produto money) {
        this.money = money;
    }

    /**
     * 0 = comandante 1 = agente 2 = emissario 3 = mago
     *
     * @param classe
     * @param indice (0-10)
     * @return tituloPericia
     */
    public String getTituloPericia(int classe, int indice) {
        if (this.tituloPericia[0].length < 12) {
            //xxx: temporary for migration
            return this.tituloPericia[classe][indice - 1];
        }
        return this.tituloPericia[classe][indice];
    }

    public String[][] getTituloPericiaAll() {
        return this.tituloPericia;
    }

    public int getMoneyIndex() {
        // PENDING: criar um flag de controle independente da PK (id).
        int moneyIndex = 7;
        if (getCodigo().equals("GRECIA01")) {
            moneyIndex = 7;
        } else if (getCodigo().equals("1650ER01")) {
            moneyIndex = 7;
        } else if (getCodigo().equals("1650GB01")) {
            moneyIndex = 7;
        } else {
            moneyIndex = 7;
        }
        return moneyIndex;
    }

    /*
     * todos os produtos, incluindo ouro
     */
    public SortedMap<String, Produto> getProdutos() {
        return produtos;
    }

    public void setProdutos(SortedMap<String, Produto> produtos) {
        this.produtos = produtos;
        for (Produto produto : this.produtos.values()) {
            if (produto.isMoney()) {
                this.setMoney(produto);
            }
        }
    }

    public String[][] getTaticas() {
        if (this.hasHabilidade(";ST1;")) {
            return BaseMsgs.taticasGb;
        } else if (this.hasHabilidade(";ST2;")) {
            return BaseMsgs.taticasTk;
        } else {
            return BaseMsgs.taticasGb;
        }
    }

    public void setTerrenos(SortedMap<String, Terreno> list) {
        this.terrenos = list;
    }

    public SortedMap<String, Terreno> getTerrenos() {
        return this.terrenos;
    }

    public boolean isGrecia() {
        return (this.getCodigo().contains("GRECIA"));
        //return (this.getCodigo().equalsIgnoreCase("GRECIA01") || this.getCodigo().equalsIgnoreCase("GRECIA02"));
    }

    public boolean isSW() {
        return (this.getCodigo().equalsIgnoreCase("SW001"));
    }

    public boolean isArzhog() {
        return (this.getCodigo().contains("ARZOG"));
    }

    public boolean isGot() {
        return (this.getCodigo().contains("GOT"));
    }

    public boolean isWdo() {
        return (this.getCodigo().contains("WDO"));
    }

    public SortedMap<String, TipoTropa> getTipoTropas() {
        return tipoTropa;
    }

    public void addTipoTropa(TipoTropa tpTropa) {
        this.tipoTropa.put(tpTropa.getCodigo(), tpTropa);
    }

    public int getNumMaxArtefatos() {
        return numMaxArtefatos;
    }

    public void setNumMaxArtefatos(int numMaxArtefatos) {
        this.numMaxArtefatos = numMaxArtefatos;
    }

    /**
     * @return the numMaxPersonagem
     */
    public int getNumMaxPersonagem() {
        return numMaxPersonagem;
    }

    /**
     * @param numMaxPersonagem the numMaxPersonagem to set
     */
    public void setNumMaxPersonagem(int numMaxPersonagem) {
        this.numMaxPersonagem = numMaxPersonagem;
    }

    /**
     * @return the numMaxMovimento
     */
    public int getNumMaxMovimento() {
        return numMaxMovimento;
    }

    /**
     * @param numMaxMovimento the numMaxMovimento to set
     */
    public void setNumMaxMovimento(int numMaxMovimento) {
        this.numMaxMovimento = numMaxMovimento;
    }

    /**
     * @return the numOrdens
     */
    public int getNumOrdensPers() {
        return numOrdens;
    }

    /**
     * @param numOrdens the numOrdens to set
     */
    public void setNumOrdens(int numOrdens) {
        this.numOrdens = numOrdens;
    }

    /**
     * @return the numMaxOrdens
     */
    public int getNumMaxOrdens() {
        return numMaxOrdens;
    }

    /**
     * @param numMaxOrdens the numMaxOrdens to set
     */
    public void setNumMaxOrdens(int numMaxOrdens) {
        this.numMaxOrdens = numMaxOrdens;
    }

}
