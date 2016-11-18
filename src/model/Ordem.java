/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class Ordem extends BaseModel {

    private boolean comandante = false, emissario = false, mago = false, agente = false, geral = false;
    private boolean feitico;
    private boolean improve;
    private int numero, custo;
    private String descricao;
    private String chave;
    private String dificuldade;
    private String tipo;
    private String tipoPersonagem;
    private String requisito;
    private String parametros; //kept for backwards compatibility
    private String ajuda;
    private String[] parametrosIde;
    private String[] parametrosIdeDisplay;

    public String getChave() {
        return chave;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getDificuldade() {
        return dificuldade;
    }

    public void setDificuldade(String dificuldade) {
        this.dificuldade = dificuldade;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean isTipoPericia() {
        return this.tipo.equalsIgnoreCase("Per");
    }

    public boolean isTipoMovimentacao() {
        return this.tipo.equalsIgnoreCase("Mov");
    }

    public String getTipoPersonagem() {
        return tipoPersonagem;
    }

    public void setTipoPersonagem(String tipoPersonagem) {
        this.tipoPersonagem = tipoPersonagem;
    }

    public String getRequisito() {
        return requisito;
    }

    public void setRequisitos(String observacao) {
        this.requisito = observacao;
    }

    public boolean isComandante() {
        return comandante;
    }

    public boolean isCidadeOrdem() {
        return tipoPersonagem.equalsIgnoreCase("F");
    }

    public boolean isNacaoOrdem() {
        return tipoPersonagem.equalsIgnoreCase("N");
    }

    public boolean isNpc() {
        return tipoPersonagem.equalsIgnoreCase("G");
    }

    public boolean isMilestone() {
        return tipoPersonagem.equalsIgnoreCase("Z");
    }

    public boolean isMista() {
        return tipoPersonagem.equalsIgnoreCase("X");
    }

//    public boolean isMovimento() {
//        return getTipo().equalsIgnoreCase("Mov");
//    }
//    public boolean isMain() {
//        return getTipo().equalsIgnoreCase("Per");
//    }
    public void setComandante(boolean comandante) {
        this.comandante = comandante;
    }

    public boolean isEmissario() {
        return emissario;
    }

    public void setEmissario(boolean emissario) {
        this.emissario = emissario;
    }

    public boolean isMago() {
        return mago;
    }

    public void setMago(boolean mago) {
        this.mago = mago;
    }

    public boolean isAgente() {
        return agente;
    }

    public void setAgente(boolean agente) {
        this.agente = agente;
    }

    public boolean isGeral() {
        return geral;
    }

    public void setGeral(boolean geral) {
        this.geral = geral;
    }

    public int getCusto() {
        return custo;
    }

    public void setCusto(int custo) {
        this.custo = custo;
    }

    @Override
    public String getComboDisplay() {
        if (SettingsManager.getInstance().getConfig("mostraNumeroOrdem", "0").equalsIgnoreCase("0")) {
            return getDescricao();
        } else {
            return getNumero() + " - " + getDescricao();
        }
    }

    @Override
    public String getComboId() {
        return String.format("%d", getNumero());
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public boolean isFeitico() {
        return this.feitico;
    }

    /**
     * @param feitico the feitico to set
     */
    public void setFeitico(boolean feitico) {
        this.feitico = feitico;
    }

    /**
     * @return the improve
     */
    public boolean isImprove() {
        return improve;
    }

    /**
     * @param improve the improve to set
     */
    public void setImprove(boolean improve) {
        this.improve = improve;
    }

    public int compareToByNumber(Object o) {
        Ordem outro = (Ordem) o;
        return (this.getNumero() - outro.getNumero());
    }

    public String getParametros() {
        String ret = "";
        for (String title : parametrosIdeDisplay) {
            ret += title + "; ";
        }
        return ret;
    }

    public String getAjuda() {
        return ajuda;
    }

    public void setAjuda(String ajuda) {
        this.ajuda = ajuda;
    }

    public String getParametroIde(int index) {
        try {
            return parametrosIde[index].trim();
        } catch (ArrayIndexOutOfBoundsException exception) {
            return "-";
        } catch (NullPointerException ex) {
            return "-";
        }
    }

    public int getParametrosIdeQtd() {
        try {
            return parametrosIde.length;
        } catch (Exception ex) {
            //do nothing
            return 0;
        }
    }

    public void setParametrosIde(String[] parametrosIde) {
        this.parametrosIde = parametrosIde;
    }

    public String getParametroIdeDisplay(int index) {
        try {
            return parametrosIdeDisplay[index].trim();
        } catch (ArrayIndexOutOfBoundsException exception) {
            return "-";
        } catch (NullPointerException ex) {
            return "-";
        }
    }

    public void setParametrosIdeDisplay(String[] parDisplay) {
        this.parametrosIdeDisplay = parDisplay;
    }
}
