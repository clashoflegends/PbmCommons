/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.services;

import baseLib.BaseModel;
import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import model.Nacao;
import model.Ordem;
import model.Pelotao;
import model.Personagem;
import model.PersonagemOrdem;
import model.Produto;
import model.Terreno;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class ComparatorFactory implements Serializable {

    private static final Log log = LogFactory.getLog(ComparatorFactory.class);

    /**
     * Sorting by model.Ordem.Number
     *
     * @param lista
     */
    public static void getComparatorOrdemSorter(List<Ordem> lista) {
        Collections.sort(lista, new ComparatorOrdemSorter());
    }

    public static void getComparatorNationVictoryPointsSorter(List<Nacao> nations) {
        Collections.sort(nations, (Object a, Object b) -> ((Nacao) a).compareToByPv((Nacao) b));
    }

    public static void getComparatorNationSorter(List<Personagem> chars) {
        Collections.sort(chars, (Object a, Object b) -> ((Personagem) a).getNacao().compareTo(((Personagem) b).getNacao()));
    }

    /**
     * Sorting by model.Ordem.Number
     *
     * @param lista
     */
    public static void getComparatorPersonagemOrdemSorter(List<PersonagemOrdem> lista) {
        try {
            Collections.sort(lista, new ComparatorPersonagemOrdemSorter());
        } catch (IllegalArgumentException e) {
            //FIXME: Comparison method violates its general contract!
            log.error("Comparison method for PersonagemOrder violates its general contract!", e);
            //don't sort. 
        }
    }

    /**
     * ordena a lista de pelotoes pelo Id
     *
     * @param lista
     */
    public static void getComparatorPelotaoSorter(List<Pelotao> lista) {
        Collections.sort(lista, new ComparatorBaseModelSorter(false));
    }

    /**
     * ordena a lista de TipoTropa pelo ataque
     *
     * @param lista
     */
    public static void getComparatorTipoTropaAttackSorter(List<TipoTropa> lista, Terreno terreno) {
        Collections.sort(lista, new ComparatorTipoTropaAttackSorter(terreno));
    }

    /**
     * ordena a lista de TipoTropa pelo ataque
     *
     * @param lista
     */
    public static void getComparatorPelotaoAttackSorter(List<Pelotao> lista, Terreno terreno) {
        Collections.sort(lista, new ComparatorTipoTropaAttackSorter(terreno));
    }

    /**
     * ordena pelotao de acordo com a tatica para casualties
     *
     * @param lista
     * @param tatica
     * @param terreno
     */
    private static void getComparatorCasualtiesPelotaoSorter(List<Pelotao> lista, int tatica, Terreno terreno, int partidaId) {
        Collections.sort(lista, new ComparatorCasualtiesSorter(tatica, terreno));
    }

    public static void getComparatorCasualtiesPelotaoSorter(List<Pelotao> lista, int tatica, Terreno terreno) {
        Collections.sort(lista, new ComparatorCasualtiesSorter(tatica, terreno));
    }

    /**
     * ordena tropas de acordo com a tatica para casualties
     *
     * @param lista
     * @param tatica
     * @param terreno
     */
    public static void getComparatorCasualtiesTipoTropaSorter(List<TipoTropa> lista, int tatica, Terreno terreno, int partidaId) {
        Collections.sort(lista, new ComparatorCasualtiesSorter(tatica, terreno));
    }

    private static void getComparatorCasualtiesTipoTropaSorter(List<TipoTropa> lista, int tatica, Terreno terreno) {
        Collections.sort(lista, new ComparatorCasualtiesSorter(tatica, terreno));
    }

    /**
     * ordena pelotao de acordo com o custo das unidades
     *
     * @param lista
     */
    public static void getComparatorPelotaoUpkeepSorter(List<Pelotao> lista) {
        Collections.sort(lista, new ComparatorTipoTropaUpkeepSorter());
    }

    public static void getComparatorProdutoSorter(List<Produto> lista) {
        Collections.sort(lista, new ComparatorBaseModelSorter(false));
    }

    public static void getComparatorProdutoDescSorter(List<Produto> lista) {
        Collections.sort(lista, new ComparatorBaseModelSorter(true));
    }

    public static void getComparatorComboDisplaySorter(List<? extends BaseModel> lista) {
        Collections.sort(lista, new ComparatorBaseDisplayModelSorter(false));
    }

    public static void getComparatorBaseModelNameSorter(List<? extends BaseModel> lista) {
        Collections.sort(lista, new ComparatorBaseDisplayModelSorter(false));
    }

    public static void getComparatorFileTimeSorter(File[] files) {
        Arrays.sort(files, (File f1, File f2) -> Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()));
    }
}
