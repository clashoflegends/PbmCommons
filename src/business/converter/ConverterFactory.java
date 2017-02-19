/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.converter;

import business.facade.LocalFacade;
import java.awt.Point;
import java.io.Serializable;
import java.util.List;
import java.util.SortedMap;
import model.Local;
import model.Ordem;
import model.Produto;
import msgs.BaseMsgs;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 *
 * @author jmoura
 */
public final class ConverterFactory implements Serializable {

    private static final LocalFacade localFacade = new LocalFacade();
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    public static int taticaToInt(String tatica) {
        if (tatica.equalsIgnoreCase("CA")) {
            return 0;
        } else if (tatica.equalsIgnoreCase("FL")) {
            return 1;
        } else if (tatica.equalsIgnoreCase("PA")) {
            return 2;
        } else if (tatica.equalsIgnoreCase("CE")) {
            return 3;
        } else if (tatica.equalsIgnoreCase("GU")) {
            return 4;
        } else if (tatica.equalsIgnoreCase("EM")) {
            return 5;
        } else if (tatica.equalsIgnoreCase("BA")) {
            return 6;
        } else if (tatica.equalsIgnoreCase("SH")) {
            return 7;
        } else if (tatica.equalsIgnoreCase("SF")) {
            return 8;
        } else if (tatica.equalsIgnoreCase("SM")) {
            return 9;
        } else {
            return 2;
        }
    }

    public static String taticaToLabel(String cdTatica) {
        for (String[] tatica : BaseMsgs.taticasGb) {
            if (tatica[1].equalsIgnoreCase(cdTatica)) {
                return tatica[0];
            }
        }
        for (String[] tatica : BaseMsgs.taticasTk) {
            if (tatica[1].equalsIgnoreCase(cdTatica)) {
                return tatica[0];
            }
        }
        return "@PADRAO#";
    }

    private static String taticaToCodigo(int tatica) {
        switch (tatica) {
            case 0:
                return "ca";
            case 1:
                return "fl";
            case 2:
                return "pa";
            case 3:
                return "ce";
            case 4:
                return "gu";
            case 5:
                return "em";
            default:
                return "pa";
        }
    }

    public static String direcaoToStr(String direcao) {
        String ret = "0";
        if (direcao.equalsIgnoreCase("C")) {
            ret = "0";
        } else if (direcao.equalsIgnoreCase("NO")) {
            ret = "1";
        } else if (direcao.equalsIgnoreCase("NE")) {
            ret = "2";
        } else if (direcao.equalsIgnoreCase("L")) {
            ret = "3";
        } else if (direcao.equalsIgnoreCase("SE")) {
            ret = "4";
        } else if (direcao.equalsIgnoreCase("SO")) {
            ret = "5";
        } else if (direcao.equalsIgnoreCase("O")) {
            ret = "6";
        } else {
            ret = "0";
        }
        return ret;
    }

    public static String direcaoToLabel(int direcao) {
        String ret;
        if (direcao == 0) {
            ret = labels.getString("CENTRO.ABREVIADO");
        } else if (direcao == 1) {
            ret = labels.getString("NORTEOESTE.ABREVIADO");
        } else if (direcao == 2) {
            ret = labels.getString("NORTELESTE.ABREVIADO");
        } else if (direcao == 3) {
            ret = labels.getString("LESTE.ABREVIADO");
        } else if (direcao == 4) {
            ret = labels.getString("SULLESTE.ABREVIADO");
        } else if (direcao == 5) {
            ret = labels.getString("SULOESTE.ABREVIADO");
        } else if (direcao == 6) {
            ret = labels.getString("OESTE.ABREVIADO");
        } else {
            ret = labels.getString("CENTRO.ABREVIADO");
        }
        return ret;
    }

    public static int direcaoToInt(String direcao) {
        int ret = 0;
        if (direcao.equalsIgnoreCase("C")) {
            ret = 0;
        } else if (direcao.equalsIgnoreCase("H")) {
            ret = 0;
        } else if (direcao.equalsIgnoreCase("NO")) {
            ret = 1;
        } else if (direcao.equalsIgnoreCase("NW")) {
            ret = 1;
        } else if (direcao.equalsIgnoreCase("NE")) {
            ret = 2;
        } else if (direcao.equalsIgnoreCase("L")) {
            ret = 3;
        } else if (direcao.equalsIgnoreCase("E")) {
            ret = 3;
        } else if (direcao.equalsIgnoreCase("SE")) {
            ret = 4;
        } else if (direcao.equalsIgnoreCase("SO")) {
            ret = 5;
        } else if (direcao.equalsIgnoreCase("SW")) {
            ret = 5;
        } else if (direcao.equalsIgnoreCase("O")) {
            ret = 6;
        } else if (direcao.equalsIgnoreCase("W")) {
            ret = 6;
        } else {
            ret = 0;
        }
        return ret;
    }

