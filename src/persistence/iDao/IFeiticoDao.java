/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import java.util.List;
import model.Cenario;
import model.Feitico;
import persistenceCommons.PersistenceException;


/**
 *
 * @author gurgel
 */
public interface IFeiticoDao {

    public Feitico get(int id)throws PersistenceException;

    public List<Feitico> list(Cenario cenario)throws PersistenceException;

}
