/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.report;

import business.converter.ConverterFactory;
import business.facade.CenarioFacade;
import business.services.ComparatorFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import model.Feitico;
import model.Habilidade;
import model.Ordem;
import model.Partida;
import model.Terreno;
import model.TipoTropa;
import business.converter.TitleFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.reports.SysReport;
import persistenceCommons.BundleManager;
import persistenceCommons.PersistenceException;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;
import persistenceLocal.PathFactory;

/**
 *
 * @author jmoura
 */
public class RegrasReportDefault implements Serializable {

    private static final Log log = LogFactory.getLog(RegrasReportDefault.class);
    private final CenarioFacade cenarioFacade = new CenarioFacade();
    private Partida partida;
    protected static BundleManager labels;
    private final SysReport report = new SysReport();

    public RegrasReportDefault(Partida aPartida) {
        labels = SettingsManager.getInstance().getBundleManager();
        this.partida = aPartida;
    }

    public void printMain() {
        doAbreReport();
        //controle da impressao
        printInfos();
        printIntro();
        printOrdens();
        printFeiticos();
        printSkills();
        printTropasSection();
        printTaticasTropas();
        if (cenarioFacade.hasLockedAlliances(getPartida().getCenario())) {
            //todo: what???
        }
        doFechaReport();
    }

    protected SysReport getReport() {
        return report;
    }

    private void doFechaReport() {
        //fecha saidas
        getReport().consoleDesviaClose();
    }

    private void doAbreReport() {
        getReport().setPrintToRoot(true);
        //redireciona console de PDF/HTML para arquivo.
        getReport().consoleDesvia(
                PathFactory.getDirName(getPartida()),
                PathFactory.getFileNameRegras(getPartida()),
                getPartida().getTurno(),
                !SettingsManager.getInstance().isDebug());
    }

    /**
     * @return the partida
     */
    public Partida getPartida() {
        return partida;
    }

    /**
     * @param partida the partida to set
     */
    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    private void printInfos() {
        getReport().writeTitle(String.format(labels.getString("RULES.TITLE.MAIN"), getPartida().getCenario().getNome()));
        getReport().write(String.format(labels.getString("RULES.TITLE.MAIN.SUB")));
        getReport().write(String.format(labels.getString("RULES.TITLE.DEADLINE"), getPartida().getDeadline().toDateString()));
        getReport().write(String.format(labels.getString("RULES.TITLE.TIME"), SysApoio.nowTime()));
        //print date
    }

    private void printIntro() {
        getReport().writeTitle(String.format(labels.getString("RULES.TITLE.INTRODUCTION"), getPartida().getCenario().getNome()));
        getReport().write(String.format(labels.getString("RULES.INTRODUCTION.BODY")));
    }

    private void printOrdens() {
        getReport().write("\n");
        getReport().write("\n");
        getReport().lineSeparator();
        getReport().writeTitle(labels.getString("RULES.TITLE.ORDENS"));
        getReport().writeBold(labels.getString("RULES.TITLE.ORDENS.SUB"));
        getReport().write("\n");
        for (Ordem ordem : partida.getCenario().getOrdens().values()) {
            getReport().writeBoldFirstLine(TitleFactory.getOrdemDisplay(ordem));
            getReport().write("\n");
        }
    }

    private void printFeiticos() {
        if (!partida.getCenario().getFeiticos().isEmpty()) {
            getReport().write("\n");
            getReport().write("\n");
            getReport().lineSeparator();
            getReport().writeTitle(labels.getString("RULES.TITLE.FEITICO"));
            getReport().writeBold(labels.getString("RULES.TITLE.FEITICO.SUB"));
            getReport().write("\n");
            for (Feitico feitico : partida.getCenario().getFeiticos()) {
//            getReport().writeTitle(feitico.getDescricao());
                getReport().writeBoldFirstLine(TitleFactory.getFeiticoDisplay(feitico));
                getReport().write("\n");
            }
        }
    }

    private void printTropasSection() {
        final int borderBefore = getReport().getBorder();
        getReport().setBorder(1);
        getReport().setConvertNewLine(true);
        getReport().write("\n");
        getReport().write("\n");
        getReport().lineSeparator();
        getReport().write("\n");
        getReport().writeTitle(labels.getString("RULES.TITLE.TROOPS.TITLE"));
        try {
            printTropasTable();
        } catch (PersistenceException ex) {
            getReport().write("Ops. Error on printTropasSection\n");
            log.error(ex);
        }
        getReport().write("\n");
        getReport().setBorder(borderBefore);
        getReport().setConvertNewLine(false);
    }

    private void printTaticasTropas() {
        final int borderBefore = getReport().getBorder();
        getReport().setBorder(1);
        getReport().setConvertNewLine(true);
        getReport().write("\n");
        getReport().write("\n");
        getReport().lineSeparator();
        getReport().write("\n");
        getReport().writeTitle(labels.getString("RULES.TITLE.TATICAS.COMBAT"));
        getReport().write(labels.getString("RULES.TITLE.TATICAS.COMBATSUB"));
        printTaticasVsTaticasTable();
        getReport().write("\n");
        getReport().writeTitle(labels.getString("RULES.TITLE.TATICAS"));
        getReport().writeTitle(labels.getString("TACTIC.LAND"));
        getReport().write(labels.getString("RULES.TITLE.TATICAS.SUB"));
        getReport().write(labels.getString("RULES.TITLE.TATICAS.TABLES"));
        getReport().write(labels.getString("RULES.TITLE.TATICAS.DAMAGE"));
        printTaticasTropasTable(false);
        getReport().write("\n");
        getReport().writeTitle(labels.getString("TACTIC.WATER"));
        getReport().write(labels.getString("RULES.TITLE.TATICAS.SUB"));
        getReport().write(labels.getString("RULES.TITLE.TATICAS.TABLES"));
        getReport().write(labels.getString("RULES.TITLE.TATICAS.DAMAGE"));
        printTaticasTropasTable(true);
        getReport().setBorder(borderBefore);
        getReport().setConvertNewLine(false);
    }

