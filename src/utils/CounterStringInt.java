/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.Serializable;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * servers to concatenate strings
 *
 * @author jmoura
 */
public final class CounterStringInt implements Serializable {

    private final SortedMap<String, Integer> counter = new TreeMap<String, Integer>();

    public SortedMap<String, Integer> getCounter() {
        return counter;
    }

    public Set<String> getKeys() {
        return counter.keySet();
    }

    public int getTotal() {
        int ret = 0;
        for (Integer qtd : counter.values()) {
            ret += qtd;
        }
        return ret;
    }

    public int getValue(String key) {
        return counter.get(key);
    }

    public void add(String key, int value) {
        if (counter.containsKey(key)) {
            counter.put(key, counter.get(key) + value);
        } else {
            counter.put(key, value);
        }
    }

    @Override
    public String toString() {
        if (counter.isEmpty()) {
            return "Counter is empty";
        }
        String ret = "";
        for (String key : counter.keySet()) {
            ret += String.format("%s: %s; ", key, counter.get(key));
        }
        return ret;
    }
}
