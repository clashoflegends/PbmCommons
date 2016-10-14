/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import java.util.SortedMap;
import model.Exercito;
import model.Partida;
import persistenceCommons.PersistenceException;

/**
 *
 * @author gurgel
 */
public interface IExercitoDao {

    public void clear();

    public SortedMap<String, Exercito> list(Partida partida) throws PersistenceException;

    public SortedMap<String, Exercito> loadVisible(Partida partida) throws PersistenceException;
}
