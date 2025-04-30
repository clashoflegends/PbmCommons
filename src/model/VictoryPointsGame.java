/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Prepping and structuring data for Java FX Graphs in Counselor.
 *
 * @author gurgel
 */
public class VictoryPointsGame implements Serializable {

    private final SortedMap<Nacao, SortedMap<Integer, Integer>> nationsPoints = new TreeMap(); //<Nation, <Turn, Points>
    private final Set<Integer> turnList = new TreeSet();

    public void addPoints(Nacao nation, int turn, int points) {
        //check if nation exists
        if (nationsPoints.containsKey(nation)) {
            nationsPoints.get(nation).put(turn, points);
        } else {
            //add nation and set
            final SortedMap newMap = new TreeMap();
            newMap.put(turn, points);
            nationsPoints.put(nation, newMap);
        }
        //update list of turns for easy keys extraction
        turnList.add(turn);
    }

    public List<String> getTurnListAsString() {
        List<String> ret = new ArrayList<>();
        for (Integer turn : turnList) {
            ret.add(turn + "");
        }
        return ret;
    }

    public List<Integer> getTurnList() {
        return new ArrayList<>(turnList);
    }

    public Set<Nacao> getNationsList() {
        return nationsPoints.keySet();
    }

    public SortedMap<Integer, Integer> getNationPoints(Nacao nation) {
        return nationsPoints.get(nation);
    }

    public boolean isEmpty() {
        return turnList.isEmpty();
    }
}
