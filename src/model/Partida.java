/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;

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
}
