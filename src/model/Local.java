/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author gurgel
 */
public class Local extends BaseModel implements Cloneable {

    private int clima;
    private boolean visible = false, producaoInfo = false;
    private String coordenadas;
    private String estrada, rio, riacho, vau, ponte, rastro, landing = "";
    private Terreno terreno;
    private Cidade cidade;
    private final SortedMap<Produto, Integer> producao = new TreeMap<>();
    private final SortedMap<String, Personagem> indicePersonagem = new TreeMap<>();
    private final SortedMap<String, Exercito> indiceExercito = new TreeMap<>();
    private final SortedMap<String, Artefato> indiceArtefato = new TreeMap<>();
    private String visibilidadeNacao;  //manter em string por causa da forma de carga, aonde cada jogador tem uma visibilidade diferente da nacao. Usar sortedMap vai quebrar...

    @Override
    public Local clone() throws CloneNotSupportedException {
        return (Local) super.clone();
    }

    @Override
    public String toString() {
        return this.getCoordenadas();
    }

    public SortedMap<String, Artefato> getArtefatos() {
        return this.indiceArtefato;
    }

    public SortedMap<String, Exercito> getExercitos() {
        return this.indiceExercito;
    }

    public boolean isEstrada(Integer direcao) {
        return this.estrada.contains(direcao.toString());
    }

    /**
     * only to be used by the Judge
     *
     * @return
     */
    public String getEstrada() {
        return this.estrada;
    }

    /**
     * only to be used by the Judge
     *
     * @return
     */
    public String getRio() {
        return this.rio;
    }

    /**
     * only to be used by the Judge
     *
     * @return
     */
    public String getRiacho() {
        return this.riacho;
    }

    /**
     * only to be used by the Judge
     *
     * @return
     */
    public String getVau() {
        return this.vau;
    }

    /**
     * only to be used by the Judge
     *
     * @return
     */
    public String getPonte() {
        return this.ponte;
    }

    public boolean isPonte(Integer direcao) {
        return this.ponte.contains(direcao.toString());
    }

    public boolean isRiacho(Integer direcao) {
        return this.riacho.contains(direcao.toString());
    }

    public boolean isRio(Integer direcao) {
        return this.rio.contains(direcao.toString());
    }

    public boolean isVau(Integer direcao) {
        return this.vau.contains(direcao.toString());
    }

    public boolean isRastro(Integer direcao) {
        return this.rastro.contains(direcao.toString());
    }

    public boolean isLanding(Integer direcao) {
        return this.landing.contains(direcao.toString());
    }

    public Terreno getTerreno() {
        return terreno;
    }

    public void setTerreno(Terreno terreno) {
        this.terreno = terreno;
    }

    public String getCoordenadas() {
        return coordenadas;
    }

    public void setCoordenadas(String coordenadas) {
        this.coordenadas = coordenadas;
    }

