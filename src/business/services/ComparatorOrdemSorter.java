/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.services;

import java.util.Comparator;
import model.Ordem;

/**
 *
 * @author jmoura
 */
public class ComparatorOrdemSorter implements Comparator {

    public ComparatorOrdemSorter() {
    }

    @Override
    public int compare(Object a, Object b) {
        return ((Ordem) a).compareToByNumber((Ordem) b);
    }
}
