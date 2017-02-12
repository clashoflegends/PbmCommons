/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;

/**
 *
 * @author gurgel
 */
public class Produto extends BaseModel implements Comparable<Object> {

    private boolean money = false;
    private boolean armor = false;
    private boolean weapon = false;

    /*
     * If this < o, return a negative value
     * If this = o, return 0
     * If this > o, return a positive value
     */
    @Override
    public int compareTo(Object o) {
        Produto outro = (Produto) o;
        return (this.getNome().compareTo(outro.getNome()));
    }
    /* old implementation
     * follow the order in the database
     @Override
     public int compareTo(Object o) {
     Produto outro = (Produto) o;
     return (this.getId() - outro.getId());
     }
     */

    public boolean isMoney() {
        return money;
    }

    public void setMoney(boolean money) {
        this.money = money;
    }

    public boolean isArmor() {
        return armor;
    }

    public void setArmor(boolean armor) {
        this.armor = armor;
    }

    public boolean isWeapon() {
        return weapon;
    }

    public void setWeapon(boolean weapon) {
        this.weapon = weapon;
    }

    public boolean isFood() {
        return this.getCodigo().equals("cm");
    }

    public boolean isWood() {
        return this.getCodigo().equals("le");
    }
}