    private void printTaticasTropasTable(boolean water) {
        final String[][] taticas = cenarioFacade.listTaticas(partida.getCenario());
        for (Terreno terreno : partida.getCenario().getTerrenos().values()) {
            getReport().writeTabela(taticas.length - 1);
            //title row
            for (String[] tatica : taticas) {
                if (!tatica[1].equals("pa")) {
                    getReport().writeCelulaBold(String.format(labels.getString("S.ON.S"), tatica[0], terreno.getNome()));
                }
            }
            getReport().writeTabelaLine();
            //content row
            for (String[] tatica : taticas) {
                if (tatica[1].equals("pa")) {
                    continue;
                }
                final List<TipoTropa> tropas = new ArrayList<TipoTropa>();
                for (TipoTropa tipoTropa : getPartida().getCenario().getTipoTropas().values()) {
                    if (tipoTropa.isBarcos() == water) {
                        tropas.add(tipoTropa);
                    }
                }
                //sort array
                ComparatorFactory.getComparatorCasualtiesTipoTropaSorter(tropas, ConverterFactory.taticaToInt(tatica[1]), terreno, partida.getId());
                int counter = 1;
                String msg = "";
                for (TipoTropa tpTropa : tropas) {
                    msg += String.format("%s: %s\n", counter, tpTropa.getNome());
                    if (!tatica[1].equals("pa")) {
                        counter++;
                    }
                }
                getReport().writeCelula(msg);
            }
            getReport().writeTabelaFecha();
        }
    }

    private void printSkills() {
        final String[] rangeLabel = {"0", "1-9", "10-19", "20-29", "30-39", "40-49", "50-59", "60-69", "70-79", "80-89", "90-99", "100+"};
        SortedMap<Integer, String[]> skillsList = cenarioFacade.getTituloPericiaAll(partida.getCenario());
        getReport().write("\n");
        getReport().write("\n");
        getReport().lineSeparator();
        getReport().writeTitle(labels.getString("RULES.TITLE.SKILLS"));
        getReport().write("\n");
        final int borderBefore = getReport().getBorder();
        getReport().setBorder(1);
        getReport().writeTabela(skillsList.size() + 1);
        int maxSize = 0;
        getReport().writeCelula(" ");
        for (Integer skillClass : skillsList.keySet()) {
            //Class title, one table row
            getReport().writeCelulaBold(TitleFactory.getTipoPersonagem(skillClass));
            maxSize = Math.max(maxSize, skillsList.get(skillClass).length);
        }
        for (int ii = 0; ii < maxSize; ii++) {
            //list all skills, multiple rows in a table?
            getReport().writeTabelaLine();
            getReport().writeCelula(rangeLabel[ii]);
            for (Integer skillClass : skillsList.keySet()) {
                String[] skillTitles = skillsList.get(skillClass);
                getReport().writeCelula(skillTitles[ii]);
            }
        }
        getReport().writeTabelaFecha();
        getReport().setBorder(borderBefore);
    }

    private void printTaticasVsTaticasTable() {
        final String[][] taticas = cenarioFacade.listTaticas(partida.getCenario());
        getReport().writeTabela(taticas.length + 1);
        //title row
        getReport().writeCelulaBold("%");
        for (String[] tatica : taticas) {
            getReport().writeCelulaBold(tatica[0]);
        }
        getReport().writeTabelaLine();
        for (String[] taticaAtaque : taticas) {
            getReport().writeCelulaBold(taticaAtaque[0]);
            for (String[] taticaDefesa : taticas) {
                getReport().writeCelula(cenarioFacade.getTaticaBonus(partida.getCenario(), taticaAtaque[1], taticaDefesa[1]) + "");
            }
            getReport().writeTabelaLine();
        }
        getReport().writeTabelaFecha();
    }

    private void printTropasTable() throws PersistenceException {
        final List<TipoTropa> tropas = new ArrayList<TipoTropa>(getPartida().getCenario().getTipoTropas().values());
        //sort array
        ComparatorFactory.getComparatorBaseModelNameSorter(tropas);
        getReport().writeTabela(6);

        //titles
        getReport().writeTabelaLine();
        getReport().writeCelula(labels.getString("TROPA.NOME"));
        getReport().writeCelula(labels.getString("TROPA.RECRUIT.MONEY"));
        getReport().writeCelula(labels.getString("TROPA.UPKEEP.MONEY"));
        getReport().writeCelula(labels.getString("TROPA.UPKEEP.FOOD"));
        getReport().writeCelula(labels.getString("TROPA.MOVIMENTO"));
        getReport().writeCelula(labels.getString("TROPA.HABILIDADE"));

        //details
        for (TipoTropa tpTropa : tropas) {
            getReport().writeTabelaLine();
            getReport().writeCelula(tpTropa.getNome());
            getReport().writeCelula(tpTropa.getRecruitCostMoney() + "");
            getReport().writeCelula(tpTropa.getUpkeepMoney() + "");
            getReport().writeCelula(tpTropa.getUpkeepFood() + "");
            getReport().writeCelula(tpTropa.getMovimento() + "");
            String msg = "";
            for (Habilidade habilidade : tpTropa.getHabilidades().values()) {
                msg += habilidade.getNome() + "\n";
            }
            getReport().writeCelula(msg);
        }
        getReport().writeTabelaFecha();

    }
}
