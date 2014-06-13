/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.services;

import java.util.Comparator;
import model.Pelotao;
import model.TipoTropa;

/**
 *
 * @author jmoura
 */
public class ComparatorTipoTropaUpkeepSorter implements Comparator {

    public ComparatorTipoTropaUpkeepSorter() {
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
        return ((este.getRecruitCostMoney() + este.getUpkeepMoney() * 1000) - (outro.getRecruitCostMoney() + outro.getUpkeepMoney() * 1000));
    }
}
