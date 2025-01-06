/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import model.Cenario;
import model.Cidade;
import model.Exercito;
import model.ExtratoDetail;
import model.Habilidade;
import model.HabilidadeNacao;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Partida;
import model.Pelotao;
import model.Personagem;
import model.Produto;
import model.Raca;
import model.TipoTropa;
import msgs.BaseMsgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import utils.StringRet;

/**
 *
 * @author gurgel
 */
public class NacaoFacade implements Serializable {

    private static final Log log = LogFactory.getLog(NacaoFacade.class);
    public static final String MASK_CAMPING_RESTRICTION_HABILIDADE = ";FHC%s;";
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final CidadeFacade cidadeFacade = new CidadeFacade();
    //TODO: carregar do banco de dados. Cenario?
//    private static final int[][] podeLord = {
//        {0, 1, 0, 0, 0, 0, 0},
//        {1, 0, 2, 0, 0, 2, 2},
//        {0, 1, 0, 2, 0, 2, 2},
//        {0, 1, 0, 0, 2, 2, 2},
//        {0, 0, 1, 0, 0, 2, 2},
//        {1, 0, 0, 0, 0, 0, 0},
//        {0, 0, 1, 0, 0, 0, 0}
//    };
    private static final int[][] podeAny = {
        {0, 1, 0, 0, 0, 0, 0},
        {1, 0, 2, 0, 0, 2, 2},
        {0, 1, 0, 2, 0, 2, 2},
        {0, 1, 0, 0, 0, 2, 2},
        {0, 0, 1, 0, 0, 2, 2},
        {1, 0, 0, 0, 0, 0, 0},
        {0, 0, 1, 0, 0, 0, 0}
    };
    private static final int[][] podeVassal = {
        {0, 1, 0, 0, 0, 0, 0},
        {1, 0, 2, 0, 0, 0, 0},
        {0, 1, 0, 2, 0, 0, 0},
        {0, 1, 0, 0, 0, 0, 0},
        {0, 0, 1, 0, 0, 0, 0},
        {1, 0, 0, 0, 0, 0, 0},
        {0, 0, 1, 0, 0, 0, 0}
    };

    public static int[][] getPodeAny() {
        return podeAny;
    }

    public static int[][] getPodeVassal() {
        return podeVassal;
    }

    public int getArrecadacao(Nacao nacao) {
        int ret = 0;
        for (Cidade cidade : nacao.getCidades()) {
            if (nacao == cidade.getNacao()) {
                ret += cidadeFacade.getArrecadacaoImpostos(cidade);
            }
        }
        return ret;
    }

    public String getCoordenadasCapital(Nacao nacao) {
        try {
            return this.getLocal(nacao).getCoordenadas();
        } catch (NullPointerException ex) {
            return "-";
        }
    }