    /*
     * ProduçAo alterada pelo clima e demais fatores.<br> Producao natural
     * (original) deve ser utilizado o metodo getProducaoNatural()
     */
    public int getProducaoClima(Produto produto) {
        if (getCoordenadas().equals("1145")) {
            int aa = 1;
        }
        try {
            return producao.get(produto) * this.getProducaoClimaFactor(produto) / 100;
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public int getProducaoNatural(Produto produto) {
        if (getCoordenadas().equals("1145")) {
            int aa = 1;
        }
        return producao.get(produto);
    }

    public void setProducao(Produto produto, int qtd) {
        this.producao.put(produto, qtd);
    }

    /**
     * don't use this. Prefer to use LocalFacade.getProducao(local).
     *
     * @return
     */
    public SortedMap<Produto, Integer> getProducao() {
        if (getCoordenadas().equals("1145")) {
            int aa = 1;
        }
        return this.producao;
    }

    public void clearProducao() {
        this.producao.clear();
    }

    public int getClima() {
        return clima;
    }

    public void setClima(int clima) {
        this.clima = clima;
    }

    public Cidade getCidade() {
        return cidade;
    }

    public void setCidade(Cidade cidade) {
        this.cidade = cidade;
    }

    public void addPersonagem(Personagem personagem) {
        this.indicePersonagem.put(personagem.getCodigo(), personagem);
    }

    public SortedMap<String, Personagem> getPersonagens() {
        return indicePersonagem;
    }

    public void addExercito(Exercito exercito) {
        this.indiceExercito.put(exercito.getCodigo(), exercito);
    }

    public void addArtefato(Artefato artefato) {
        this.indiceArtefato.put(artefato.getCodigo(), artefato);
    }

    public void setEstrada(String estrada) {
        this.estrada = estrada;
    }

    public void addEstrada(int dir) {
        if (!isEstrada(dir)) {
            setEstrada(getEstrada().concat(dir + ""));
        }
    }

    public void setRastro(String rastro) {
        this.rastro = rastro;
    }

    public void setRio(String rio) {
        this.rio = rio;
    }

    public void setRiacho(String riacho) {
        this.riacho = riacho;
    }

    public void setVau(String vau) {
        this.vau = vau;
    }

    public void setPonte(String ponte) {
        this.ponte = ponte;
    }

    public void removeExercito(Exercito exercito) {
        this.indiceExercito.remove(exercito.getCodigo());
    }

    public void removeArtefato(Artefato artefato) {
        this.indiceArtefato.remove(artefato.getCodigo());
    }

//    private int getProducaoClimaOld( Produto produto) {
//        // PENDING: mover para o banco de dados.
//        int[][] producaoClima = new int[8][8];
//        producaoClima[0][0] = 0;
//        producaoClima[0][1] = 0;
//        producaoClima[0][2] = 0;
//        producaoClima[0][3] = 0;
//        producaoClima[0][4] = 0;
//        producaoClima[0][5] = 0;
//        producaoClima[0][6] = 0;
//        producaoClima[0][7] = 0;
//        producaoClima[1][0] = 10;
//        producaoClima[1][1] = 30;
//        producaoClima[1][2] = 30;
//        producaoClima[1][3] = 30;
//        producaoClima[1][4] = 10;
//        producaoClima[1][5] = 10;
//        producaoClima[1][6] = 10;
//        producaoClima[1][7] = 30;
//        producaoClima[2][0] = 20;
//        producaoClima[2][1] = 40;
//        producaoClima[2][2] = 40;
//        producaoClima[2][3] = 40;
//        producaoClima[2][4] = 20;
//        producaoClima[2][5] = 20;
//        producaoClima[2][6] = 20;
//        producaoClima[2][7] = 40;
//        producaoClima[3][0] = 30;
//        producaoClima[3][1] = 60;
//        producaoClima[3][2] = 60;
//        producaoClima[3][3] = 60;
//        producaoClima[3][4] = 30;
//        producaoClima[3][5] = 30;
//        producaoClima[3][6] = 30;
//        producaoClima[3][7] = 60;
//        producaoClima[4][0] = 80;
//        producaoClima[4][1] = 100;
//        producaoClima[4][2] = 100;
//        producaoClima[4][3] = 100;
//        producaoClima[4][4] = 80;
//        producaoClima[4][5] = 80;
//        producaoClima[4][6] = 80;
//        producaoClima[4][7] = 100;
//        producaoClima[5][0] = 90;
//        producaoClima[5][1] = 100;
//        producaoClima[5][2] = 100;
//        producaoClima[5][3] = 100;
//        producaoClima[5][4] = 90;
//        producaoClima[5][5] = 90;
//        producaoClima[5][6] = 90;
//        producaoClima[5][7] = 100;
//        producaoClima[6][0] = 100;
//        producaoClima[6][1] = 100;
//        producaoClima[6][2] = 100;
//        producaoClima[6][3] = 100;
//        producaoClima[6][4] = 100;
//        producaoClima[6][5] = 100;
//        producaoClima[6][6] = 100;
//        producaoClima[6][7] = 100;
//        producaoClima[7][0] = 80;
//        producaoClima[7][1] = 80;
//        producaoClima[7][2] = 80;
//        producaoClima[7][3] = 80;
//        producaoClima[7][4] = 80;
//        producaoClima[7][5] = 80;
//        producaoClima[7][6] = 80;
//        producaoClima[7][7] = 80;
//        return producaoClima[clima][produto.getId()];
//    }
    /**
     * outra coisa, um dia, colocar no banco de dados
     */
//    private void getProducaoClima(int clima, Produto produto) {
//        //YYY: clean up
//        if (this.getIdPartida() >= 122 || this.getIdPartida() == 119) {
//            getProducaoClimaNew();
//        } else {
//            getProducaoClimaOld();
//        }
//    }
    private int getProducaoClimaFactor(Produto produto) {
        // PENDING: mover para o banco de dados.
        final int[][] producaoClima = new int[8][8];
        producaoClima[0][0] = 0;
        producaoClima[0][1] = 0;
        producaoClima[0][2] = 0;
        producaoClima[0][3] = 0;
        producaoClima[0][4] = 0;
        producaoClima[0][5] = 0;
        producaoClima[0][6] = 0;
        producaoClima[0][7] = 0;

        producaoClima[1][0] = 10;
        producaoClima[1][1] = 30;
        producaoClima[1][2] = 30;
        producaoClima[1][3] = 30;
        producaoClima[1][4] = 10;
        producaoClima[1][5] = 40;
        producaoClima[1][6] = 10;
        producaoClima[1][7] = 30;

        producaoClima[2][0] = 40;
        producaoClima[2][1] = 70;
        producaoClima[2][2] = 70;
        producaoClima[2][3] = 70;
        producaoClima[2][4] = 60;
        producaoClima[2][5] = 100;
        producaoClima[2][6] = 40;
        producaoClima[2][7] = 60;

        producaoClima[3][0] = 60;
        producaoClima[3][1] = 100;
        producaoClima[3][2] = 100;
        producaoClima[3][3] = 100;
        producaoClima[3][4] = 60;
        producaoClima[3][5] = 120;
        producaoClima[3][6] = 60;
        producaoClima[3][7] = 100;

        producaoClima[4][0] = 80;
        producaoClima[4][1] = 100;
        producaoClima[4][2] = 100;
        producaoClima[4][3] = 100;
        producaoClima[4][4] = 80;
        producaoClima[4][5] = 100;
        producaoClima[4][6] = 80;
        producaoClima[4][7] = 100;

        producaoClima[5][0] = 90;
        producaoClima[5][1] = 90;
        producaoClima[5][2] = 90;
        producaoClima[5][3] = 90;
        producaoClima[5][4] = 90;
        producaoClima[5][5] = 90;
        producaoClima[5][6] = 90;
        producaoClima[5][7] = 90;

        producaoClima[6][0] = 100;
        producaoClima[6][1] = 80;
        producaoClima[6][2] = 80;
        producaoClima[6][3] = 80;
        producaoClima[6][4] = 100;
        producaoClima[6][5] = 80;
        producaoClima[6][6] = 100;
        producaoClima[6][7] = 80;

        producaoClima[7][0] = 80;
        producaoClima[7][1] = 70;
        producaoClima[7][2] = 70;
        producaoClima[7][3] = 70;
        producaoClima[7][4] = 80;
        producaoClima[7][5] = 60;
        producaoClima[7][6] = 80;
        producaoClima[7][7] = 80;
        return producaoClima[getClima()][produto.getId()];
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * @return the producaoInfo
     */
    public boolean isProducaoInfo() {
        return producaoInfo;
    }

    /**
     * @param producaoInfo the producaoInfo to set
     */
    public void setProducaoInfo(boolean producaoInfo) {
        this.producaoInfo = producaoInfo;
    }

    public void setVisibilidadeNacao(String flVisibilidade) {
        this.visibilidadeNacao = flVisibilidade;
    }

    public String getVisibilidadeNacao() {
        return this.visibilidadeNacao;
    }

    public String getLanding() {
        return landing;
    }

    public void setLanding(String landing) {
        this.landing = landing;
    }
}
