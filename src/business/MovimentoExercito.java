/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business;

import business.converter.ConverterFactory;
import business.facade.ExercitoFacade;
import business.facade.LocalFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import model.Local;
import model.TipoTropa;

/**
 *
 * @author jmoura
 */
public final class MovimentoExercito implements Serializable, Cloneable {

    private Local origem;
    private Local destino;
    private int direcaoAnterior = 0; //direcao anterior
    private int direcao;
    private boolean comida;
    private boolean moveMountain = true;
    private boolean cavalarias;
    private int moveType = 0;
    private boolean evasivo;
    private int limiteMovimento;
    private final List<TipoTropa> tropas = new ArrayList<>();
    private boolean docas;
    private final ExercitoFacade ef = new ExercitoFacade();
    private final LocalFacade localFacade = new LocalFacade();
    public static final int BY_LAND = 0;
    public static final int BY_WATER = 1;
    public static final int BY_FIXED = 2;

    @Override
    public MovimentoExercito clone() throws CloneNotSupportedException {
        return (MovimentoExercito) super.clone();
    }

    /**
     *
     * @return 9999 if not possible.
     */
    public int getCustoMovimento() {
        int ret;
        if (isPorAgua()) {
            //por agua
            ret = getCustoMovimentacaoAgua();
        } else if (isPorFixed()) {
            //PC or fixed cost
            ret = 1;
        } else {
            //por terra
            ret = getCustoMovimentacaoTerra();
        }
        return ret;
    }

    /**
     * @return the origem
     */
    public Local getOrigem() {
        return origem;
    }

    /**
     * @param origem the origem to set
     */
    public void setOrigem(Local origem) {
        this.origem = origem;
    }

    /**
     * @return the destino
     */
    public Local getDestino() {
        return destino;
    }

    /**
     * @param destino the destino to set
     */
    public void setDestino(Local destino) {
        this.destino = destino;
    }

    /**
     * @return the direcao
     */
    public int getDirecao() {
        return direcao;
    }

    /**
     * @param direcao the direcao to set
     */
    public void setDirecao(int direcao) {
        this.direcao = direcao;
    }

    /**
     * @return the comida
     */
    public boolean isComida() {
        return comida;
    }

    /**
     * @param comida the comida to set
     */
    public void setComida(boolean comida) {
        this.comida = comida;
    }

    public boolean isMoveMountain() {
        return moveMountain;
    }

    /**
     * @param Montanha the comida to set
     */
    public void setMoveMountain(boolean Montanha) {
        this.moveMountain = Montanha;
    }

    /**
     * @return the cavalarias
     */
    private boolean isCavalarias() {
        return cavalarias;
    }

    /**
     * @param cavalarias the cavalarias to set
     */
    private void setCavalarias(boolean cavalarias) {
        this.cavalarias = cavalarias;
    }

    public int getMoveType() {
        return moveType;
    }

    public void setMoveType(int moveType) {
        this.moveType = moveType;
    }

    /**
     * @return the porAgua
     */
    public boolean isPorAgua() {
        return getMoveType() == MovimentoExercito.BY_WATER;
    }

    public boolean isPorFixed() {
        return getMoveType() == MovimentoExercito.BY_FIXED;
    }

    /**
     * @param porAgua the porAgua to set
     */
    public void setPorAgua(boolean porAgua) {
        if (porAgua) {
            setMoveType(MovimentoExercito.BY_WATER);
        } else {
            setMoveType(MovimentoExercito.BY_LAND);
        }
    }

    /**
     * @return the evasivo
     */
    public boolean isEvasivo() {
        return evasivo;
    }

    /**
     * @param evasivo the evasivo to set
     */
    public void setEvasivo(boolean evasivo) {
        this.evasivo = evasivo;
    }

    /**
     * @return the limiteMovimento
     */
    public int getLimiteMovimento() {
        return limiteMovimento;
    }

    /**
     * @param limiteMovimento the limiteMovimento to set
     */
    public void setLimiteMovimento(int limiteMovimento) {
        this.limiteMovimento = limiteMovimento;
    }

    /**
     * @return the anterior
     */
    public int getDirecaoAnterior() {
        return direcaoAnterior;
    }

