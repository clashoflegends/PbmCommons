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
 * @author gurgel
 */
public class Raca extends BaseModel {

    private String display;
    private int numero;
    private SortedMap<String, String> tropasDescricao = new TreeMap(); //codigo, descricao
    private SortedMap<TipoTropa, Integer> tropas = new TreeMap();    //0=tropas da raca, 1=tropas especiais da raca, 2=tropas default

    public void addTropa(TipoTropa tpTropa, String descricao, boolean especial) {
        if (especial) {
            this.tropas.put(tpTropa, 1);
        } else {
            this.tropas.put(tpTropa, 0);
        }
        this.tropasDescricao.put(tpTropa.getCodigo(), descricao);
    }

    public SortedMap<TipoTropa, Integer> getTropas() {
        return this.tropas;
    }

    public String getTropaDescricao(String tipo) {
        //PENDING: quando for customizar nomes das torpas por raca, alterar aki.
        return this.tropasDescricao.get(tipo);
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }
}
