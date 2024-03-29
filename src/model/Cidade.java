/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import business.interfaces.IActor;
import java.util.SortedMap;
import java.util.TreeMap;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class Cidade extends BaseModel implements IActor, Cloneable {

    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private Local local;
    private Nacao nacao;
    private Alianca alianca; //keep for backwards compatibility
    private int fortificacao;
    private int tamanho;
    private int docas;
    private boolean capital;
    private int lealdade;
    private int lealdadeAnterior;
    private int fonteInfo;
    private boolean oculto, sitiado;
    private final SortedMap<Produto, Integer> estoques = new TreeMap();
    private final SortedMap<String, TipoTropa> tpTropa = new TreeMap();
    private Raca raca;

    @Override
    public String getCodigo() {
        if (super.getCodigo() != null) {
            return super.getCodigo();
        } else {
            //transition from nome to cod on cities. eventualy, we may clen up
            return getNome();
        }
    }

    public String getCoordenadas() {
        try {
            return this.local.getCoordenadas();
        } catch (NullPointerException ex) {
            return "-";
        }
    }

    public boolean isFortificado() {
        return this.getFortificacao() > 0;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
        local.setCidade(this);
    }

    @Override
    public Nacao getNacao() {
        return nacao;
    }

    public void setNacao(Nacao nacao) {
        this.nacao = nacao;
        try {
            nacao.addCidade(this);
        } catch (NullPointerException e) {
        }
    }

    public int getFortificacao() {
        return fortificacao;
    }

    public void setFortificacao(int fortificacao) {
        this.fortificacao = fortificacao;
    }

    public int getTamanho() {
        return tamanho;
    }

    public void setTamanho(int tamanho) {
        this.tamanho = tamanho;
    }

    public int getDocas() {
        return docas;
    }

    public void setDocas(int docas) {
        this.docas = docas;
    }

    public boolean isCapital() {
        return capital;
    }

    public void setCapital(boolean capital) {
        this.capital = capital;
        if (capital) {
            nacao.setCapital(this);
        }
    }

    public int getLealdade() {
        return lealdade;
    }

    public void setLealdade(int lealdade) {
        this.lealdade = lealdade;
    }

    public int getFonteInfo() {
        return fonteInfo;
    }

    public void setFonteInfo(int fonteInfo) {
        this.fonteInfo = fonteInfo;
    }

    public boolean isOculto() {
        return oculto;
    }

    public void setOculto(boolean oculto) {
        this.oculto = oculto;
    }

    public SortedMap<Produto, Integer> getEstoques() {
        return estoques;
    }

    public int getEstoque(Produto produto) {
        try {
            return estoques.get(produto);
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    // private static final Log log = LogFactory.getLog(Cidade.class);
    public void setEstoque(Produto produto, int qtd) {
        //xxx: Investigate resources production
//        if (this.getLocal().getCodigo().equals("4217") && produto.getCodigo().equalsIgnoreCase("cr")) {
//            String format = String.format("0750: %s to %s", this.estoques.get(produto), qtd);
//        }
        this.estoques.put(produto, Math.max(0, qtd));
    }

    public SortedMap<String, TipoTropa> getTipoTropas() {
        return this.tpTropa;
    }

    public boolean isTipoTropa(String cdTipoTropa) {
        return this.tpTropa.containsKey(cdTipoTropa);
    }

    public void addTipoTropa(String cdTipoTropa, TipoTropa tpTropa) {
        this.tpTropa.put(cdTipoTropa, tpTropa);
    }

    public int getProducao(Produto produto) {
        try {
            int ret = this.getLocal().getProducaoClima(produto);
            if (produto.isMoney()) {
                return ret;
            }
            int[] sizeFactor = {0, 100, 80, 60, 40, 20};
            ret = ret * sizeFactor[this.getTamanho()] / 100;
            return ret;
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public SortedMap<String, Personagem> getPersonagens() {
        return getLocal().getPersonagens();
    }

    public void setSitiado(boolean sitiado) {
        this.sitiado = sitiado;
    }

    public boolean isSitiado() {
        return sitiado;
    }

    @Override
    public String getComboDisplay() {
        String ret = this.getNome();
        if (ret == null) {
            ret = labels.getString("DESCONHECIDA");
        }
        return String.format("%s (%s)", ret, this.getCoordenadas());
    }

    @Override
    public String getComboId() {
        return this.getCoordenadas();
    }

    /**
     * @return the lealdadeAnterior
     */
    public int getLealdadeAnterior() {
        return lealdadeAnterior;
    }

    /**
     * @param lealdadeAnterior the lealdadeAnterior to set
     */
    public void setLealdadeAnterior(int lealdadeAnterior) {
        this.lealdadeAnterior = lealdadeAnterior;
    }

//    @Override
//    public String getCodigo() {
//        return this.getNome();
//    }
    @Override
    public String toString() {
        return this.getNome() + "-" + this.getCoordenadas() + "-" + getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    /**
     * @return the raca
     */
    public Raca getRaca() {
        return raca;
    }

    /**
     * @param raca the raca to set
     */
    public void setRaca(Raca raca) {
        this.raca = raca;
    }

    @Override
    public int getOrdensQt() {
        return getTamanho();
    }

    @Override
    public String getTpActor() {
        return "C";
    }

    @Override
    public boolean isActorActive() {
        return this.getTamanho() > 0;
    }

    public int getDefenseBonus() {
        return 0;
    }

    public void setDefenseBonus(int bonusDefense) {
        //do nothing here. 
    }
    public void addDefenseBonus(int bonusDefense) {
        //do nothing here. 
    }

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public Cidade clone() {
        //attention: no deep copy implemented
        try {
            return (Cidade) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }
}
