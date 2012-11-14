/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import persistence.PersistenceException;
import java.util.List;
import model.Jogador;
import model.Partida;


/**
 *
 * @author gurgel
 */
public interface IJogadorDao {

    public void clear();

    public Jogador get(int id) throws PersistenceException;

    public List<Jogador> list(Partida partida) throws PersistenceException;
}
