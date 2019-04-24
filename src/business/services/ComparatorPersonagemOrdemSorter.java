/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.services;

import java.util.Comparator;
import model.PersonagemOrdem;

/**
 *
 * @author jmoura
 */
public class ComparatorPersonagemOrdemSorter implements Comparator {

    public ComparatorPersonagemOrdemSorter() {
    }

    @Override
    public int compare(Object a, Object b) {
        try {
            return ((PersonagemOrdem) a).compareToByNumber((PersonagemOrdem) b);
        } catch (Exception e) {
            return ((PersonagemOrdem) a).compareToByNumber((PersonagemOrdem) b);
        }
    }
}
