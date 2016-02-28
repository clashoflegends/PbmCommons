/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author gurgel
 */
public class Raca extends BaseModel {

    private String display, titletLord = "";
    private int numero;
    private final SortedMap<String, String> tropasDescricao = new TreeMap(); //codigo, descricao
    private final SortedMap<TipoTropa, Integer> tropas = new TreeMap();    //0=tropas da raca, 1=tropas especiais da raca, 2=tropas default

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

    public List<TipoTropa> getTropasElite() {
        List<TipoTropa> list = new ArrayList<TipoTropa>();
        for (TipoTropa key : tropas.keySet()) {
            if (tropas.get(key).equals(1)) {
                list.add(key);
            }
        }
        Collections.shuffle(list);
        return list;
    }

    public List<TipoTropa> getTropasRegular() {
        List<TipoTropa> list = new ArrayList<TipoTropa>();
        for (TipoTropa key : tropas.keySet()) {
            if (tropas.get(key).equals(0)) {
                list.add(key);
            }
        }
        Collections.shuffle(list);
        return list;
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

    /**
     * @return the rulerTitle
     */
    public String getTitleLord() {
        return titletLord;
    }

    /**
     * @param rulerTitle the rulerTitle to set
     */
    public void setTitleLord(String rulerTitle) {
        this.titletLord = rulerTitle;
    }
}
