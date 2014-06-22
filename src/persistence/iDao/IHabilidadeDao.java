/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import java.util.SortedMap;
import model.Habilidade;
import persistence.PersistenceException;

/**
 *
 * @author jmoura
 */
public interface IHabilidadeDao {

    public Habilidade get(String cd) throws PersistenceException;

    public SortedMap<String, Habilidade> list() throws PersistenceException;
}