    public static int getDirecao(int direcao) {
        int ret = direcao;
        if (ret < 1) {
            ret += 6;
        }
        if (ret > 6) {
            ret -= 6;
        }
        if (ret < 1) {
            ret += 6;
        }
        if (ret > 6) {
            ret -= 6;
        }
        if (ret < 1) {
            ret += 6;
        }
        if (ret > 6) {
            ret -= 6;
        }
        return (ret);
    }

    public static boolean isDirecaoValid(String direcao) {
        boolean ret = false;
        if (direcao.equalsIgnoreCase("C")) {
            ret = true;
        } else if (direcao.equalsIgnoreCase("H")) {
            ret = true;
        } else if (direcao.equalsIgnoreCase("NO")) {
            ret = true;
        } else if (direcao.equalsIgnoreCase("NW")) {
            ret = true;
        } else if (direcao.equalsIgnoreCase("NE")) {
            ret = true;
        } else if (direcao.equalsIgnoreCase("L")) {
            ret = true;
        } else if (direcao.equalsIgnoreCase("E")) {
            ret = true;
        } else if (direcao.equalsIgnoreCase("SE")) {
            ret = true;
        } else if (direcao.equalsIgnoreCase("SO")) {
            ret = true;
        } else if (direcao.equalsIgnoreCase("SW")) {
            ret = true;
        } else if (direcao.equalsIgnoreCase("O")) {
            ret = true;
        } else if (direcao.equalsIgnoreCase("W")) {
            ret = true;
        } else {
            ret = false;
        }
        return ret;
    }

    public static String getCodigoVizinho(Local local, int direcao) {
        String newIdentificacao;
        Integer lCol = Integer.valueOf(local.getCodigo().substring(0, 2));
        Integer lRow = Integer.valueOf(local.getCodigo().substring(2));
        switch (direcao) {
            case 0: //C 0 0
                break;
            case 1: //NO 0 -1
                if ((lRow % 2) == 1) {
                    lCol--;
                }
                lRow--;
                break;
            case 2: //NE +1 -1
                if ((lRow % 2) == 0) {
                    lCol++;
                }
                lRow--;
                break;
            case 3: //L +1 0
                lCol++;
                break;
            case 4: //SE +1 +1
                if ((lRow % 2) == 0) {
                    lCol++;
                }
                lRow++;
                break;
            case 5: //SO 0 +1
                if ((lRow % 2) == 1) {
                    lCol--;
                }
                lRow++;
                break;
            case 6: //O -1 0
                lCol--;
                break;
            default:
                //direcao inválida
                lCol = -1;
                lRow = -1;
                break;
        }
        //verifica se saiu do mapa
        if (lCol < 1 || lRow < 1 || lCol > 99 || lRow > 99) {
            //fora do mapa.
            newIdentificacao = null;
        } else if (direcao == 0) {
            newIdentificacao = local.getCodigo();
        } else {
            newIdentificacao = SysApoio.pointToCoord(lCol, lRow);
        }
        return newIdentificacao;
    }

    public static Point localToPoint(Local local) {
        final Point ret;
        //calcula coordenadas e posicao no grafico.
        final int row = localFacade.getRow(local) - 1, col = localFacade.getCol(local) - 1;
        if (row % 2 == 0) {
            ret = new Point(col * 60, row * 45);
        } else {
            ret = new Point(col * 60 + 30, row * 45);
        }
        return ret;
    }

