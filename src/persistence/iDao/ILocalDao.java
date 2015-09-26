/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import java.util.SortedMap;
import model.Local;
import model.Partida;
import persistence.PersistenceException;

/**
 *
 * @author gurgel
 */
public interface ILocalDao {

    public void clear();

    //public Local get(int id, boolean visible, boolean fullInfo) throws PersistenceException;
    public Local getInfoBasico(int id) throws PersistenceException;

    public Local getInfoVisible(int id) throws PersistenceException;

    public Local getInfoFull(int id) throws PersistenceException;

//    public Local get(int id, int turno, Cenario cenario) throws PersistenceException;
    public Local get(String coord) throws PersistenceException;

    public SortedMap<String, Local> listWithVisibility(Partida partida) throws PersistenceException;

    public SortedMap<String, Local> listFull(Partida partida) throws PersistenceException;

    public void load(Partida partida) throws PersistenceException;

    public void unload(Partida partida);

    public void update(Local local);
}