    /**
     * @param anterior the anterior to set
     */
    public void setDirecaoAnterior(int anterior) {
        int oldDirection = anterior % 6;
        if (oldDirection == 0 && anterior != 0) {
            //6%6 returns zero, but we want to keep 6 for a valid direction.
            oldDirection = 6;
        }
        this.direcaoAnterior = oldDirection;
        //this.direcaoAnterior = anterior % 6;
    }

    /**
     * @return the tropas
     */
    private List<TipoTropa> getTropas() {
        return tropas;
    }

    /**
     * @param add troop to set
     */
    public void addTropas(TipoTropa tropa) {
        this.tropas.add(tropa);
    }

    public void addTropasAll(Collection<TipoTropa> tropa) {
        this.tropas.addAll(tropa);
    }

    @Override
    public String toString() {
        try {
            return this.origem.getCoordenadas() + " -> " + this.destino.getCoordenadas();
        } catch (NullPointerException e) {
            return "N/A -> N/A";
        }
    }

    private boolean isNavegavel() {
        boolean ret = false;
        //inicio do movimento:
        if (getDirecaoAnterior() == 0 && getDestino().getTerreno().isAgua()) {
            //getDirecaoAnterior() = 0 -> parado ou primeiro movimento.
            //Logo pode sair de terra para agua ou cruzando um rio, mesmo em origem nao navegavel.
            ret = true;
        } else if (getDirecaoAnterior() == 0 && localFacade.isRioLado(getDestino(), getDirecao() + 3)) {
            //anterior = 0 -> parado ou primeiro movimento.
            //Logo pode sair de terra para agua ou cruzando um rio, mesmo em origem nao navegavel.
            ret = true;
        } else if (getOrigem().getTerreno().isAgua() && getDestino().getTerreno().getCodigo().equals("L")) {
            //da agua para um litoral
            ret = true;
        } else if (getOrigem().getTerreno().isAgua() && localFacade.isAncoravel(getDestino())) {
            //da agua para um ancoravel(litoral ou cidadeDocas), provavelmente terminando o movimento.
            ret = true;
        } else if (getOrigem().getTerreno().isAgua() && isDocas()) {
            //TODO: compatibilidade entre Judge/Counselor enquanto Cidade != CentroPopulacional
            //cidadeDocas), provavelmente terminando o movimento.
            ret = true;
        } else if (getOrigem().getTerreno().isAgua() && getDestino().getTerreno().isAgua()) {
            //de agua paga agua
            ret = true;
        } else if (getOrigem().getTerreno().isAgua() && localFacade.isRioLado(getDestino(), getDirecao() + 3)) {
            //da agua entrando em um rio.
            ret = true;
        } else if (localFacade.isRioLado(getOrigem(), getDirecao()) && getDestino().getTerreno().isAgua()) {
            //de um rio para agua
            ret = true;
        } else if (localFacade.isRioLado(getOrigem(), getDirecao()) && localFacade.isRioLado(getDestino(), getDirecao() + 3)) {
            //estes devem levar em contra o percurso, nao apenas destino e origem
            //entao e de terra pra terra, ai tem que ver o percurso do rio.
            int min, max;
            min = Math.min(direcao, getDirecaoAnterior());
            max = Math.max(direcao, getDirecaoAnterior());
            if (min == max) {
                ret = true;
            } else {
                boolean aux = true;
                for (int ii = min + 1; aux && ii <= max - 1; ii++) {
                    if (!getOrigem().isRio(ConverterFactory.getDirecao(ii))) {
                        aux = false;
                    }
                }
                if (!aux) {
                    //testa pelo outro lado
                    aux = true;
                    for (int ii = max + 1; aux && ii <= min + 6 - 1; ii++) {
                        if (!getOrigem().isRio(ConverterFactory.getDirecao(ii))) {
                            aux = false;
                        }
                    }
                }
                ret = aux;
            }
            //entao e de terra pra terra, movimento de ou para cabeceira do rio?
            //tambem se entrar em uma doca e tentar sair do do oturo lado pelo rio.
        }
        return ret;
    }

