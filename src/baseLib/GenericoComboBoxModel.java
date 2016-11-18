/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package baseLib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class GenericoComboBoxModel extends DefaultComboBoxModel implements ComboBoxModel, Serializable {

    private static final Log log = LogFactory.getLog(GenericoComboBoxModel.class);

    public GenericoComboBoxModel(final String[][] items) {
        super();
        try {
            int i, c;
            for (i = 0, c = items.length; i < c; i++) {
                //arruma o array que esta no formato String[][] para criar o objeto IBaseModel...
                GenericoComboObject comboObj = new GenericoComboObject(items[i][0], items[i][1]);
                this.addElement(comboObj);
            }
        } catch (NullPointerException e) {
            log.fatal("1", e);
        }
    }

    public GenericoComboBoxModel(final IBaseModel[] items) {
        this(items, false);
    }

    public GenericoComboBoxModel(final IBaseModel[] items, boolean noSort) {
        super();
        try {
            if (noSort || !SettingsManager.getInstance().getConfig("SortAllCombos", "1").equals("1")) {
                sortByNone(items);
            } else {
                sortByDisplay(items);
            }
        } catch (NullPointerException ex) {
            log.fatal("Ops 2", ex);
        }
    }

    public int getIndexByDisplay(String objectToFind) {
        int ret = 0;
        if (objectToFind != null) {
            String objToCompare;
            for (int ii = 0; ii < getSize(); ii++) {
                objToCompare = ((GenericoComboObject) getElementAt(ii)).getComboDisplay();
                if (objectToFind.equals(objToCompare)) {
                    return ii;
                }
            }
        }
        return ret;
    }

    public int getIndexById(String objectToFind) {
        int ret = 0;
        if (objectToFind != null) {
            String objToCompare;
            for (int ii = 0; ii < getSize(); ii++) {
                objToCompare = ((GenericoComboObject) getElementAt(ii)).getComboId();
                if (objectToFind.equals(objToCompare)) {
                    return ii;
                }
            }
        }
        return ret;
    }

    public List<GenericoComboObject> getElementAll() {
        List<GenericoComboObject> ret = new ArrayList();
        for (int ii = 0; ii < getSize(); ii++) {
            ret.add((GenericoComboObject) getElementAt(ii));
        }
        return ret;
    }

    private void sortByDisplay(final IBaseModel[] items) {
        List<GenericoComboObject> list = new ArrayList();
        for (IBaseModel elem : items) {
            GenericoComboObject comboObj = new GenericoComboObject((IBaseModel) elem);
            list.add(comboObj);
        }
        Collections.sort(list);
        for (GenericoComboObject gco : list) {
            addElement(gco);
        }
    }

    private void sortByNone(final IBaseModel[] items) {
        for (IBaseModel elem : items) {
            addElement(new GenericoComboObject((IBaseModel) elem));
        }
    }
}
