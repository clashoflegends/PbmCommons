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

    @Override
    public String toString();
}
