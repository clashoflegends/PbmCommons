/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.interfaces;

import model.Nacao;

/**
 *
 * @author jmoura
 */
public interface IOrder {

    public Nacao getNacao();

    public boolean isAtivo();

    public boolean isPersonagem();

    public int getOrdensExtraQt();
    
}
