/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import java.io.Serializable;
import java.util.Collection;
import model.Local;
import model.Nacao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import utils.CounterStringInt;

/**
 *
 * @author gurgel
 */
public class PointsFacade implements Serializable {

    private static final Log log = LogFactory.getLog(PointsFacade.class);
    private static final BundleManager label = SettingsManager.getInstance().getBundleManager();
    private final LocalFacade localFacade = new LocalFacade();
    private final CidadeFacade cidadeFacade = new CidadeFacade();

    public CounterStringInt doVictoryDomination(Collection<Local> locais, Nacao barbarians) {
        //Victory goal: Domination, game ends when one nation has more key cities (original capitals) than all opponents combined
        final CounterStringInt counterSingle = new CounterStringInt();
        for (Local hex : locais) {
            if (!localFacade.isKeyLocalCity(hex)) {
                continue;
                /*
                - targaryen can move city
                - city may have been razed then re-populated
                - city may have been razed and be empity hex
                - city may have been captured
                - can't double count when tragaryen moves cities (original hex and new city)
                 */
            }
            if (localFacade.isCidade(hex) && hex.getCidade().getNacao() != null) {
                counterSingle.add(hex.getCidade().getNacao().getNome(), 1);
            } else {
                counterSingle.add(barbarians.getNome(), 1);
            }
        }
        return counterSingle;
    }

    public CounterStringInt doDominationBattleRoyale(Collection<Local> locais, Nacao barbarians) {
        final CounterStringInt counterSingle = new CounterStringInt();
        for (Local hex : locais) {
            int cityPt = cidadeFacade.getPointsDomination(hex);
            if (cityPt <= 0) {
                continue;
            }
            if (hex.getCidade().getNacao() != null) {
                counterSingle.add(hex.getCidade().getNacao().getNome(), cityPt);
            } else {
                counterSingle.add(barbarians.getNome(), cityPt);
            }
        }
        return counterSingle;
    }
}
