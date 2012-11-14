/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package baseLib;

import java.io.Serializable;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author gurgel
 */
public class GenericoTableModel extends DefaultTableModel implements Serializable {

    private Class[] classNames;
    private boolean[] canEditCol = new boolean[]{false, false, false};

    public GenericoTableModel(String[] columnNames, Object[][] data) {
        this.setDataVector(data, columnNames);
    }

    public GenericoTableModel(String[] columnNames, Object[][] data, Class[] classnames) {
        this.setDataVector(data, columnNames);
        this.classNames = classnames;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        if (this.classNames != null) {
            try {
                return this.classNames[columnIndex];
            } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
                return java.lang.String.class;
            }
        } else {
            return super.getColumnClass(columnIndex);
        }
    }

    /**
     * Don't need to implement this method unless your table's
     * editable.
     */
    @Override
    public boolean isCellEditable(int row, int col) {
        //PENDING: tornar as celulas nao editaveis de outra forma.
        //Note that the data/cell address is constant,
        //no matter where the cell appears onscreen.
        //        if (col < 2) {
        //            return false;
        //        } else {
        //            return false;
        //        }
        try {
            return canEditCol[col];
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    public void setEditable(boolean[] edit) {
        this.canEditCol = edit;
    }
}
