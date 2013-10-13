/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import java.util.SortedMap;
import model.Pelotao;
import persistence.PersistenceException;

/**
 *
 * @author jmoura
 */
public interface IPelotaoDao {

    public void clear();

    public SortedMap<String, Pelotao> list(int idExercito) throws PersistenceException;
}
