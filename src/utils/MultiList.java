/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
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
import java.util.TreeSet;

/**
 *
 * @author jmoura
 */
public class MultiList<K extends String, V extends Object> implements Serializable {

    private final SortedMap<String, Set> counter = new TreeMap<String, Set>();

    public SortedMap<String, Set> getCounter() {
        return counter;
    }

    public int getCounter(String key) {
        try {
            return counter.get(key).size();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    public Set getKeyMinElem() {
        if (counter.isEmpty()) {
            return new TreeSet();
        }
        int minCount = 9999999;
        String keyMin = "";
        for (String key : counter.keySet()) {
            if (minCount > counter.get(key).size()) {
                minCount = counter.get(key).size();
                keyMin = key;
            }
        }
        return counter.get(keyMin);
    }

    public Set getKeyMaxElem() {
        if (counter.isEmpty()) {
            return new TreeSet();
        }
        int maxCount = -1;
        String maxKey = "";
        for (String key : counter.keySet()) {
            if (maxCount < counter.get(key).size()) {
                maxCount = counter.get(key).size();
                maxKey = key;
            }
        }
        return counter.get(maxKey);
    }

    public Set<String> getKeys() {
        return counter.keySet();
    }

    public Set getSet(String key) {
        return counter.get(key);
    }

    public void add(String key, V elemList) {
        if (counter.containsKey(key)) {
            counter.get(key).add(elemList);
        } else {
            final TreeSet treeSet = new TreeSet();
            treeSet.add(elemList);
            counter.put(key, treeSet);
        }
    }

    public void remove(String key, V elemList) {
        try {
            counter.get(key).remove(elemList);
        } catch (Exception e) {
        }
    }

    public V removeRand(String key) {
        final List<V> remainingElem = new ArrayList(counter.get(key));
        Collections.shuffle(remainingElem);
        final V remove = remainingElem.get(0);
        remove(key, remove);
        return remove;
    }

    public V removeRandFromMax() {
        final Set setMaxElem = getKeyMaxElem();
        final List<V> remainingElem = new ArrayList(setMaxElem);
        Collections.shuffle(remainingElem);
        final V remove = remainingElem.get(0);
        setMaxElem.remove(remove);
        return remove;
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
