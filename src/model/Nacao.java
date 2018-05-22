/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import business.interfaces.IActor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author gurgel
 */
public class Nacao extends BaseModel implements IActor {

    private int impostos, money, pontos;
    private boolean ativa = true;
    private Alianca alianca;
    private Cidade capital;
    private Jogador jogador;
    private Raca raca;
    private Color fillColor, borderColor;
    private String flAlianca = "-";
    private final Extrato extrato = new Extrato();
    private final List<Cidade> cidades = new ArrayList();
    private final List<Personagem> personagens = new ArrayList();
    private final SortedMap<String, HabilidadeNacao> habilidadesNacao = new TreeMap();
    private SortedMap<Nacao, Integer> relacionamentos = new TreeMap();
    private SortedMap<String, List<String>> mensagensNacao = new TreeMap<String, List<String>>();

    @Override
    public Nacao getNacao() {
        return this;
    }

    public void addHabilidadeNacao(HabilidadeNacao habilidadeNacao) {
        this.habilidadesNacao.put(habilidadeNacao.getCodigo(), habilidadeNacao);
    }

    public Alianca getAlianca() {
        return alianca;
    }

    public SortedMap<String, HabilidadeNacao> getHabilidadesNacao() {
        return this.habilidadesNacao;
    }

    public boolean hasHabilidadeNacao(String codigo) {
        return this.habilidadesNacao.containsKey(codigo);
    }

    public int getHabilidadeNacaoValor(String codigo) {
        try {
            return this.habilidadesNacao.get(codigo).getVlHabilidadeNacao();
        } catch (Exception ex) {
            return 0;
        }
    }

    public void setAlianca(Alianca alianca) {
        this.alianca = alianca;
    }

    public List<Cidade> getCidades() {
        return cidades;
    }

    public void addCidade(Cidade cidade) {
        this.cidades.add(cidade);
    }

    public Cidade getCapital() {
        return capital;
    }

    public void setCapital(Cidade capital) {
        this.capital = capital;
    }

    public List<Personagem> getPersonagens() {
        return personagens;
    }

    public void addPersonagens(Personagem personagem) {
        this.personagens.add(personagem);
    }

    public void remPersonagen(Personagem personagem) {
        this.personagens.remove(personagem);
    }

    public Raca getRaca() {
        return raca;
    }

    public void setRaca(Raca raca) {
        this.raca = raca;
    }

    public Jogador getOwner() {
        return jogador;
    }

    public void setOwner(Jogador owner) {
        this.jogador = owner;
        owner.addNacao(this);
    }

    public int getImpostos() {
        return impostos;
    }

    public void setImpostos(int impostos) {
        this.impostos = impostos;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int reservas) {
        this.money = reservas;
    }

    public void addRelacionamento(Nacao nacao, int vlRelacionamento) {
        this.relacionamentos.put(nacao, vlRelacionamento);
    }

    public void clearRelacionamento() {
        this.relacionamentos = new TreeMap();
    }

    public int getRelacionamento(Nacao nacao) {
        if (this == nacao) {
            return 0;
        } else {
            try {
                return this.relacionamentos.get(nacao);
            } catch (NullPointerException e) {
                return 0;
            }
        }
    }

    public SortedMap<Nacao, Integer> getRelacionamentos() {
        return this.relacionamentos;
    }

    public Extrato getExtrato() {
        return extrato;
    }

    public int getExtratoSize() {
        return extrato.getSize();
    }

    public void addExtratoDetail(String dsOperacao, int vlOperacao, int vlSaldo) {
        this.extrato.addDetail(dsOperacao, vlOperacao, vlSaldo);
    }

    /**
     * @return the pontos
     */
    public int getPontosVitoria() {
        return pontos;
    }

    /**
     * @param pontos the pontos to set
     */
    public void setPontosVitoria(int pontos) {
        this.pontos = pontos;
    }

    public SortedMap<String, List<String>> getMensagensNacao() {
        return mensagensNacao;
    }

    /**
     * @param mensagensNacao the mensagensNacao to set
     */
    public void setMensagensNacao(SortedMap<String, List<String>> mensagensNacao) {
        this.mensagensNacao = mensagensNacao;
    }

    /**
     * @return the ativa
     */
    public boolean isAtiva() {
        return ativa;
    }

    /**
     * @param ativa the ativa to set
     */
    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fill) {
        this.fillColor = fill;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color border) {
        this.borderColor = border;
    }

    @Override
    public int getOrdensQt() {
        return 3;
    }

    @Override
    public String getTpActor() {
        return "N";
    }

    public String getTeamFlag() {
        return flAlianca;
    }

    public void setTeamFlag(String flAlianca) {
        this.flAlianca = flAlianca;
    }

    @Override
    public boolean isActorActive() {
        return this.isAtiva();
    }

    public int compareToByPv(Object o) {
        Nacao outro = (Nacao) o;
        return (this.getPontosVitoria() - outro.getPontosVitoria());
    }
}
