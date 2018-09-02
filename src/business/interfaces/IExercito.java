/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.interfaces;

import java.util.SortedMap;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Terreno;

/**
 *
 * @author jmoura
 */
public interface IExercito {

    public int getMoral();

    public int getPericiaComandante();

    public Local getLocal();

    public Terreno getTerreno();

    public Nacao getNacao();

    public SortedMap<String, Pelotao> getPelotoes();

    public int getBonusAttack();

    public int getTatica();

    public int getBonusDefense();

    @Override
    public String toString();
}
