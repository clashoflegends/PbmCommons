/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        try {
            return counter.get(key);
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public String getKeyWithMinCount() {
        if (counter.isEmpty()) {
            return "";
        }
        int maxCount = 999999999;
        String maxKey = "";
        final List<String> keySet = new ArrayList<String>(counter.keySet());
        Collections.shuffle(keySet);
        for (String key : keySet) {
            if (maxCount > counter.get(key)) {
                maxCount = counter.get(key);
                maxKey = key;
            }
        }
        return maxKey;
    }

    public String getKeyWithMaxCount() {
        if (counter.isEmpty()) {
            return "";
        }
        int minCount = -999999999;
        String maxKey = "";
        final List<String> keySet = new ArrayList<String>(counter.keySet());
        Collections.shuffle(keySet);
        for (String key : keySet) {
            if (counter.get(key) > minCount) {
                minCount = counter.get(key);
                maxKey = key;
            }
        }
        return maxKey;
    }

    public void remove(String key) {
        counter.remove(key);
    }

    public void add(String key) {
        add(key, 1);
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
