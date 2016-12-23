/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import model.Partida;
import persistenceCommons.PersistenceException;

/**
 *
 * @author gurgel
 */
public interface IPartidaDao {

    public void clear();

    public Partida get(int idPartida, int turno) throws PersistenceException;

    public boolean setRun(int idPartida);

    public boolean setRunNow(int idPartida);
}
