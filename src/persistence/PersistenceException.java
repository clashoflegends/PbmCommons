/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence;

import java.io.Serializable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Walter
 */
public class PersistenceException extends Exception implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -7627358693367987945L;
    private static Log log = LogFactory.getLog(PersistenceException.class);

    public PersistenceException() {

    }

    public PersistenceException(Throwable cause) {
        super("Persistency layer error.", cause);
        log.error("Erro na camada de Persistencia", cause);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
        log.error(message, cause);
    }

    public PersistenceException(String message) {
        super(message);
        log.error(message);
    }
}
