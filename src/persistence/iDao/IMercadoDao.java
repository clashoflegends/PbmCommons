/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import model.Mercado;
import model.Partida;
import persistence.PersistenceException;

/**
 *
 * @author gurgel
 */
public interface IMercadoDao {

    public void clear();

    public Mercado get(Partida partida) throws PersistenceException;

}
