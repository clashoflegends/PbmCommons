/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import java.util.SortedMap;
import model.Partida;
import model.Personagem;
import persistence.PersistenceException;

/**
 *
 * @author gurgel
 */
public interface IPersonagemDao {

    public void clear();

    public Personagem get(int id, int turno) throws PersistenceException;

    public Personagem get(int id, Partida partida) throws PersistenceException;

    public SortedMap<String, Personagem> list(Partida partida) throws PersistenceException;
}
