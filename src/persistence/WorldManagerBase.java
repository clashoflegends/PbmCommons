/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence;

import java.io.Serializable;
import java.util.SortedMap;
import model.*;

/**
 *
 * @author jmoura
 */
public abstract class WorldManagerBase implements Serializable {

    protected World world;

    public WorldManagerBase() {
    }

    public void addLocal(Local local) {
        world.addLocal(local);
    }

    public void addCidade(Cidade cidade) {
        world.addCidade(cidade);
    }

    public SortedMap<String, Artefato> getArtefatos() {
        return world.getArtefatos();
    }

    public Cenario getCenario() {
        return world.getPartida().getCenario();
    }

    public SortedMap<String, Cidade> getCidades() {
        return world.getCidades();
    }

    public SortedMap<String, Exercito> getExercitos() {
        return world.getExercitos();
    }

    public SortedMap<String, Local> getLocais() {
        return world.getLocais();
    }

    public Mercado getMercado() {
        return world.getPartida().getMercado();
    }

    public SortedMap<String, Nacao> getNacoes() {
        return world.getNacoes();
    }

    public SortedMap<String, Ordem> getOrdens() {
        return world.getPartida().getCenario().getOrdens();
    }

    public Partida getPartida() {
        return world.getPartida();
    }

    public SortedMap<String, Personagem> getPersonagens() {
        return world.getPersonagens();
    }

    public int getTurno() {
        return world.getPartida().getTurno();
    }

    public boolean isJogadorAtivo(Nacao nacao) {
        return world.getPartida().getJogadorAtivo().isNacao(nacao);
    }

    public boolean isJogadorAtivoNpc() {
        return world.getPartida().getJogadorAtivo().getId() == 1;
    }

    public void destroyWorld() {
        this.world = null;
    }

    public World createWorld() {
        world = new World();
        return world;
    }
}
