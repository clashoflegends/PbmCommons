/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import java.sql.ResultSet;
import java.util.SortedMap;
import model.Cidade;
import model.Partida;
import persistenceCommons.PersistenceException;

/**
 *
 * @author gurgel
 */
public interface ICidadeDao {

    public void clear();

    public Cidade get(int id) throws PersistenceException;

    public SortedMap<String, Cidade> list(Partida partida) throws PersistenceException;

    public void update(Cidade cidade);

    public Cidade get(ResultSet rs, Partida partida) throws PersistenceException;
}
