/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package persistence.iDao;
import java.util.SortedMap;
import model.Cenario;
import model.Produto;
import persistenceCommons.PersistenceException;



/**
 *
 * @author gurgel
 */
public interface IProdutoDao {

    public Produto get(int id) throws PersistenceException;

    public SortedMap<String, Produto> list(Cenario cenario) throws PersistenceException;

}
