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
public class ComparatorBaseDisplayModelSorter implements Comparator {

    private boolean reversed = false;

    public ComparatorBaseDisplayModelSorter(Boolean aReversed) {
        this.reversed = aReversed;
    }

    @Override
    public int compare(Object a, Object b) {
        return compareToByDisplay((BaseModel) a, (BaseModel) b);
    }

    public int compareToByDisplay(BaseModel a, BaseModel b) {
        if (reversed) {
            return b.getComboDisplay().compareToIgnoreCase(a.getComboDisplay());
        } else {
            return a.getComboDisplay().compareToIgnoreCase(b.getComboDisplay());
        }
    }
}
