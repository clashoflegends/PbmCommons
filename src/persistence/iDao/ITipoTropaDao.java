/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import model.Cenario;
import model.TipoTropa;
import persistenceCommons.PersistenceException;

/**
 *
 * @author jmoura
 */
public interface ITipoTropaDao {

    public void clear();

    public TipoTropa get(int id, Cenario cenario) throws PersistenceException;

    public TipoTropa get(String cd, Cenario cenario) throws PersistenceException;

    /**
     * Fills the scenario's troop catalogue with every type the GAME can field, independently of what
     * the exporting player can see.
     *
     * Until this existed the catalogue was a side effect: both {@code get} overloads end with
     * {@code cenario.addTipoTropa(...)}, so it held whatever the export happened to touch, and fog
     * removes the referencing objects. Measured on game 916 turn 4: 33 types across its 14 player
     * files, no more than 30 in any one of them, and "Mountain Giants" reached exactly ONE file.
     * The BattleSim reads this map, so its roster was short by whatever the player had not scouted.
     *
     * The commented-out {@code list(Cenario)} this replaces was never written because there is
     * nothing to select: {@code ex_tipo_tropa} has no scenario column, and which troops belong to a
     * game is defined by what REFERENCES them. So it is derived rather than listed.
     *
     * @param idPartida the game, not the scenario - races, items and starting armies are per game,
     * which is what makes War of Dwarves and Orcs work, its races being chosen per game.
     */
    public void loadCatalogo(Cenario cenario, int idPartida) throws PersistenceException;
}
