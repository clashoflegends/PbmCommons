/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package persistence.iDao;

import java.util.List;
import model.Cenario;
import model.HabilidadeNacao;
import persistence.PersistenceException;

/**
 *
 * @author jmoura
 */
public interface IHabilidadeNacaoDao {
    public HabilidadeNacao get(int id)throws PersistenceException;

    public List<HabilidadeNacao> list(Cenario cenario)throws PersistenceException;

}
