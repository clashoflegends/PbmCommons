/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.Serializable;

/**
 * To find it in JTables and paint differently! No other use for most things.
 *
 * @author gurgel
 */
public final class StringIntSortedCell extends Number implements Serializable, Comparable<Object> {

    private int value = 0;
    private String description = "";

    public StringIntSortedCell(int armySizeInt, String sizeDescription) {
        value = armySizeInt;
        description = sizeDescription;
    }

    @Override
    public String toString() {
        return this.description;
    }

    @Override
    public int intValue() {
        return this.value;
    }

    @Override
    public long longValue() {
        return this.value;
    }

    @Override
    public float floatValue() {
        return this.value;
    }

    @Override
    public double doubleValue() {
        return this.value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int compareTo(Object o) {
        StringIntSortedCell outro = (StringIntSortedCell) o;
        final int sortOrder = this.getValue() - outro.getValue();
        if (sortOrder == 0) {
            return this.description.compareToIgnoreCase(outro.description);
        } else {
            return sortOrder;
        }
    }
}
