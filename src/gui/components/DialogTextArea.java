/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.io.Serializable;
import javax.swing.JFrame;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class DialogTextArea extends javax.swing.JDialog implements Serializable {

    private static final Log log = LogFactory.getLog(DialogTextArea.class);
    // Variables declaration - do not modify
    private javax.swing.JScrollPane detAjuda;
    private javax.swing.JPanel jpAjuda;
    private javax.swing.JTextArea jtaTextArea;
    // End of variables declaration

    public DialogTextArea(boolean modal) {
        super(new JFrame(), modal);
        this.setAlwaysOnTop(true);
        this.setPreferredSize(new Dimension(500, 400));
        initComponents();
        //this.add(detAjuda);
    }

    private void initComponents() {

        detAjuda = new javax.swing.JScrollPane();
        jpAjuda = new javax.swing.JPanel();
        jtaTextArea = new javax.swing.JTextArea();

        detAjuda.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        jtaTextArea.setLineWrap(true);
        jtaTextArea.setRows(80);
        jtaTextArea.setWrapStyleWord(true);

        javax.swing.GroupLayout jpAjudaLayout = new javax.swing.GroupLayout(jpAjuda);
        jpAjuda.setLayout(jpAjudaLayout);
        jpAjudaLayout.setHorizontalGroup(
                jpAjudaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jtaTextArea));
        jpAjudaLayout.setVerticalGroup(
                jpAjudaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jtaTextArea, javax.swing.GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE));

        detAjuda.setViewportView(jpAjuda);
        this.add(detAjuda);
    }

    public void setText(String text) {
        this.jtaTextArea.setText(text);
        this.jtaTextArea.setCaretPosition(0);
    }

    public void setTextBackground(Color color) {
        this.jtaTextArea.setBackground(color);
    }
}
