/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence;

import java.io.Serializable;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class BundleManager implements Serializable {

    private static final Log log = LogFactory.getLog(BundleManager.class);

    public BundleManager() {
    }

    public String getString(String label) {
        String ret = "N/A (Missing Translation)";
        try {
            ret = ResourceBundle.getBundle("labels").getString(label);
        } catch (MissingResourceException e) {
            try {
                ret = ResourceBundle.getBundle("mensagens").getString(label);
            } catch (MissingResourceException ex) {
                //YYY: to de/activate the fatal stop when a string is not found
//                throw new UnsupportedOperationException("Missing string: " + label);
                log.fatal("Missing string: " + label);
            }
        }
        return ret;
    }
}
