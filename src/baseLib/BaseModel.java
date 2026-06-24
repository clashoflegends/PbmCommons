/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package baseLib;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Habilidade;
import model.Nacao;
import model.PersonagemOrdem;
import persistenceCommons.SysApoio;

/**
 *
 * @author gurgel
 */
public class BaseModel implements Serializable, IBaseModel, Comparable<Object> {

    private int id;
    private String nome;
    private String codigo;
    private boolean changed = false;
    private String resultados = "";
    private SortedMap<String, Habilidade> habilidades = new TreeMap<>();
    private SortedMap<Integer, PersonagemOrdem> acao = new TreeMap();
    private SortedMap<Integer, PersonagemOrdem> acaoExecutadas = new TreeMap();
    // Optional per-instance extra value for an ability, keyed by ability code (e.g. plague reduction %).
    // DELIBERATELY lazy-null (no initializer) and NOT defaulted in readResolve(): XStream omits a null
    // field, so EGFs stay byte-identical until an ability actually needs a parameter. Do NOT eagerly
    // instantiate it - that would emit <habilidadeParameter/> into every EGF and break old
    // (XStream 1.3.1 / Java 8) Counselors. Accessors are null-safe; remHabilidade() nulls it when empty.
    private SortedMap<String, String> habilidadeParameter;

    // Called by XmlManager's custom ReflectionConverter after XStream deserializes
    // an object. Fields absent from old EGFs arrive as null; this restores defaults.
    private Object readResolve() {
        if (acao == null)           acao = new TreeMap<>();
        if (acaoExecutadas == null) acaoExecutadas = new TreeMap<>();
        if (habilidades == null)    habilidades = new TreeMap<>();
        if (resultados == null)     resultados = "";
        // habilidadeParameter is intentionally left null when absent (lazy-null / omit-when-empty). Do NOT
        // default it here - see the field comment.
        return this;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getComboId() {
        return this.codigo;
    }

    @Override
    public String getComboDisplay() {
        return this.nome;
    }

    @Override
    public String getNome() {
        return nome;
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    public final void setId(int id) {
        this.id = id;
    }

    public final void setNome(String nome) {
        this.nome = nome;
    }

    public final void setCodigo(String codigo) {
        this.codigo = SysApoio.removeAcentos(codigo);
    }

    public String getHabilidadesToDb() {
        String ret = ";";
        for (String cdHab : getHabilidades().keySet()) {
            if (cdHab.equals(";-;")) {
                //skip none, it will be added later. Serves to purge NONE when another Hab is added
                continue;
            }
            ret += cdHab.substring(1);
        }
        if (ret.equals(";")) {
            return ";-;";
        }
        return ret;
    }

    public int getHabilidadesPoints() {
        int ret = 0;
        for (Habilidade habilidade : getHabilidades().values()) {
            ret += habilidade.getCost();
        }
        return ret;
    }

    /**
     * @return the habilidades
     */
    public SortedMap<String, Habilidade> getHabilidades() {
        return habilidades;
    }

    /**
     * @param habilidades the habilidades to set
     */
    public void setHabilidades(SortedMap<String, Habilidade> habilidades) {
        this.habilidades = habilidades;
    }

    public void addHabilidades(SortedMap<String, Habilidade> habilidades) {
        this.habilidades.putAll(habilidades);
    }

    public void addHabilidade(Habilidade habilidade) {
        remHabilidadeNone();
        this.habilidades.put(habilidade.getCodigo(), habilidade);
    }

    public void remHabilidade(Habilidade habilidade) {
        remHabilidade(habilidade.getCodigo());
    }

    public void remHabilidadeNone() {
        this.habilidades.remove(";-;");
    }

    public void remHabilidade(String cdHab) {
        this.habilidades.remove(cdHab);
        // keep any per-instance parameter in lockstep with its ability; null the map when empty so a
        // model that no longer carries a parameter serializes WITHOUT the field (EGF stays clean).
        if (habilidadeParameter != null) {
            habilidadeParameter.remove(cdHab);
            if (habilidadeParameter.isEmpty()) {
                habilidadeParameter = null;
            }
        }
    }

    public boolean hasHabilidade(String cdHabilidade) {
        return this.habilidades.get(cdHabilidade) != null;
    }

    public int getHabilidadeValor(String cdHabilidade) {
        try {
            return this.habilidades.get(cdHabilidade).getValor();
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public boolean hasHabilidades() {
        return this.habilidades.size() > 0;
    }

    /**
     * Per-instance extra value for an ability, keyed by ability code. Null-safe: returns null when no
     * parameter (or no map) exists. Does NOT create the map (preserves lazy-null / EGF omit-when-empty).
     */
    public String getHabilidadeParametro(String cdHabilidade) {
        if (habilidadeParameter == null) {
            return null;
        }
        return habilidadeParameter.get(cdHabilidade);
    }

    /**
     * Stores a per-instance value for an ability (lazily creating the backing map). Pair it with the
     * ability itself; remHabilidade() drops the matching parameter and nulls the map when empty.
     */
    public void setHabilidadeParametro(String cdHabilidade, String valor) {
        if (habilidadeParameter == null) {
            habilidadeParameter = new TreeMap<>();
        }
        habilidadeParameter.put(cdHabilidade, valor);
    }

    @Override
    public String toString() {
        return this.getNome() + "-" + getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    @Override
    public int compareTo(Object o) {
        BaseModel outro = (BaseModel) o;
        return (this.getCodigo().compareToIgnoreCase(outro.getCodigo()));
    }

    /**
     * @return the changed
     */
    public boolean isChanged() {
        return changed;
    }

    /**
     * @param changed the changed to set
     */
    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public String getResultados() {
        return resultados;
    }

    public void setResultados(String resultados) {
        this.resultados = resultados;
    }

    public boolean isPersonagem() {
        return false;
    }

    public boolean isNacaoClass() {
        return getTpActor().equals("N");
    }

    public boolean isCidadeClass() {
        return getTpActor().equals("C");
    }

    public boolean isExercitoClass() {
        return getTpActor().equals("E");
    }

    //client
    public PersonagemOrdem getAcao(int index) {
        try {
            return this.acao.get(index);
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    public void setAcao(int index, PersonagemOrdem pOrdem) {
        if (pOrdem != null) {
            this.acao.put(index, pOrdem);
        } else {
            this.acao.remove(index);
        }
    }

    public int getAcaoSize() {
        return this.acao.size();
    }

    public SortedMap<Integer, PersonagemOrdem> getAcoes() {
        return this.acao;
    }

    public void remAcoes() {
        this.acao.clear();
    }

    //server
    public void addAcaoExecutada(PersonagemOrdem po) {
        this.acaoExecutadas.put(this.acaoExecutadas.size(), po);
    }

    public SortedMap<Integer, PersonagemOrdem> getAcaoExecutadas() {
        return this.acaoExecutadas;
    }

    public int getOrdensQt() {
        return 0;
    }

    public Nacao getNacao() {
        return null;
    }

    public String getTpActor() {
        return "-";
    }
}
