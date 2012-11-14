/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.services;

import baseLib.BaseModel;
import java.util.Comparator;

/**
 *
 * @author jmoura
 */
public class ComparatorBaseModelSorter implements Comparator {

    private boolean reversed = false;

    public ComparatorBaseModelSorter(Boolean aReversed) {
        this.reversed = aReversed;
    }

    @Override
    public int compare(Object a, Object b) {
        if (reversed) {
            return compareToByIdReversed((BaseModel) a, (BaseModel) b);
        } else {
            return compareToById((BaseModel) a, (BaseModel) b);
        }
    }

    public int compareToById(BaseModel este, BaseModel outro) {
        return (este.getId() - outro.getId());
    }

    public int compareToByIdReversed(BaseModel este, BaseModel outro) {
        return (outro.getId() - este.getId());
    }
}
