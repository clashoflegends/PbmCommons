/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import model.Cenario;
import model.TipoTropa;
import persistence.PersistenceException;

/**
 *
 * @author jmoura
 */
public interface ITipoTropaDao {

    public void clear();

    public TipoTropa get(int id, Cenario cenario) throws PersistenceException;

    public TipoTropa get(String cd, Cenario cenario) throws PersistenceException;
//    public SortedMap<String, TipoTropa> list(Cenario cenario) throws PersistenceException;
}