    /**
     * retorna o custo de movimentacao entre dois hexes<br> 0 = cavalaria<br> 1 = infantaria<br> 2 = esquadra embarcada<p>
     * TERRAIN INFANTRY/MIXED	CAVALRY ONLY <br> NORMAL	ROAD	NORMAL	ROAD <br>
     * SHORE/PLAINS	3	2	2	1 <br> ROUGH 5 3	3	1 <br> FOREST 5	3	5	2 <br> DESERT 4 2	2	1 <br> SWAMP 6	3	5	2 <br>
     * MOUNTAINS	12	6	12	3 <br> BRIDGE/FORD	+1	+1	+1	+1 <br> MINOR RIVER	+2	+2 +2	+2 <br>
     */
    private int getCustoMovimentacaoTerra() {
        if (this.getDestino() == null) {
            return 9999;
        }
        if (getTropas().isEmpty()) {
            return 1;
        }
        int ret = 999;
        if (this.getDirecao() == 0) {
            //Halt order, not affected by lack of food.
            ret = 1;
        } else if (this.getDirecao() > 6) {
            //direcao invalida
            ret = 9999;
        } else {
            //verifica se o movimento eh possivel por terra
            if (this.getDestino().getTerreno().isAgua()) {
                // nao pode ser agua
                //movimento nao eh possivel
                ret = 9999;
            } else if (this.getOrigem().isRio(this.getDirecao())
                    && !this.getOrigem().isPonte(this.getDirecao())
                    && !this.getOrigem().isVau(this.getDirecao())) {
                // nao pode ter rio sem ponte ou vau
                //movimento nao eh possivel
                ret = 9999;
            } else if (this.getOrigem().getTerreno().getCodigo().equals("M")
                    && this.getDestino().getTerreno().getCodigo().equals("M")
                    && !this.getOrigem().isEstrada(this.getDirecao())) {
                // nao pode ser de montanha para montanha sem estrada
                //movimento nao eh possivel
                if (isMoveMountain()) {
                    //entao pode mover na montanha (magia)
                } else {
                    for (TipoTropa tpTropa : getTropas()) {
                        if (!tpTropa.isMoveMountain()) {
                            ret = 9999;
                            break;
                        }
                    }
                }
            }
            if (ret == 999) {
                //se eh possivel, calcula o custo.
                //pega o custo base do terreno destino de acordo com tropa + Com/sem estrada
                ret = this.getCustoMovimentoTerreno();
                //tem riacho ou ponte
                if (this.getOrigem().isPonte(this.getDirecao()) || this.getOrigem().isVau(this.getDirecao())) {
                    ret += 1;
                } else if (this.getOrigem().isRiacho(this.getDirecao())) {
                    ret += 2;
                }
                // 4/3 do custo arredondando pra cima se estiver sem comida.
                if (!this.isComida()) {
                    float temp = (float) ret * 4F / 3F;
                    ret = (int) Math.ceil(temp);
                }
                //dobra os pontos se for movimento evasivo
                if (this.isEvasivo()) {
                    ret *= 2;
                }
            }
        }
        return ret;
    }

    private int getCustoMovimentacaoAgua() {
        int ret;
        //movimento de esquadra por agua.
        if (this.getDirecao() == 0) {
            //Halt order, not affected by lack of food.
            ret = 1;
            if (isEvasivo()) {
                //dobra os pontos se for movimento evasivo
                ret *= 2;
            }
        } else if (isNavegavel()) {
            //pega o custo base do terreno destino de acordo com tropa + Com/sem estrada
            ret = this.getCustoMovimentoTerreno();

            //ret = getDestino().getTerreno().getMovimentoValor()[4];
            // 4/3 do custo arredondando pra cima se estiver sem comida.
            if (!isComida()) {
                float temp = (float) ret * 4F / 3F;
                ret = (int) Math.ceil(temp);
            }
            if (isEvasivo()) {
                //dobra os pontos se for movimento evasivo
                ret *= 2;
            }
        } else {
            ret = 9999;
        }
        return ret;
    }

    /**
     * @return calculate the movement costs
     */
    private int getCustoMovimentoTerreno() {
        if (getTropas().isEmpty()) {
            return 1;
        }
        int ret = ef.getCustoMovimentoBase(getTropas(),
                getDestino().getTerreno(),
                this.getOrigem().isEstrada(this.getDirecao()),
                this.isPorAgua());
        return ret;
    }

    private boolean isDocas() {
        return this.docas;
    }

    /**
     * Only to be used at the judge, to ensure compatibility.
     *
     * @param docas the docas to set
     */
    public void setDocas(boolean docas) {
        this.docas = docas;
    }
}
