/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import model.Artefato;
import model.Cenario;
import model.Cidade;
import model.Exercito;
import model.Habilidade;
import model.Local;
import model.Mercado;
import model.Nacao;
import model.Ordem;
import model.Partida;
import model.Personagem;
import model.VictoryPointsGame;
import model.World;

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

    public Nacao getNacao(int idNation) {
        for (Nacao nation : this.getNacoes().values()) {
            if (nation.getId() == idNation) {
                return nation;
            }
        }
        return null;

    }

    public Nacao getNacao(String idNation) {
        return getNacao(Integer.parseInt(idNation));
    }

    public SortedMap<String, Ordem> getOrdens() {
        return world.getPartida().getCenario().getOrdens();
    }

    public VictoryPointsGame getVictoryPoints() {
        return world.getVictoryPoints();
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

    public int getTurnoMax() {
        return world.getPartida().getTurnoMax();
    }

    public SortedMap<String, Habilidade> getPackages() {
        return world.getPackages();
    }

    public boolean isJogadorAtivo(Nacao nacao) {
        return world.getPartida().getJogadorAtivo().isNacao(nacao);
    }

    public boolean isJogadorAliado(Nacao nacao) {
        return world.getPartida().getJogadorAtivo().isJogadorAliado(nacao);
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

    public List<Nacao> getNacoesJogadorAtivo() {
        List<Nacao> ret = new ArrayList<Nacao>();
        for (Nacao nacao : world.getNacoes().values()) {
            if (world.getPartida().getJogadorAtivo() == nacao.getOwner()) {
                ret.add(nacao);
            }
        }
        return ret;
    }
}
