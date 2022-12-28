/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import java.io.Serializable;
import java.util.ArrayList;
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
}
