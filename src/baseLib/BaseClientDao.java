/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package baseLib;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author gurgel
 */
public class BaseClientDao implements Serializable {

    private static final SortedMap<Integer, BaseModel> cacheById = new TreeMap();

    protected void addCache(BaseModel elemento) {
        cacheById.put(elemento.getId(), elemento);
    }

    public void clear() {
        cacheById.clear();
    }

    protected boolean isCache(int id) {
        return (cacheById.get(id) != null);
    }

    protected Object getCache(int id) {
        return (cacheById.get(id));
    }
}
