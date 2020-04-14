/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import java.util.Date;
import utils.LongDate;

/**
 *
 * @author gurgel
 */
public class Partida extends BaseModel {

    private int numero, turno, turnoNext, turnoMax = 999;
    private Cenario cenario;
    private Jogador jogadorAtivo; //jogador ativo
    private Mercado mercado;
    private String language;
    private LongDate dtDeadline;
    private final Date deadline = new Date(); //kept for backwards compatibility
    private boolean gameOver = false;

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public int getTurno() {
        return turno;
    }

    public void setTurno(int turno) {
        this.turno = turno;
    }

    public Cenario getCenario() {
        return cenario;
    }

    public void setCenario(Cenario cenario) {
        this.cenario = cenario;
    }

    /**
     * Jogador ativo da partida. OWNER
     *
     * @return
     */
    public Jogador getJogadorAtivo() {
        return jogadorAtivo;
    }

    public void setJogadorAtivo(Jogador jogador) {
        this.jogadorAtivo = jogador;
    }

    public boolean isJogadorAtivo(Nacao nacao) {
        return this.getJogadorAtivo().isNacao(nacao);
    }

    public Mercado getMercado() {
        return mercado;
    }

    public void setMercado(Mercado mercado) {
        this.mercado = mercado;
    }

    /**
     * @return the turnoNext
     */
    public int getTurnoNext() {
        return turnoNext;
    }

    /**
     * @param turnoNext the turnoNext to set
     */
    public void setTurnoNext(int turnoNext) {
        this.turnoNext = turnoNext;
    }

    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @return the gameOver
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * @param gameOver the gameOver to set
     */
    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public LongDate getDeadline() {
        return dtDeadline;
    }

    public void setDeadline(LongDate deadline) {
        this.dtDeadline = deadline;
    }

    /**
     * @return the turnoMax
     */
    public int getTurnoMax() {
        return turnoMax;
    }

    /**
     * @param turnoMax the turnoMax to set
     */
    public void setTurnoMax(int turnoMax) {
        this.turnoMax = turnoMax;
    }

    public boolean isMoveMontanha() {
        return !this.hasHabilidade(";GMM;");
    }

    public boolean isStartupPackages() {
        return this.hasHabilidade(";GCS;");
    }

    public boolean isDeathMatch() {
        return this.hasHabilidade(";GDM;");
    }

    public boolean isTurnRand() {
        return this.hasHabilidade(";GRE;");
    }

    public boolean isTeamLocked() {
        return this.hasHabilidade(";GLA;");
    }

    public boolean isTeamWithLord() {
        return this.hasHabilidade(";GSL;");
    }

    public boolean isFreeForAll() {
        return this.hasHabilidade(";FFA;");
    }

    public boolean isBattleRoyal() {
        return this.hasHabilidade(";GBR;");
    }

    public boolean isInformationNetwork() {
        return this.hasHabilidade(";GIN;");
    }

    public boolean isNewRules() {
        return this.hasHabilidade(";GNR;");
    }

    public boolean isNationPackages() {
        return this.hasHabilidade(";GSP;");
    }

    public int getNationPackagesLimit() {
        return this.getHabilidadeValor(";GSP;");
    }
}