    public static String coordAlphaToInt(String coord) {
        String ret = "", x, y = "xx", z;
        if (coord.length() == 4) {
            x = coord.substring(0, 2);
            z = coord.substring(2, 4);
        } else {
            x = coord.substring(0, 1);
            z = coord.substring(1, 3);
        }
        if (x.equals("A")) {
            y = "01";
        }
        if (x.equals("B")) {
            y = "01";
        }
        if (x.equals("C")) {
            y = "02";
        }
        if (x.equals("D")) {
            y = "02";
        }
        if (x.equals("E")) {
            y = "03";
        }
        if (x.equals("F")) {
            y = "03";
        }
        if (x.equals("G")) {
            y = "04";
        }
        if (x.equals("H")) {
            y = "04";
        }
        if (x.equals("I")) {
            y = "05";
        }
        if (x.equals("J")) {
            y = "05";
        }
        if (x.equals("K")) {
            y = "06";
        }
        if (x.equals("L")) {
            y = "06";
        }
        if (x.equals("M")) {
            y = "07";
        }
        if (x.equals("N")) {
            y = "07";
        }
        if (x.equals("O")) {
            y = "08";
        }
        if (x.equals("P")) {
            y = "08";
        }
        if (x.equals("Q")) {
            y = "09";
        }
        if (x.equals("R")) {
            y = "09";
        }
        if (x.equals("S")) {
            y = "10";
        }
        if (x.equals("T")) {
            y = "10";
        }
        if (x.equals("U")) {
            y = "11";
        }
        if (x.equals("V")) {
            y = "11";
        }
        if (x.equals("W")) {
            y = "12";
        }
        if (x.equals("X")) {
            y = "12";
        }
        if (x.equals("Y")) {
            y = "13";
        }
        if (x.equals("Z")) {
            y = "13";
        }
        if (x.equals("AA")) {
            y = "14";
        }
        if (x.equals("AB")) {
            y = "14";
        }
        if (x.equals("AC")) {
            y = "15";
        }
        if (x.equals("AD")) {
            y = "15";
        }
        if (x.equals("AE")) {
            y = "16";
        }
        if (x.equals("AF")) {
            y = "16";
        }
        if (x.equals("AG")) {
            y = "17";
        }
        if (x.equals("AH")) {
            y = "17";
        }
        if (x.equals("AI")) {
            y = "18";
        }
        if (x.equals("AJ")) {
            y = "18";
        }
        if (x.equals("AK")) {
            y = "19";
        }
        if (x.equals("AL")) {
            y = "19";
        }
        if (x.equals("AM")) {
            y = "20";
        }
        if (x.equals("AN")) {
            y = "20";
        }
        if (x.equals("AO")) {
            y = "21";
        }
        if (x.equals("AP")) {
            y = "21";
        }
        if (x.equals("AQ")) {
            y = "22";
        }
        if (x.equals("AR")) {
            y = "22";
        }
        if (x.equals("AS")) {
            y = "23";
        }
        if (x.equals("AT")) {
            y = "23";
        }
        if (x.equals("AU")) {
            y = "24";
        }
        if (x.equals("AV")) {
            y = "24";
        }
        if (x.equals("AW")) {
            y = "25";
        }
        ret = y + z;
        return ret;
    }

    public static int estoqueToInt(String tpEstoque) {
        int ret = -1;
        if (tpEstoque.equalsIgnoreCase("CR")) {
            ret = 0;
        } else if (tpEstoque.equalsIgnoreCase("BR")) {
            ret = 1;
        } else if (tpEstoque.equalsIgnoreCase("FE")) {
            ret = 2;
        } else if (tpEstoque.equalsIgnoreCase("AC")) {
            ret = 3;
        } else if (tpEstoque.equalsIgnoreCase("MI")) {
            ret = 3;
        } else if (tpEstoque.equalsIgnoreCase("CM")) {
            ret = 4;
        } else if (tpEstoque.equalsIgnoreCase("LE")) {
            ret = 5;
        } else if (tpEstoque.equalsIgnoreCase("MA")) {
            ret = 5;
        } else if (tpEstoque.equalsIgnoreCase("MO")) {
            ret = 6;
        } else if (tpEstoque.equalsIgnoreCase("OU")) {
            ret = 7;
        }
        return ret;
    }

    public static float initialProductValue(Produto produto) {
        int ret = -1;
        final String tpEstoque = produto.getCodigo();
        if (tpEstoque.equalsIgnoreCase("CR")) {
            ret = 16;
        } else if (tpEstoque.equalsIgnoreCase("BR")) {
            ret = 13;
        } else if (tpEstoque.equalsIgnoreCase("FE")) {
            ret = 19;
        } else if (tpEstoque.equalsIgnoreCase("AC")) {
            ret = 159;
        } else if (tpEstoque.equalsIgnoreCase("MI")) {
            ret = 159;
        } else if (tpEstoque.equalsIgnoreCase("CM")) {
            ret = 5;
        } else if (tpEstoque.equalsIgnoreCase("LE")) {
            ret = 18;
        } else if (tpEstoque.equalsIgnoreCase("MA")) {
            ret = 18;
        } else if (tpEstoque.equalsIgnoreCase("MO")) {
            ret = 35;
        } else if (tpEstoque.equalsIgnoreCase("OU")) {
            ret = 1;
        }
        return ret;
    }

    public static Produto intToProduto(int tipoProduto, SortedMap<String, Produto> produtos) {
        for (Produto produto : produtos.values()) {
            if (produto.getId() == tipoProduto) {
                return produto;
            }
        }
        return null;
    }

    public static Produto intToProduto(int tipoProduto, List<Produto> produtos) {
        for (Produto produto : produtos) {
            if (produto.getId() == tipoProduto) {
                return produto;
            }
        }
        return null;
    }