    public Local getLocal(Nacao nacao) {
        try {
            return this.getCapital(nacao).getLocal();
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public int getCustoCidades(Nacao nacao, Cenario cenario) {
        if (cenario.hasHabilidade(";SUC;")) {
            return 0;
        }
        int ret = 0;
        for (Cidade cidade : nacao.getCidades()) {
            if (nacao == cidade.getNacao() && cidade.getTamanho() > 0) {
                ret += cidade.getDocas() * 250;
                if (this.hasHabilidade(nacao, "0042") && cidade.getFortificacao() == 5) {
                    //The Wall: Fortress' upkeep is free
                } else if (this.hasHabilidade(nacao, ";PFG;") && cidade.getFortificacao() == 5) {
                    //The Wall: Fortress' upkeep is free
                    ret += 0;
                } else if (nacao.hasHabilidade(";PUF;")) {
                    //Pays half upkeep cost on fortifications
                    ret += cidade.getFortificacao() * 500 / nacao.getHabilidadeValor(";PUF;");
                } else if (this.hasHabilidade(nacao, "0037")) {
                    //Pays half upkeep cost on fortifications
                    ret += cidade.getFortificacao() * 250;
                } else {
                    ret += cidade.getFortificacao() * 500;
                }
            }
        }
        return ret;
    }

    public int getCustoExercitoNacao(Nacao nacao, Collection<Exercito> exercitos) {
        int ret = 0;
        ExercitoFacade ef = new ExercitoFacade();
        for (Exercito exercito : exercitos) {
            if (nacao == ef.getNacao(exercito)) {
                for (Pelotao pelotao : exercito.getPelotoes().values()) {
                    ret += pelotao.getQtd() * pelotao.getTipoTropa().getUpkeepMoney();
                }
            }
        }
        return ret;
    }

    /**
     * *
     * Keep in sync with NacaoControl
     */
    public int getDescontoExercitoNacao(Nacao nacao, Collection<Exercito> exercitos) {
        if (!nacao.hasHabilidadeNacao("0053") && !nacao.hasHabilidade(";NMF;")) {
            return 0;
        }
        ExercitoFacade ef = new ExercitoFacade();
        int discount = 0;
        int qtdFreeBlood = nacao.getHabilidadeNacaoValor("0053");
        int qtdFreeFast = nacao.getHabilidadeValor(";NMF;");
        for (Exercito exercito : exercitos) {
            if (nacao != ef.getNacao(exercito)) {
                continue;
            }
            for (Pelotao pelotao : ef.getPelotoes(exercito).values()) {
                if (qtdFreeBlood + qtdFreeFast <= 0) {
                    break;
                }
                if (qtdFreeBlood > 0 && pelotao.getTipoTropa().getCodigo().contains("cpblood") && nacao.hasHabilidadeNacao("0053")) {
                    final int qtd = Math.min(qtdFreeBlood, pelotao.getQtd());
                    discount += qtd * pelotao.getTipoTropa().getUpkeepMoney();
                    qtdFreeBlood -= qtd;
                    //only discount once
                    continue;
                }
                if (qtdFreeFast > 0 && pelotao.getTipoTropa().isFastMovement() && nacao.hasHabilidade(";NMF;")) {
                    final int qtd = Math.min(qtdFreeFast, pelotao.getQtd());
                    discount += qtd * pelotao.getTipoTropa().getUpkeepMoney();
                    qtdFreeFast -= qtd;
                }
            }
        }
        return discount;
    }

    public int getPersonagens(Nacao nacao) {
        return nacao.getPersonagens().size();
    }

    public int getPersonagensSlot(Nacao nacao, Cenario cenario) {
        float qtBaseChars = cenario.getNumMaxPersonagem();
        float qtMaxChars = qtBaseChars;
        float qtIncrease;
        qtIncrease = this.getHabilidadeValor(nacao, ";NMC;");
        if (qtIncrease != 0) {
            qtMaxChars += qtIncrease;
        }
        qtIncrease = this.getHabilidadeValor(nacao, ";NNX2;");
        if (qtIncrease != 0) {
            qtMaxChars = qtBaseChars * qtIncrease / 100f;
        }
        qtIncrease = this.getHabilidadeValor(nacao, ";NNX3;");
        if (qtIncrease != 0) {
            qtMaxChars = qtBaseChars * qtIncrease / 100f;
        }
        return (int) Math.max(qtMaxChars - getPersonagens(nacao), 0);
    }

    public int getCustoPersonagens(Nacao nacao, Cenario cenario) {
        if (cenario.hasHabilidade(";SUP;")) {
            return 0;
        }
        int ret = 0;
        for (Personagem pers : nacao.getPersonagens()) {
            if (nacao != pers.getNacao()) {
                continue;
            }
            int mod = 20;
            if (this.hasHabilidade(nacao, ";PUC;")) {
                //Free People: Character's upkeep cost %s%% less
                mod = 20 * (100 - nacao.getHabilidadeValor(";PUC;")) / 100;
            } else if (this.hasHabilidade(nacao, ";PUC5;")) {
                //Free People: Character's upkeep cost %s%% less
                mod = 20 * (100 - nacao.getHabilidadeValor(";PUC5;")) / 100;
            } else if (this.hasHabilidade(nacao, "0043")) {
                //Free People: Character's upkeep cost %s%% less
                mod = 20 * (100 - nacao.getHabilidadeNacaoValor("0043")) / 100;
            }
            ret += pers.getPericiaNaturalTotal() * mod;
        }
        return ret;
    }

    public int getEstoque(Nacao nacao, Produto produto) {
        int ret = 0;
        for (Cidade cidade : nacao.getCidades()) {
            ret += cidadeFacade.getEstoque(cidade, produto);
        }
        return ret;
    }

    public Object getImpostos(Nacao nacao) {
        try {
            return nacao.getImpostos();
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public Object getTeamFlag(Nacao nacao) {
        try {
            return nacao.getTeamFlag();
        } catch (NullPointerException ex) {
            return labels.getString("DESCONHECIDA");
        }
    }

    public String getJogadorDisplay(Nacao nacao) {
        try {
            return nacao.getOwner().getNome();
        } catch (NullPointerException ex) {
            return labels.getString("DESCONHECIDA");
        }
    }

    public String getJogadorEmail(Nacao nacao) {
        try {
            return nacao.getOwner().getEmail();
        } catch (NullPointerException ex) {
            return labels.getString("DESCONHECIDA");
        }
    }

    public int getMoneySaldo(Nacao nacao) {
        try {
            return nacao.getMoney();
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public String getNome(Nacao nacao) {
        try {
            return nacao.getNome();
        } catch (NullPointerException ex) {
            return labels.getString("DESCONHECIDA");
        }
    }

    public String getNomeDbClean(Nacao nation) {
        return nation.getNomeDbClean();
    }

    public Jogador getOwner(Nacao nacao) {
        try {
            return nacao.getOwner();
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public int getProducao(Nacao nacao, Produto produto, Cenario cenario, int turno) {
        int ret = 0;
        for (Cidade cidade : nacao.getCidades()) {
            ret += cidadeFacade.getProducao(cidade, produto, cenario, turno);
        }
        return ret;
    }

    public String getRacaDisplay(Nacao nacao) {
        try {
            return nacao.getRaca().getDisplay();
        } catch (NullPointerException ex) {
            return labels.getString("DESCONHECIDA");
        }
    }

    public String getRacaNome(Nacao nacao) {
        try {
            return nacao.getRaca().getNome();
        } catch (NullPointerException ex) {
            return labels.getString("DESCONHECIDA");
        }
    }

    public int getTropasQt(Nacao nation, Collection<Exercito> armies) {
        int ret = 0;
        try {
            for (Exercito army : armies) {
                if (nation == army.getNacao()) {
                    ret += army.getTropasTotal();
                }
            }
        } catch (NullPointerException ex) {
            ret = 0;
        }
        return ret;
    }

    public String getRelacionamento(Nacao nacaoJogador, Nacao nacaoAlvo) {
        return BaseMsgs.nacaoRelacionamento[nacaoJogador.getRelacionamento(nacaoAlvo) + 2];
    }

    public SortedMap<Nacao, Integer> getRelacionamentos(Nacao nacao) {
        return nacao.getRelacionamentos();
    }

    public boolean hasHabilidade(Nacao nacao, String cdHabilidade) {
        if (cdHabilidade.contains(";")) {
            return (nacao.hasHabilidade(cdHabilidade));
        } else {
            return (nacao.getHabilidadesNacao().containsKey(cdHabilidade));
        }
    }

    public int getHabilidadeValor(Nacao nacao, String cdHabilidade) {
        if (cdHabilidade.contains(";")) {
            return (nacao.getHabilidadeValor(cdHabilidade));
        } else {
            return (nacao.getHabilidadeNacaoValor(cdHabilidade));
        }
    }

    public int getPointVictory(Nacao nacao) {
        return nacao.getPontosVitoria();
    }

    public Iterable<String> getMensagensAll(Nacao nacao) {
        List<String> ret = new ArrayList<>();
        for (String chave : nacao.getMensagensNacao().keySet()) {
            try {
                List<String> listMsg = (List<String>) nacao.getMensagensNacao().get(chave);
                for (String msg : listMsg) {
                    ret.add(msg);
                }
            } catch (Exception ex) {
            }
        }
        return ret;
    }

    public Iterable<String> getMensagensCombatesDuelos(Nacao nacao) {
        String[] keys = {"Duelos", "Combates"};
        List<String> ret = new ArrayList<>();
        for (String chave : keys) {
            try {
                List<String> listMsg = (List<String>) nacao.getMensagensNacao().get(chave);
                for (String msg : listMsg) {
                    ret.add(msg);
                }
            } catch (NullPointerException ex) {
            }
        }
        return ret;
    }

    public Iterable<String> getMensagensResultsRumoresEncontros(Nacao nacao) {
        String[] keys = {"Results", "Rumores", "Encontros"};
        List<String> ret = new ArrayList<>();
        for (String chave : keys) {
            try {
                List<String> listMsg = (List<String>) nacao.getMensagensNacao().get(chave);
                for (String msg : listMsg) {
                    ret.add(msg);
                }
            } catch (NullPointerException ex) {
            }
        }
        return ret;
    }

    public String getCodigo(Nacao nacao) {
        return nacao.getCodigo();
    }

    public Iterable<HabilidadeNacao> getHabilidadesNacao(Nacao nacao) {
        return nacao.getHabilidadesNacao().values();
    }

    public Collection<Habilidade> getHabilidades(Nacao nacao) {
        return nacao.getHabilidades().values();
    }

    public int getMoney(Nacao nacao) {
        return nacao.getMoney();
    }

    public Iterable<ExtratoDetail> getExtratoDetail(Nacao nacao) {
        return nacao.getExtrato().getExtratoList();
    }

    /**
     * filtra os valores possiveis para as duas nacoes.
     *
     * @param nacaoBase
     * @param nacaoAlvo
     * @return
     */
    public String[][] listRelacionamentosTipo(Nacao nacaoBase, Nacao nacaoAlvo) {
        final String[][] nacaoRelacionamentoCombo = {
            {labels.getString("RELACIONAMENTO.ACTION.ODIADO"), "-2"},
            {labels.getString("RELACIONAMENTO.ACTION.ANTIPATIZANTE"), "-1"},
            {labels.getString("RELACIONAMENTO.ACTION.NEUTRO"), "0"},
            {labels.getString("RELACIONAMENTO.ACTION.TOLERANTE"), "1"},
            {labels.getString("RELACIONAMENTO.ACTION.AMIGAVEL"), "2"},
            {labels.getString("RELACIONAMENTO.ACTION.VASALO"), "3"},
            {labels.getString("RELACIONAMENTO.ACTION.LORDE"), "4"}};
        final String[] rebel = {labels.getString("RELACIONAMENTO.ACTION.REBELA"), "-2"};
        final String[] release = {labels.getString("RELACIONAMENTO.ACTION.LIBERA"), "0"};
        int[][] pode;
        if (this.isVassal(nacaoBase)) {
            pode = getPodeVassal();
        } else {
            pode = getPodeAny();
        }
        int relacionamento = nacaoBase.getRelacionamento(nacaoAlvo);
        List<String[]> ret = new ArrayList<>();
        switch (relacionamento) {
            case 3:
                //se vassalo, rebela
                ret.add(rebel);
                break;
            case 4:
                //se lord, libera
                ret.add(release);
                break;
            default:
                for (int ii = 0; ii < pode[relacionamento + 2].length; ii++) {
                    int tipo = pode[relacionamento + 2][ii];
                    if (tipo > 0) {
                        ret.add(nacaoRelacionamentoCombo[ii]);
                    }
                }
                break;
        }
        return ret.toArray(new String[0][0]);
    }

    public SortedMap<TipoTropa, Integer> getTropas(Nacao nacao) {
        return nacao.getRaca().getTropas();
    }

    private boolean isVassal(Nacao nacao) {
        for (Integer rel : nacao.getRelacionamentos().values()) {
            if (rel == 3) {
                return true;
            }
        }
        return false;
    }

    /*
     Nation considers all Nations allied (o self)
     */
    public boolean isAliado(Nacao nation, Collection<Nacao> nations) {
        if (nations.isEmpty()) {
            return false;
        }
        for (Nacao nationTarget : nations) {
            if (!isAliado(nation, nationTarget)) {
                return false;
            }
        }
        return true;
    }

    public boolean isAliado(Nacao nacao, Nacao nacaoAlvo) {
        return (nacao == nacaoAlvo || nacao.getRelacionamento(nacaoAlvo) >= 2);
    }

    public boolean isEnemySworn(Nacao nacao, Nacao nacaoAlvo) {
        return (nacao.getRelacionamento(nacaoAlvo) <= -2);
    }

    public boolean isAtiva(Nacao nacao) {
        return nacao.isAtiva();
    }

    public boolean isAtivaPC(Nacao nacao) {
        try {
            return nacao.isAtiva() && nacao.getOwner().getId() != 1;
        } catch (Exception e) {
            return false;
        }
    }

    public Personagem[] listPersonagemNaoNacao(Nacao nacao, Collection<Personagem> listPersonagens) {
        List<Personagem> lista = new ArrayList();
        lista.addAll(listPersonagens);
        lista.removeAll(nacao.getPersonagens());
        return (Personagem[]) lista.toArray(new Personagem[0]);
    }

    public Personagem[] listPersonagemNacao(Nacao nacao, Personagem personagem) {
        List<Personagem> lista = new ArrayList();
        lista.addAll(nacao.getPersonagens());
        lista.remove(personagem);
        return (Personagem[]) lista.toArray(new Personagem[0]);
    }

    public Raca getRaca(Nacao nacao) {
        return nacao.getRaca();
    }

    public Cidade getCapital(Nacao nacao) {
        //keep in sync with NacaoControl.getCidadeMain();
        if (nacao.getCapital() != null) {
            //if has capital, then return capital
            return nacao.getCapital();
        }
        //else, select main city
        int max = 0;
        int current;
        List<Cidade> potential = new ArrayList<>(nacao.getCidades().size());
        for (Cidade cidade : nacao.getCidades()) {
            current = cidade.getTamanho() * 1000000 + cidade.getFortificacao() * 1000 + cidade.getLealdade();
            if (current > max) {
                //clear list and add potential
                max = current;
                potential.clear();
                potential.add(cidade);
            } else if (max == current) {
                //add to potential list
                potential.add(cidade);
            }
        }
        //select one of the potential
        if (potential.isEmpty()) {
            return null;
        } else {
            Collections.shuffle(potential);
            return potential.get(0);
        }
    }

    public List<String> getInfoTitle(Nacao nacao) {
        StringRet ret = new StringRet();
        if (nacao == null) {
            return ret.getList();
        }
        ret.add(String.format(labels.getString("STARTUP.NATION.TITLE"), getNome(nacao.getNacao())));
        return ret.getList();
    }

    public int getLealdade(Nacao nacao) {
        if (nacao.getCidades().isEmpty()) {
            return 0;
        }
        int ret = 0, size = 0;
        for (Cidade cidade : nacao.getCidades()) {
            ret += cidade.getLealdade() * cidade.getTamanho();
            size += cidade.getTamanho();
        }
        if (size == 0) {
            return 0;
        }
        return ret / size;
    }

    public int getLealdadeAnterior(Nacao nacao) {
        if (nacao.getCidades().isEmpty()) {
            return 0;
        }
        int ret = 0, size = 0;
        for (Cidade cidade : nacao.getCidades()) {
            ret += cidade.getLealdadeAnterior() * cidade.getTamanho();
            size += cidade.getTamanho();
        }
        if (size == 0) {
            return 0;
        }
        return ret / size;
    }

    public int getGoldDecay(Nacao nacao, Cenario cenario) {
        return getGoldDecay(nacao, nacao.getMoney(), cenario);
    }

    public int getGoldDecay(Nacao nacao, int treasuryForecast, Cenario cenario) {
        if (cenario.hasHabilidade(";SGD;")) {
            if (nacao.hasHabilidade(";NGD;")) {
                return treasuryForecast * cenario.getHabilidadeValor(";SGD;") * (100 - nacao.getHabilidadeValor(";NGD;")) / 10000;
            } else {
                return treasuryForecast * cenario.getHabilidadeValor(";SGD;") / 100;
            }
        } else {
            return 0;
        }
    }

    public int getBonusRelacionamento(Nacao nationBase, Nacao nationTarget) {
        return BaseMsgs.dificuldadeBonus[nationBase.getRelacionamento(nationTarget) + 3];
    }

    public boolean isNacaoBarbarian(Nacao nation) {
        //FIXME: look for hability to indicate
        try {
            return nation.getOwner().getId() == 1 && nation.getNome().equals("Barbarians");
        } catch (NullPointerException e) {
            //Jogador not loaded. Not to bleed into other players files like DM/GB
            return false;
        }
    }

    public boolean isNpc(Personagem personagem) {
        //FIXME: look for hability to indicate
        try {
            return personagem.getNacao().getOwner().getId() == 1 && personagem.getNacao().getNome().equals("Barbarians");
        } catch (NullPointerException e) {
            //Jogador not loaded. Not to bleed into other players files like DM/GB
            return false;
        }
    }

    public int getCidadeRecruitingLimit(Nacao nation, String nome, Cenario cenario) {
        if (cenario.isWdo()) {
            //assume 100 for WDO
            return 100;
        }
        return getCidadeTamanho(nation, nome) * 100;
    }

    public int getCidadeTamanho(Nacao nation, String nome) {
        for (Cidade city : nation.getCidades()) {
            if (city.getNome().equals(nome)) {
                return city.getTamanho();
            }
        }
        for (Personagem pers : nation.getPersonagens()) {
            if (pers.getNome().equals(nome)) {
                try {
                    return pers.getLocal().getCidade().getTamanho();
                } catch (NullPointerException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    public Cidade getCidade(Nacao nation, String nome) {
        for (Cidade city : nation.getCidades()) {
            if (city.getNome().equals(nome)) {
                return city;
            }
        }
        for (Personagem pers : nation.getPersonagens()) {
            if (!pers.getNome().equals(nome)) {
                //not this one
                continue;
            }
            try {
                return pers.getLocal().getCidade();
            } catch (NullPointerException e) {
                //trouble in paradise; how can we find the city?
                return null;
            }
        }
        //trouble in paradise; how can we find the city?
        return null;
    }

    public int getCidadeFortificacaoCusto(Cidade city) {
        //TODO: add nation powers for discounts and stuff
        final int[] custo = {1000, 3000, 5000, 8000, 12000};
        int ret;
        try {
            ret = custo[city.getFortificacao()];
        } catch (IndexOutOfBoundsException ex) {
            ret = -1;
        }
        if (city.getNacao().hasHabilidade(";NFG;")) {
            ret = ret * city.getNacao().getHabilidadeValor(";NFG;") / 100;
        }
        return ret;
    }

    public Integer getPointsDomination(Nacao nation) {
        int ret = 0;
        for (Cidade city : nation.getCidades()) {
            ret += cidadeFacade.getPointsDomination(city);
        }
        return ret;
    }

    public Integer getPointsKeyCity(Nacao nation) {
        int ret = 0;
        for (Cidade city : nation.getCidades()) {
            ret += cidadeFacade.getPointsKeyCity(city);
        }
        return ret;
    }

    public int getNacaoNumero(Nacao nation) {
        return Integer.valueOf(nation.getCodigo());
    }

    public boolean isHexagonoRestrictedToCamping(Nacao nation, Local local) {
        final String cdHabilidade = String.format(NacaoFacade.MASK_CAMPING_RESTRICTION_HABILIDADE, this.getNomeDbClean(nation));
        for (Habilidade restriction : local.getHabilidades().values()) {
            if (restriction.getCodigo().equals(cdHabilidade)) {
                //if restriction only affects self, it is allowed
                return false;
            }
        }
        //then no restrictions. public area.
        return true;
    }

    public final List<String> getCampingRestrictionsHexagonoList(Nacao nation, Partida game) {
        final List<String> allowedHexes = new ArrayList<>();
        if (!game.hasHabilidade(";GPC;") || game.getTurno() > game.getHabilidadeValor(";GPC;")) {
            //no restrictions, return empty list
            return allowedHexes;
        }

        //need to load allowedHexes here.
        switch (this.getNomeDbClean(nation)) {
            case "WILDLINGS":
                final String[] WILDLINGS
                        = {"0101", "0201", "0301", "0401", "0501", "0601", "0701", "0801", "0901", "1001", "1201", "1301",
                            "1401", "0102", "0202", "0302", "0402", "0502", "0602", "0802", "0902", "1002", "1102", "1202", "1302", "1402", "0103",
                            "0203", "0303", "0403", "0503", "0603", "0703", "0803", "0903", "1003", "1103", "1303", "1403", "0104", "0204", "0304",
                            "0404", "0504", "0604", "0704", "0804", "0904", "1004", "1104", "1204", "1304", "0205", "0305", "0405", "0505", "0605",
                            "0705", "0805", "0905", "1005", "1105", "1205", "1305", "1405", "0306", "0406", "0506", "0606", "0806", "0906", "1006",
                            "1106", "1206", "1306", "0307", "0407", "0507", "0607", "0707", "0807", "0907", "1107", "1207", "1307", "1407", "1507",
                            "1607", "0308", "0408", "0508", "0608", "0708", "0808", "0908", "1008", "1108", "1208", "1308", "1408", "1508", "1608",
                            "1708", "0309", "0409", "0509", "0609", "0709", "0809", "0909", "1009", "1109", "1209", "1309", "1409", "1509", "0310",
                            "0410", "0510", "0610", "0710", "0810", "0910", "1010", "1110", "1210", "1310", "1410", "2010", "2210", "0511", "0611",
                            "0711", "0811", "0911", "1311", "2411", "2611", "0612", "0712", "0812", "1112", "1212", "2212", "2412", "2612", "0713",
                            "0813", "2313", "2413", "2513", "2314", "2414", "0418", "0519", "0619", "0520", "0620", "0621", "0324", "0226", "0227"
                        };
                allowedHexes.addAll(Arrays.asList(WILDLINGS));
                break;
            case "NIGHTWATCH":
                final String[] NIGHTWATCH
                        = {"1413", "1513", "1613", "1813", "1913", "2013", "1314", "1414", "1514", "1614", "1512", "1612", "1812",
                            "1814", "1914", "2014", "1215", "1315", "1415", "1515", "1615", "1715", "1915", "2015", "1216", "1316", "1416", "1516",
                            "1616", "2016", "1217", "1317", "1417", "1517", "2117", "2217", "1118", "1218", "1318", "1418", "1718", "2018", "2118",
                            "1019", "1119", "1219", "1319", "1419", "1719", "1819", "1919", "2019", "2119", "1020", "1520", "1620", "1720", "2120"
                        };
                allowedHexes.addAll(Arrays.asList(NIGHTWATCH));
                break;
            case "STARK":
                final String[] STARK
                        = {"0815", "1215", "1315", "1415", "1515",
                            "2015", "0716", "1216", "1316", "1416", "1516", "1616", "1816", "1916", "2016", "1217", "1317", "1417", "1517", "1817",
                            "2017", "2117", "2217", "2317", "2417", "2517", "0418", "1118", "1218", "1318", "1418", "1718", "1818", "1918", "2018",
                            "2118", "2218", "2318", "2418", "2518", "0519", "0619", "0919", "1019", "1119", "1219", "1319", "1419", "1719", "1819",
                            "1919", "2019", "2119", "2219", "2319", "2519", "0520", "0620", "0720", "0920", "1020", "1120", "1220", "1320", "1520",
                            "1620", "1720", "1820", "1920", "2020", "2120", "2220", "2320", "2420", "0621", "0721", "0821", "0921", "1021", "1121",
                            "1221", "1321", "1521", "1621", "1721", "1821", "1921", "2021", "2121", "2221", "2321", "2521", "0522", "0622", "0722",
                            "0822", "0922", "1022", "1122", "1222", "1422", "1522", "1622", "1722", "1822", "2022", "2122", "2222", "2422", "0523",
                            "0623", "0723", "0823", "0923", "1023", "1123", "1223", "1423", "1523", "1623", "1723", "1823", "1923", "2023", "2123",
                            "2223", "0324", "0524", "0624", "0724", "0824", "0924", "1024", "1124", "1324", "1424", "1524", "1624", "1724", "1824",
                            "1924", "2024", "0325", "0425", "0525", "0625", "0725", "0825", "0925", "1025", "1125", "1225", "1425", "1525", "1625",
                            "1725", "1825", "1925", "2025", "2125", "2225", "0226", "0326", "0426", "0526", "0626", "0726", "0826", "1016", "1126",
                            "1226", "1426", "1526", "1626", "1726", "1826", "1926", "2026", "2126", "0227", "0327", "0427", "0427", "0627", "0727",
                            "0827", "0927", "1027", "1127", "1227", "1327", "1527", "1627", "1727", "1827", "1927", "2027", "2127", "2227", "0228",
                            "0328", "0428", "0528", "0628", "0728", "0828", "0928", "1028", "1128", "1228", "1328", "1528", "1628", "1728", "1828",
                            "1928", "2128", "2228", "0429", "0529", "0629", "0729", "0829", "1029", "1129", "1229", "1329", "1429", "1629", "1729",
                            "1829", "1929", "2029", "2229", "2329", "0330", "0430", "0530", "0630", "0730", "0830", "0930", "1030", "1130", "1230",
                            "1330", "1630", "1730", "1830", "1930", "2030", "0431", "0531", "0631", "0731", "0831", "1031", "1131", "1231", "1331",
                            "1531", "1731", "1831", "1931", "2031", "0432", "0532", "0932", "1032", "1132", "1232", "1432", "1632", "1732", "1832",
                            "1932", "0733", "0833", "0933", "1033", "1133", "1233", "1333", "0334", "0634", "0734", "0834", "0934", "1034", "1134",
                            "1234", "1434", "2434", "0335", "0435", "0535", "0635", "0735", "0835", "0935", "1035", "1035", "1235", "1335", "1735",
                            "0236", "0336", "0436", "0536", "0636", "0736", "0836", "0936", "1036", "1136", "1236", "1836", "1936", "2036", "2436"
                        };
                allowedHexes.addAll(Arrays.asList(STARK));
                break;
            case "GREYJOY":
                final String[] GREYJOY
                        = {"0418", "0519", "0619", "1019", "1119", "0520", "0620", "1020", "0621",
                            "1021", "0522", "0622", "0722", "0822", "0922", "0523", "0623", "0723", "0823", "0923", "0324", "0524", "0624", "0724",
                            "0824", "0924", "0325", "0425", "0525", "0625", "0725", "0825", "0925", "1025", "0226", "0326", "0426", "0526", "0626",
                            "0726", "0826", "1026", "1126", "0227", "0327", "0427", "0527", "0627", "0727", "1127", "0228", "0328", "0428", "0528",
                            "0628", "1128", "0429", "0529", "0629", "0330", "0430", "0530", "0630", "1130", "0431", "0531", "0631", "0731", "1131",
                            "0432", "0532", "0932", "1032", "1132", "0733", "0833", "0933", "1033", "1133", "0334", "0734", "0834", "0934", "1034",
                            "1134", "0335", "0435", "0735", "0835", "0935", "1035", "1135", "0236", "0336", "0436", "0536", "0636", "0736", "0836",
                            "0936", "1036", "1136", "0337", "0837", "0937", "1037", "1137", "0938", "0941", "0721", "3046", "3151", "3152", "3153",
                            "3054", "3254", "3354", "3155", "3255", "3455", "3058", "3158", "3258", "3159", "3359", "3060", "3160", "3360", "3460",
                            "0561", "3261", "3162", "0363", "3163", "3364", "3464", "3665", "2966", "3167", "2868", "0270", "0371", "0272", "0372"
                        };
                allowedHexes.addAll(Arrays.asList(GREYJOY));
                break;
            case "TULLY":
                final String[] TULLY
                        = {"0932", "1032", "1132", "0933", "1033", "1133", "1233",
                            "0934", "1034", "1134", "1234", "1434", "0935", "1035", "1135", "1235", "1335", "0936", "1036", "1136", "1236", "0937",
                            "1037", "0938", "1039", "1439", "0940", "1040", "1140", "1240", "1440", "0941", "1241", "1341", "1541", "1641", "1242",
                            "1342", "1542", "1642", "1043", "1243", "1343", "1443", "1643", "1743", "0944", "1044", "1144", "1444", "1644", "1744",
                            "0945", "1045", "1145", "1545", "0946", "1046", "1246", "1346", "0947", "1247", "1347", "1447", "1148", "1248", "1448",
                            "1249", "1449", "1549", "1150", "1350", "1450", "1550", "1151", "1251", "1351", "1451", "1551", "1052", "1152", "1252"
                        };
                allowedHexes.addAll(Arrays.asList(TULLY));
                break;
            case "ARRYN":
                final String[] ARRYN
                        = {"2229", "1830", "1831", "1931", "2031", "1932", "1434", "2434", "1335", "1735",
                            "2335", "1236", "1836", "1936", "2036", "2136", "2236", "2336", "2436", "1637", "1737", "1837", "1937", "2037", "2137",
                            "2237", "1438", "1538", "1638", "1738", "1838", "1938", "2038", "2138", "1539", "1739", "1839", "1939", "2039", "2239",
                            "2339", "2439", "1440", "1540", "1640", "1740", "1840", "1940", "2140", "2240", "2340", "2440", "1541", "1641", "1741",
                            "1841", "1941", "2041", "2141", "2241", "2341", "3841", "1542", "1642", "1742", "1842", "1942", "2042", "2142", "2342",
                            "2542", "1643", "1743", "1843", "2043", "2143", "2243", "2343", "2443", "1644", "1744", "1944", "2144", "2244",
                            "2345", "2135", "3046", "3151", "3152", "3153", "3054", "3254", "3354", "3155", "3255", "3455", "3058", "3158", "3258",
                            "3159", "3359", "3060", "3160", "3360", "3460", "3261", "3162", "3163", "3364", "3464", "3665", "2966", "3167", "2868"
                        };
                allowedHexes.addAll(Arrays.asList(ARRYN));
                break;
            case "KINGSCOURT":
                final String[] KINGSCOURT
                        = {"1541", "1641", "3841", "1542", "1642", "1643", "1743", "1644",
                            "1945", "2245", "2345", "2545", "1744", "1746", "1946", "2046", "3046", "1547", "1847", "2347", "2447", "1648", "1848",
                            "2048", "2148", "2248", "1549", "1949", "2149", "1550", "1650", "1950", "1551", "1651", "1751", "1951", "3151", "1052",
                            "3152", "2244", "2353", "3153", "1054", "1154", "1254", "1754", "1954", "2054", "2154", "2254", "3054", "3254", "3354",
                            "1055", "1155", "1255", "2155", "2455", "3155", "3255", "3455", "0756", "0856", "0956", "1056", "1156", "1856", "1956",
                            "0557", "1057", "1157", "1757", "1857", "1957", "1558", "1758", "1858", "3058", "3158", "3258", "3159", "3359",
                            "3060", "3160", "3360", "3460", "0561", "1861", "3261", "1762", "3162", "0363", "1863", "3163", "3364", "3464", "3665",
                            "0866", "2966", "3167", "1168", "2868", "1369", "0270", "0770", "1070", "0371", "0971", "1071", "0272", "0372", "0972"
                        };
                allowedHexes.addAll(Arrays.asList(KINGSCOURT));
                break;
            case "LANNISTER":
                final String[] LANNISTER
                        = {"0745", "0845", "0646", "0647", "0747", "0847", "0548", "0648",
                            "0849", "0949", "1549", "0450", "0650", "0750", "0850", "1550", "1051", "1551", "0552", "0652", "0752", "0852", "1052",
                            "0553", "0653", "0753", "0853", "0554", "0654", "0754", "0954", "1054", "1154", "1254", "2154", "2254", "0455", "0555",
                            "0755", "0855", "0955", "1055", "1155", "1255", "2155", "2455", "0456", "0556", "0656", "0756", "0856", "0956", "1056",
                            "1156", "0557", "0657", "0757", "1057", "1157", "1757", "1857", "1957", "1558", "1758", "1858", "0249", "0561", "1861",
                            "1762", "0363", "1863", "0866", "1168", "1369", "0270", "0770", "1070", "0371", "0971", "1071", "0272", "0372", "0972"
                        };
                allowedHexes.addAll(Arrays.asList(LANNISTER));
                break;
            case "BARATHEON":
                final String[] BARATHEON
                        = {"3145", "3046", "1549", "1550", "2162",
                            "3151", "1052", "3152", "3153", "1054", "1154", "1254", "2154", "2254", "3054", "3254", "3354", "1055", "1155", "1255",
                            "2155", "2255", "2355", "2455", "3155", "3255", "3455", "0756", "0856", "0956", "1056", "1156", "2356", "0557", "1057",
                            "1157", "1757", "1857", "1957", "2057", "2157", "2557", "1558", "1758", "1858", "1958", "3058", "3158", "3258", "1659",
                            "1959", "2059", "2159", "2559", "1760", "1960", "3060", "3160", "3360", "3460", "0561", "1761", "1861", "2061", "2361",
                            "2461", "3261", "1762", "1862", "2062", "1551", "3162", "0363", "1863", "2363", "3163", "2364", "3364", "3464", "3665",
                            "0866", "2966", "3167", "1168", "2868", "1369", "0270", "0770", "1070", "0371", "0971", "1071", "0272", "0372", "0972"
                        };
                allowedHexes.addAll(Arrays.asList(BARATHEON));
                break;
            case "TYRELL":
                final String[] TYRELL
                        = {"1549", "1550", "1551", "1052", "1453", "1054", "1154", "1254", "1354", "1454", "2154", "2254", "1055", "1155", "1255",
                            "1355", "1455", "2155", "2455", "0756", "0856", "0956", "1056", "1156", "1256", "1356", "1456", "0557", "0857", "0957",
                            "1057", "1157", "1757", "1857", "1957", "0958", "1258", "1358", "1458", "1558", "1758", "1858", "0859", "0959", "1159",
                            "1359", "1559", "0760", "0860", "1060", "1260", "1460", "0561", "0661", "1061", "1161", "1261", "1361", "1461",
                            "1861", "0562", "0662", "0762", "0962", "1062", "1161", "1162", "1662", "1762", "0363", "0463", "0563", "0663", "0763",
                            "0963", "1063", "1553", "1563", "1863", "0364", "0564", "0964", "0465", "0765", "0865", "0965", "1065", "0266", "0366",
                            "0466", "0766", "0866", "0367", "0467", "0667", "0767", "0867", "0568", "0668", "0768", "1168", "0569", "0669", "0769",
                            "1369", "0270", "0570", "0670", "0770", "1070", "0371", "0571", "0771", "0971", "1071", "0272", "0372", "0972"
                        };
                allowedHexes.addAll(Arrays.asList(TYRELL));
                break;
            case "MARTELL":
                final String[] MARTELL
                        = {"3046", "1549", "1550", "1551", "3151", "1052", "3152",
                            "1054", "1154", "1254", "2154", "2254", "3054", "3254", "3354", "1055", "1155", "1255", "2155", "2455", "3155", "3255",
                            "3455", "0756", "0856", "0956", "1056", "1156", "0557", "1057", "1157", "1757", "1857", "1957", "1558", "1758", "1858",
                            "3058", "3158", "3258", "3060", "3160", "3360", "3460", "0561", "1861", "3261", "1762", "3162", "0363", "1763",
                            "0971", "1863", "3163", "3153", "1464", "1564", "3364", "3464", "1165", "1465", "1565", "3665", "0866", "0966", "1066",
                            "1266", "1366", "1466", "2966", "0967", "1167", "1467", "1567", "1867", "2667", "3167", "0868", "1068", "1168", "1368",
                            "1468", "1568", "1668", "1768", "1868", "1968", "2068", "2168", "2568", "2868", "0869", "1369", "1469", "1569", "1669",
                            "1769", "1869", "1969", "2069", "2169", "2269", "2369", "2469", "2569", "0270", "0770", "0970", "1070", "1170", "1370",
                            "1570", "1670", "1770", "1970", "2170", "2270", "0371", "1971", "1071", "1171", "1371", "1471", "1571", "1671", "1771",
                            "1871", "1971", "2171", "2271", "0272", "0372", "0972", "1072", "1172", "1272", "1372", "1472", "1572", "1672", "1772"
                        };
                allowedHexes.addAll(Arrays.asList(MARTELL));
                break;
            case "FREECITIES":
                final String[] FREECITIES
                        = {"2314", "2015", "2016", "2117", "2422", "2123", "3552",
                            "2223", "2024", "2125", "2225", "2031", "1832", "1932", "1434", "2434", "1735", "2436", "3640", "3341", "3142", "3342",
                            "3442", "3243", "3144", "3244", "3444", "3145", "3245", "3345", "3645", "3046", "3146", "3246", "3446", "3546", "3646",
                            "3147", "3247", "3447", "3547", "3647", "3747", "3348", "3448", "3548", "3648", "3748", "3049", "3449", "3549", "3649",
                            "3749", "3050", "3250", "3450", "3550", "3650", "3151", "3251", "3351", "3551", "3651", "3751", "3152", "3252", "3352",
                            "3752", "3153", "3253", "3353", "3453", "3653", "3853", "3054", "3154", "3254", "3354", "3554", "3754", "3854", "2254",
                            "3155", "3255", "3455", "3755", "3855", "2455", "3656", "3756", "3856", "3757", "3857", "3058", "3158", "3258", "3758",
                            "3159", "3259", "3359", "3759", "3060", "3160", "3260", "3360", "3460", "3560", "3261", "3361", "3461", "3561", "3162",
                            "3262", "3362", "3562", "3662", "2963", "3163", "3363", "3463", "3563", "3663", "3763", "3364", "3464", "3564", "3664",
                            "3764", "3665", "3765", "2966", "3766", "3866", "3167", "3867", "2868", "0270", "0371", "0272", "0372", "3172", "3272"
                        };
                allowedHexes.addAll(Arrays.asList(FREECITIES));
                break;
            case "TARGARYEN":
                final String[] TARGARYEN
                        = {"4239", "3940", "4140", "3841", "3941", "4141", "4241", "3742", "3842",
                            "4042", "4142", "3943", "4043", "4143", "4243", "3944", "4044", "4144", "4045", "3946", "3947", "3848", "3948", "3849",
                            "3949", "4049", "3750", "3850", "3950", "4050", "4150", "3851", "3951", "4051", "4151", "4251", "3852", "3952", "4052",
                            "4152", "3953", "4053", "4153", "4253", "3954", "4054", "4154", "3955", "4055", "4155", "4255", "3956", "4056", "4156",
                            "3957", "4057", "4157", "4257", "3848", "3948", "4058", "4158", "3859", "3959", "4059", "4159", "4259", "3860", "3960",
                            "4060", "4160", "3961", "4061", "4161", "4261", "3862", "3962", "4062", "4162", "3963", "4063", "4163", "4263", "4164"
                        };
                allowedHexes.addAll(Arrays.asList(TARGARYEN));
                break;
        }
        return allowedHexes;
    }
}
