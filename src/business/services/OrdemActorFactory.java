/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.services;

import java.io.Serializable;
import model.PersonagemOrdem;

/**
 *
 * @author jmoura
 */
public class OrdemActorFactory implements Serializable {

    private static OrdemActorFactory instance;

    public static synchronized OrdemActorFactory getInstance() {
        if (OrdemActorFactory.instance == null) {
            OrdemActorFactory.instance = new OrdemActorFactory();
        }
        return OrdemActorFactory.instance;
    }

    public PersonagemOrdem getOrdem(int cdOrder) {
        return createOrdem(cdOrder);
    }

    private PersonagemOrdem createOrdem(int cdOrder) {
        switch (cdOrder) {
            case 411:
                return new PersonagemOrdem();
            case 550:
                return new PersonagemOrdem();

            default:
                return new PersonagemOrdem();
        }
    }
}
