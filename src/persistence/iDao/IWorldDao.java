/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence.iDao;

import java.io.File;
import model.World;
import persistenceCommons.PersistenceException;

/**
 *
 * @author gurgel
 */
public interface IWorldDao {

    public World get(File file) throws PersistenceException;

    /**
     *
     * @param world
     * @return full path to base filename
     * @throws PersistenceException
     */
    public String save(World world) throws PersistenceException;

    public String save(World world, File file) throws PersistenceException;
}
