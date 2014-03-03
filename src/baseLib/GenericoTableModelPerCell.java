/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package baseLib;

import java.io.Serializable;

/**
 * authorizes each cell to be enable/editable.
 *
 * @author gurgel
 */
public class GenericoTableModelPerCell extends GenericoTableModel implements Serializable {

    private boolean[][] canEditCol = new boolean[][]{{false}};

    public GenericoTableModelPerCell(String[] columnNames, Object[][] data, Class[] classnames) {
        super(columnNames, data, classnames);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        try {
            return canEditCol[row][col];
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    public void setEditable(boolean[][] edit) {
        this.canEditCol = edit;
    }
}
