/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import java.io.Serializable;
import java.util.Collection;
import model.Cidade;
import model.Jogador;
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
    private final NacaoFacade nacaoFacade = new NacaoFacade();
    private final String NotUsFlag = "foo";

    public CounterStringInt doVictoryDominationUsThem(Collection<Local> locais, Jogador player) {
        //Victory goal: Domination, game ends when one nation has more key cities (original capitals) than all opponents combined
        String teamFlag = player.getNacoes().get(player.getNacoes().firstKey()).getTeamFlag();
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
            if (!localFacade.isCidade(hex) || hex.getCidade().getNacao() == null) {
                //Unknown, make it THEM
                counterSingle.add(getUsThem(NotUsFlag, teamFlag), 1);
            } else {
                counterSingle.add(getUsThem(hex.getCidade().getNacao().getTeamFlag(), teamFlag), 1);
            }
        }
        return counterSingle;
    }

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

    public CounterStringInt doVictoryDominationTeam(Collection<Local> locais, Nacao barbarians) {
        //Victory goal: Domination, game ends when one nation has more key cities (original capitals) than all opponents combined
        final CounterStringInt counterTeam = new CounterStringInt();
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
                counterTeam.add(hex.getCidade().getNacao().getTeamFlag(), 1);
            } else {
                counterTeam.add(barbarians.getTeamFlag(), 1);
            }
        }
        return counterTeam;
    }

    public CounterStringInt doVictoryScoreUsThem(Collection<Nacao> nations, Jogador player) {
        //DB.POWER.VSP=Victory goal: Score, game ends when one nation has more victory points than all opponents combined starting on %s turn. Or 3:1 for teams
        String teamFlag = player.getNacoes().get(player.getNacoes().firstKey()).getTeamFlag();
        final CounterStringInt counterSingle = new CounterStringInt();
        for (Nacao nation : nations) {
            int vicPt = nacaoFacade.getPointVictory(nation);
            counterSingle.add(getUsThem(nation.getTeamFlag(), teamFlag), vicPt);
        }
        return counterSingle;
    }

    public CounterStringInt doVictoryConquestUsThem(Collection<Cidade> cities, Jogador player) {
        //DB.POWER.VSC=Victory goal: Conquest, game ends when one nation has more burghs and metropolis than all opponents combined starting on %s turn.  Or 3:1 for teams
        String teamFlag = player.getNacoes().get(player.getNacoes().firstKey()).getTeamFlag();
        final CounterStringInt counterSingle = new CounterStringInt();
        for (Cidade city : cities) {
            if (!cidadeFacade.isBigCity(city)) {
                continue;
            }
            counterSingle.add(getUsThem(city.getNacao(), teamFlag), 1);
        }
        return counterSingle;
    }

    public CounterStringInt doVictorySupremacyUsThem(Collection<Nacao> nations, Jogador player) {
        //DB.POWER.VSS=Victory goal: Supremacy, game ends when a team has twice as many nations as the other teams starting on %s turn. Or 3:1 for teams
        String teamFlag = player.getNacoes().get(player.getNacoes().firstKey()).getTeamFlag();
        final CounterStringInt counterSingle = new CounterStringInt();
        for (Nacao nation : nations) {
            if (!nacaoFacade.isAtivaPC(nation)) {
                continue;
            }
            counterSingle.add(getUsThem(nation.getTeamFlag(), teamFlag), 1);
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

    public CounterStringInt doDominationBattleRoyaleUsThem(Collection<Local> locais, Jogador player) {
        //DB.POWER.VCP=Victory goal: Battle Royale, game ends when one nation has more key cities points than all opponents combined starting on %s turn.  Or 3:1 for teams
        String teamFlag = player.getNacoes().get(player.getNacoes().firstKey()).getTeamFlag();
        final CounterStringInt counterSingle = new CounterStringInt();
        for (Local hex : locais) {
            int cityPt = cidadeFacade.getPointsDomination(hex);
            if (cityPt <= 0) {
                continue;
            }
            if (hex.getCidade().getNacao() == null) {
                //Unknown, make it THEM
                counterSingle.add(getUsThem(NotUsFlag, teamFlag), cityPt);
            } else {
                counterSingle.add(getUsThem(hex.getCidade().getNacao().getTeamFlag(), teamFlag), cityPt);
            }
        }
        return counterSingle;
    }

    private String getUsThem(Nacao nation, String baseFlag) {
        try {
            return getUsThem(nation.getTeamFlag(), baseFlag);
        } catch (NullPointerException e) {
            return getUsThem(NotUsFlag, baseFlag);
        }
    }

    private String getUsThem(String flag, String baseFlag) {
        if (flag.equals(baseFlag)) {
            return "Us";
        } else {
            return "Them";
        }
    }
}
