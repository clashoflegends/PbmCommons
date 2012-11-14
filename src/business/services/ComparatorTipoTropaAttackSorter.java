/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.services;

import java.util.Comparator;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;

/**
 *
 * @author jmoura
 */
public class ComparatorTipoTropaAttackSorter implements Comparator {

    private Terreno local;

    public ComparatorTipoTropaAttackSorter(Terreno aTerreno) {
        this.local = aTerreno;
    }

    @Override
    public int compare(Object a, Object b) {
        if (a instanceof Pelotao) {
            return compareByAttack(((Pelotao) a).getTipoTropa(), ((Pelotao) b).getTipoTropa());
        } else {
            return compareByAttack((TipoTropa) a, (TipoTropa) b);
        }
    }

    private int compareByAttack(TipoTropa este, TipoTropa outro) {
        return (este.getAtaqueTerreno().get(local) - outro.getAtaqueTerreno().get(local));
    }
}
