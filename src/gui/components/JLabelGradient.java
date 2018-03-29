/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.components;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.Serializable;
import javax.swing.JLabel;

/**
 *
 * @author jmoura
 */
public class JLabelGradient extends JLabel implements Serializable {

    private final Color colorStart = Color.CYAN;
    private final Color colorFinal = Color.RED;
    private float percent = 0f;
    private int minBarSize = 12;

    /**
     * @param minBarSize the minBarSize to set
     */
    public void setMinBarSize(int minBarSize) {
        this.minBarSize = minBarSize;
    }

    /**
     * @param percent the percent to set
     */
    public void setPercent(float actionCount, float slotCount) {
        if (slotCount > 0) {
            percent = actionCount / slotCount;
        } else {
            percent = 1f;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int widthStart;
        if (percent >= 1f) {
            widthStart = getWidth();
        } else if (percent <= 0f) {
            widthStart = 0;
        } else {
            widthStart = (int) Math.min(percent * getWidth(), getWidth() - minBarSize);
        }
        Graphics2D graphics2d = (Graphics2D) g.create();
        graphics2d.setPaint(new GradientPaint(widthStart, 0, getBackground(), getWidth(), 0, colorFinal));
        graphics2d.fillRect(0, getHeight() / 2, getWidth(), getHeight()); // getHeight() / 2
        graphics2d.dispose();

        super.paintComponent(g);
    }

}
