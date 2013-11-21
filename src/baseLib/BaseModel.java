/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package baseLib;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Habilidade;

/**
 *
 * @author gurgel
 */
public class BaseModel implements Serializable, IBaseModel, Comparable<Object> {

    private int id;
    private String nome;
    private String codigo;
    private boolean changed = false;
    private SortedMap<String, Habilidade> habilidades = new TreeMap<String, Habilidade>();

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

    public void setId(int id) {
        this.id = id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getHabilidadesToDb() {
        String ret = ";";
        for (String cdHab : getHabilidades().keySet()) {
            ret += cdHab.substring(1);
        }
        if (ret.equals(";")) {
            return ";-;";
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
        this.habilidades.put(habilidade.getCodigo(), habilidade);
    }

    public void remHabilidade(Habilidade habilidade) {
        this.habilidades.remove(habilidade.getCodigo());
    }

    public boolean hasHabilidade(String cdHabilidade) {
        return this.habilidades.get(cdHabilidade) != null;
    }

    public int getHabilidadeValor(String cdHabilidade) {
        return this.habilidades.get(cdHabilidade).getValor();
    }

    public boolean hasHabilidades() {
        return this.habilidades.size() > 0;
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
}
