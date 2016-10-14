/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import java.util.List;
import model.Alianca;
import persistenceCommons.PersistenceException;


/**
 *
 * @author gurgel
 */
public interface IAliancaDao {

    public Alianca get(int id) throws PersistenceException;

    public List<Alianca> list() throws PersistenceException;
}
