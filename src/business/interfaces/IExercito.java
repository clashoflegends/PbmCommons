/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.interfaces;

import java.util.SortedMap;
import model.Local;
import model.Nacao;
import model.Pelotao;
import model.Personagem;
import model.Terreno;

/**
 *
 * @author jmoura
 */
public interface IExercito {
    //TODO: refactor to remove a bunch of tasks from ExercitoControl and move them to ExercitoFacade or ExercitoControlFacade 

    public int getMoral();

    public Local getLocal();

    public Terreno getTerreno();

    public Nacao getNacao();

    public SortedMap<String, Pelotao> getPelotoes();

    public int getTatica();

    public int getAttackBonus();

    public int getArmyDefenseBonus();

    public void setArmyDefenseBonus(int bonus);

    public void setCombatDamageClear();

    public boolean isGarrison();

    public void doDisband();

    public void doDisbandWithMsg();

    public void setDisband(boolean disband);

    public boolean isDisband();

    public Personagem getComandanteModel();

    public String getComandanteNome();

    public int getComandantePericia();

    @Override
    public String toString();

}