    public static String getLandmarkName(String cdFeature) {
        if (cdFeature.equals(";LFC;")) {
            return labels.getString("TERRAIN.CAVES.L");
        } else if (cdFeature.equals(";LFH;")) {
            return labels.getString("TERRAIN.HENGES.L");
        } else if (cdFeature.equals(";LFI;")) {
            return labels.getString("TERRAIN.IGLOOS.L");
        } else if (cdFeature.equals(";LFK;")) {
            return labels.getString("TERRAIN.LAKES.L");
        } else if (cdFeature.equals(";LFL;")) {
            return labels.getString("TERRAIN.LITHS.L");
        } else if (cdFeature.equals(";LFE;")) {
            return labels.getString("TERRAIN.TEMPLES.L");
        } else if (cdFeature.equals(";LFR;")) {
            return labels.getString("TERRAIN.RUINS.L");
        } else if (cdFeature.equals(";LFT;")) {
            return labels.getString("TERRAIN.TOWERS.L");
        } else {
            return labels.getString("TERRAIN.RUINS.L");
        }
    }

    public int estoqueToIntEnPt(String tpEstoque) {
        int ret = -1;
        if (tpEstoque.equalsIgnoreCase("LE")) {
            ret = 0;
        } else if (tpEstoque.equalsIgnoreCase("BR")) {
            ret = 1;
        } else if (tpEstoque.equalsIgnoreCase("IR")) {
            ret = 2;
        } else if (tpEstoque.equalsIgnoreCase("ST")) {
            ret = 3;
        } else if (tpEstoque.equalsIgnoreCase("FO")) {
            ret = 4;
        } else if (tpEstoque.equalsIgnoreCase("CM")) {
            ret = 4;
        } else if (tpEstoque.equalsIgnoreCase("WO")) {
            ret = 5;
        } else if (tpEstoque.equalsIgnoreCase("MA")) {
            ret = 5;
        } else if (tpEstoque.equalsIgnoreCase("MO")) {
            ret = 6;
        } else if (tpEstoque.equalsIgnoreCase("GO")) {
            ret = 7;
        } else if (tpEstoque.equalsIgnoreCase("MI")) {
            ret = 3;
        } else if (tpEstoque.equalsIgnoreCase("BR")) {
            ret = 1;
        } else if (tpEstoque.equalsIgnoreCase("FE")) {
            ret = 2;
        } else if (tpEstoque.equalsIgnoreCase("AC")) {
            ret = 3;
        } else if (tpEstoque.equalsIgnoreCase("CM")) {
            ret = 4;
        } else if (tpEstoque.equalsIgnoreCase("CR")) {
            ret = 4;
        } else if (tpEstoque.equalsIgnoreCase("LE")) {
            ret = 5;
        } else if (tpEstoque.equalsIgnoreCase("MA")) {
            ret = 5;
        } else if (tpEstoque.equalsIgnoreCase("MO")) {
            ret = 6;
        } else if (tpEstoque.equalsIgnoreCase("OU")) {
            ret = 7;
        } else if (tpEstoque.equalsIgnoreCase("MI")) {
            ret = 3;
        }
        return ret;
    }

    /**
     * produto de armas e armaduras = estoqueToInt + 1 com criticas
     */
    public static int produtoToInt(String produto) {
        int ret = ConverterFactory.estoqueToInt(produto) + 1;
        if (ret < 0 || ret > 4) {
            ret = 0;
        }
        return ret;
    }

    public static String getAtivaBd(boolean ativa) {
        if (ativa) {
            return "ATIVO";
        } else {
            return "INATIVO";
        }
    }

    public static int getSpellDamage(int levelDiff) {
        final int[] spellDamage = {5, 5, 5, 15, 30, 30};
        return spellDamage[levelDiff];
    }

    public static int getSpellDamage(Ordem ordem) {
        return getSpellDamage(getDificultyToInt(ordem));
    }

    public static int getOrdemDificuldadeAjuste(String orderDif) {
        final int[] ordemAjuste = {0, 1000, 15, 0, -10, 5};
        return ordemAjuste[ConverterFactory.getDificultyToInt(orderDif)];
    }

    public static int getDificultyToInt(Ordem ordem) {
        return ConverterFactory.getDificultyToInt(ordem.getDificuldade());
    }

    public static int getDificultyToInt(String orderDif) {
        //applies to both ordem and feitico
        if (orderDif.equalsIgnoreCase("Aut")) {
            return 1;
        }
        if (orderDif.equalsIgnoreCase("Fac")) {
            return 2;
        }
        if (orderDif.equalsIgnoreCase("Med")) {
            return 3;
        }
        if (orderDif.equalsIgnoreCase("Dif")) {
            return 4;
        }
        if (orderDif.equalsIgnoreCase("Var")) {
            return 5;
        }
        return 0;
    }
}
