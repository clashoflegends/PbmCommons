/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import model.Local;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class ArmyPath implements Serializable, Cloneable {

    private static final Log log = LogFactory.getLog(ArmyPath.class);
    private Local start;
    private List<MovimentoExercito> step = new ArrayList<MovimentoExercito>();
    private int cost = 0;

    @Override
    public ArmyPath clone() throws CloneNotSupportedException {
        ArmyPath armyPath = (ArmyPath) super.clone();
        armyPath.step = new ArrayList<MovimentoExercito>();
        for (MovimentoExercito mov : this.step) {
            armyPath.step.add(mov.clone());
        }
        return armyPath;
    }

    public ArmyPath(Local atual) {
        this.start = atual;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int totalCost) {
        this.cost = totalCost;
    }

    public void addStep(MovimentoExercito movEx) {
        try {
            MovimentoExercito movExNew = movEx.clone();
            this.step.add(movExNew);
        } catch (CloneNotSupportedException ex) {
            log.fatal(ex);
            throw new UnsupportedOperationException(ex);
        }
    }

    /**
     * @return the start
     */
    public Local getStart() {
        return start;
    }

    @Override
    public String toString() {
        String ret = start.getCoordenadas();
        for (MovimentoExercito me : step) {
            ret += " -> " + me.getDestino().getCoordenadas();
        }
        ret += " (" + cost + ") ";
        return ret;
    }

    public MovimentoExercito getLastMove() {
        return step.get(step.size() - 1);
    }

    public List<MovimentoExercito> getSteps() {
        return this.step;
    }
}
