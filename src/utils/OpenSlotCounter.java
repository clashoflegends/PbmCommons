/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.Serializable;

/**
 * To find it in JTables and paint differently! No other use for most things.
 * @author gurgel
 */
public final class OpenSlotCounter extends Number implements Serializable {

    private int openSlotQt = 0;

    public OpenSlotCounter(int qt) {
        this.openSlotQt = qt;
    }

    @Override
    public String toString() {
        return this.openSlotQt + "";
    }

    public int getOpenSlotQt() {
        return openSlotQt;
    }

    public void setOpenSlotQt(int openSlotQt) {
        this.openSlotQt = openSlotQt;
    }

    @Override
    public int intValue() {
        return this.openSlotQt;
    }

    @Override
    public long longValue() {
        return this.openSlotQt;
    }

    @Override
    public float floatValue() {
        return this.openSlotQt;
    }

    @Override
    public double doubleValue() {
        return this.openSlotQt;
    }
}
