/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author gurgel
 */
public class Extrato implements Serializable {

    private int qtDetail = 0;
    private final List<ExtratoDetail> detalhe = new ArrayList();

    public void addDetail(String dsOperacao, int vlOperacao, int vlSaldo) {
        this.detalhe.add(new ExtratoDetail(qtDetail++, dsOperacao, vlOperacao, vlSaldo));
    }

    public Iterator<ExtratoDetail> getExtratoDetalhes() {
        return this.detalhe.iterator();
    }

    public List<ExtratoDetail> getExtratoList() {
        return this.detalhe;
    }

    public int getSize() {
        return this.detalhe.size();
    }
}
