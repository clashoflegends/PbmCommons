/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import java.io.Serializable;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author gurgel
 */
public class Mercado extends BaseModel implements Serializable {

    private final SortedMap<Produto, Integer[]> mercado = new TreeMap(); //[disponivel, compra, venda]

    public void addProduto(Produto produto, int disponivel, int compra, int venda) {
        Integer[] temp = {disponivel, compra, venda};
        this.mercado.put(produto, temp);
    }

    public int getProdutoQtDisponivel(Produto produto) {
        try {
            return this.mercado.get(produto)[0];
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public int getProdutoVlCompra(Produto produto) {
        try {
            return this.mercado.get(produto)[1];
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public int getProdutoVlVenda(Produto produto) {
        try {
            return this.mercado.get(produto)[2];
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    /*
     * todos os produtos, exceto ouro
     */
    public Collection<Produto> getProdutos() {
        return this.mercado.keySet();
    }
}
