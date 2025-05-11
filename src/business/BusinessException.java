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
public class BusinessException extends Exception implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -7627356893367987945L;
    private static Log log = LogFactory.getLog(BusinessException.class);

    public BusinessException() {
    }

    public BusinessException(Throwable cause) {
        super(cause.getMessage(), cause);
        //log.error(cause.getMessage(), cause);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        //log.error(message, cause);
    }

    public BusinessException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        if (super.getMessage() != null) {
            return super.getMessage();
        } else {
            //log.error(Arrays.toString(super.getStackTrace()).replaceAll("[\\[\\]]", "").replace(", ", "\n"));
            return "no message";
        }
    }
}
