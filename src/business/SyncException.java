/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business;

import java.io.Serializable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Walter
 */
public class SyncException extends Exception implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -7627356893367987945L;
    private static Log log = LogFactory.getLog(SyncException.class);

    public SyncException() {
    }

    public SyncException(Throwable cause) {
        super("Erro na camada Bussiness", cause);
        //log.error("Erro na camada de Persistencia", cause);
    }

    public SyncException(String message, Throwable cause) {
        super(message, cause);
        //log.error(message, cause);
    }

    public SyncException(String message) {
        super(message);
        //log.error(message, this);
    }
}
