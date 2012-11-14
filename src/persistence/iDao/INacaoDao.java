/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import java.util.List;
import java.util.SortedMap;
import model.ComandoDetail;
import model.Nacao;
import model.Partida;
import persistence.PersistenceException;

/**
 *
 * @author gurgel
 */
public interface INacaoDao {

    public void clear();

    public Nacao get(int id) throws PersistenceException;

    public SortedMap<String, Nacao> list(Partida partida) throws PersistenceException;

    public boolean salvaOrdens(int idPartida, int turno, int idJogador, int idNacao, String ordensCsv, List<ComandoDetail> comDet) throws PersistenceException;
}
