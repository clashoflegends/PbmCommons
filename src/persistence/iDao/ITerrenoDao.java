/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import java.util.SortedMap;
import persistence.PersistenceException;
import model.Cenario;
import model.Terreno;

/**
 *
 * @author gurgel
 */
public interface ITerrenoDao {

    public Terreno get(Cenario cenario, String cdTerreno) throws PersistenceException;

    public SortedMap<String, Terreno> list(Cenario cenario) throws PersistenceException;
}
