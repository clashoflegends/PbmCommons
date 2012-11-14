/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import java.util.SortedMap;
import model.Cenario;
import model.Ordem;
import persistence.PersistenceException;

/**
 *
 * @author gurgel
 */
public interface IOrdemDao {

    public Ordem get(int id) throws PersistenceException;

    public SortedMap<String, Ordem> list(Cenario cenario) throws PersistenceException;
}
