/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import model.Cenario;
import model.Partida;
import persistenceCommons.PersistenceException;

/**
 *
 * @author gurgel
 */
public interface ICenarioDao {

    public void clear();

    /**
     * To be called from PbmDistiler<br>
     * Also hides forbidden orders
     *
     * @param idPartida
     * @return
     * @throws PersistenceException
     */
    public Cenario get(Partida idPartida) throws PersistenceException;

}
