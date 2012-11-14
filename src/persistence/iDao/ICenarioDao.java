/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import model.Cenario;
import persistence.PersistenceException;

/**
 *
 * @author gurgel
 */
public interface ICenarioDao {

    public void clear();

    public Cenario get(int idPartida) throws PersistenceException;
}
