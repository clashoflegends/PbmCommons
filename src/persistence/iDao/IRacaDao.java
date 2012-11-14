/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import model.Cenario;
import model.Raca;
import persistence.PersistenceException;

/**
 *
 * @author gurgel
 */
public interface IRacaDao {

    public void clear();

    public Raca get(int id, Cenario cenario) throws PersistenceException;
}
