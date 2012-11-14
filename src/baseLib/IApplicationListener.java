/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package baseLib;

/**
 *
 * @author gurgel
 */
public interface IApplicationListener extends java.util.EventListener {

    public void applicationDidInit();

    public void applicationExiting();

    public boolean canApplicationExit();
}
