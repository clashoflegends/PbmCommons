/*
 * SysApoio.java
 *
 * Created on 26 de Junho de 2006, 16:04
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package baseLib;

import com.ibm.icu.text.Normalizer;
import gui.components.DialogTextArea;
import java.awt.Component;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;
import javax.swing.text.NumberFormatter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;

/**
 *
 * @author Gurgel
 */
public class SysApoio implements Serializable {

    private static final Log log = LogFactory.getLog(SysApoio.class);
    private static DecimalFormat myFormatter = new DecimalFormat("###,###");
    private static long tempoInicio = System.currentTimeMillis();

    /**
     * Creates a new instance of SysApoio
     */
    public SysApoio() {
    }

    /**
     * Returns the dialog that is shown when a problem is encountered.
     *
     * @return dialog to show a message
     */
    public static void showDialogError(String message) {
        showDialogError(message, "Error!");
    }

    /**
     * Returns the dialog that is shown when a problem is encountered.
     *
     * @return dialog to show a message
     */
    public static void showDialogError(String message, String title) {
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.ERROR_MESSAGE);
        JDialog dialog = optionPane.createDialog(null, title);
        dialog.setAlwaysOnTop(true);
        dialog.toFront();
        dialog.setVisible(true);
    }

    /**
     * Returns the dialog that is shown when a problem is encountered.
     *
     * @return dialog to show a message
     */
    public static void showDialogAlert(String message) {
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE);
        JDialog dialog = optionPane.createDialog(null, "Watch out!");
        dialog.setAlwaysOnTop(true);
        dialog.toFront();
        dialog.setVisible(true);
    }

    public static void showDialogInfo(String message, String title) {
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = optionPane.createDialog(null, title);
        dialog.setAlwaysOnTop(true);
        dialog.toFront();
        dialog.setVisible(true);
    }

    public static DialogTextArea showDialogPopup(String title, String text, Component relativeTo) {
        DialogTextArea localTextArea = new DialogTextArea(false);
        localTextArea.setText(text);
        localTextArea.setTitle(title);
        //configura jDialog
        if (relativeTo != null) {
            localTextArea.setLocationRelativeTo(relativeTo);
        }
        localTextArea.pack();
        localTextArea.setVisible(true);
        return localTextArea;
    }

    public static String iif(boolean test, String a, String b) {
        if (test) {
            return a;
        } else {
            return b;
        }
    }

    /**
     * rolagem de dados ate o valor val nunca rola o zero val eh o maior valor
     * possivel
     */
    public static int rand(int val) {
        return (int) (Math.random() * val) + 1;
    }

    public static int iif(boolean test, int a, int b) {
        if (test) {
            return a;
        } else {
            return b;
        }
    }

    /**
     * repete a string x vezes
     */
    public static String repetir(String str, int qtd) {
        String ret = "";
        for (int ii = 0; ii < qtd; ii++) {
            ret += str;
        }
        return ret;
    }

    public static String sprintf(String base, String entra) {
        String ret = "";
        ret += base.replaceFirst("%s", entra);
        return ret;
    }

    public static String sprintf(String base, int entra) {
        String ret = "";
//        Integer entraInt = (Integer) entra;
        ret += base.replaceFirst("%s", new Integer(entra).toString());
        return ret;
    }

    public static String sprintf(String base, int entra, int entra2) {
        String ret = "";
//        Integer entraInt = (Integer) entra;
        ret = base.replaceFirst("%s", new Integer(entra).toString());
        ret = ret.replaceFirst("%s", new Integer(entra2).toString());
        return ret;
    }

    public static String sprintf(String base, String entra, String entra2) {
        String ret = "";
//        Integer entraInt = (Integer) entra;
        ret = base.replaceFirst("%s", entra);
        ret = ret.replaceFirst("%s", entra2);
        return ret;
    }

    public static String sprintf(String base, int entra, String entra2) {
        String ret = "";
//        Integer entraInt = (Integer) entra;
        ret = base.replaceFirst("%s", new Integer(entra).toString());
        ret = ret.replaceFirst("%s", entra2);
        return ret;
    }

    public static String sprintf(String base, int entra, String entra2, String entra3) {
        String ret = "";
//        Integer entraInt = (Integer) entra;
        ret = base.replaceFirst("%s", new Integer(entra).toString());
        ret = ret.replaceFirst("%s", entra2);
        ret = ret.replaceFirst("%s", entra3);
        return ret;
    }

    public static String sprintf(String base, String entra, String entra2, String entra3) {
        String ret = "";
        ret = base.replaceFirst("%s", entra);
        ret = ret.replaceFirst("%s", entra2);
        ret = ret.replaceFirst("%s", entra3);
        return ret;
    }

    /**
     * completa a string com o char ate o comprimento desejado
     */
    public static String rpad(String original, char adicionar, int size) {
        String ret = original;
        while (ret.length() < size) {
            ret += adicionar;
        }
        return ret;
    }

    /**
     * completa a string com o char A ESQUERDA ate o comprimento desejado ideal
     * para zerofill ex: lpad(String numeroOriginal, '0', 2)
     */
    public static String lpad(String original, char adicionar, int size) {
        String ret = "";
        while ((ret.length() + original.length()) < size) {
            ret += adicionar;
        }
        ret = ret.concat(original);
        return ret;
    }

    /**
     * completa a string com o char A ESQUERDA ate o comprimento desejado ideal
     * para zerofill ex: lpad(String numeroOriginal, '0', 2)
     */
    public static String lpad(int original, char adicionar, int size) {
        String ret = "";
        String originalStr = String.valueOf(original);
        while ((ret.length() + originalStr.length()) < size) {
            ret += adicionar;
        }
        ret = ret.concat(originalStr);
        return ret;
    }

    /**
     * Componentes
     */
    public static void impDebug(String linha) {
        log.info((System.currentTimeMillis() - tempoInicio) / 1000 + " \t: " + linha);
    }

    public static void imp(int linha) {
        System.out.println(linha);
    }

    public static void imp(long linha) {
        System.out.println(linha);
    }

    public static void imp(String linha) {
        System.out.println(linha);
    }

    public static void imp() {
        System.out.println();
    }

    public static void impp(int linha) {
        System.out.print(linha);
    }

    public static void impp(String linha) {
        System.out.print(linha);
    }

    /**
     * retorna a data formatada
     */
    public static String now() {
        return String.format("%1$te/%1$tm/%1$tY", java.util.Calendar.getInstance());
    }

    public static String nowTime() {
        return String.format("%tT %1$te/%1$tm/%1$tY", java.util.Calendar.getInstance());
    }

    public static String nowTimestamp() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        return sdf.format(cal.getTime());
    }

    public static long parseDate(String text) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a MM/dd/yyyy", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.parse(text).getTime();
    }

    public static Calendar nextDayOfWeek(int dow) {
        Calendar date = Calendar.getInstance();
        int diff = dow - date.get(Calendar.DAY_OF_WEEK);
        if (!(diff > 0)) {
            diff += 7;
        }
        date.add(Calendar.DAY_OF_MONTH, diff);
        return date;
    }

    /**
     * Converte String em int retorna -9999 se nao for conversivel
     */
    public static int parseInt(String numero) {
        int ret = 0;
        try {
            numero = numero.trim();
            numero = numero.replace('.', ',');
            numero = numero.replaceAll(",", "");
            numero = numero.replaceAll("%", "");
            if (numero.equalsIgnoreCase("")) {
                numero = "0";
            }
//            ret = Integer.parseInt(numero);
            ret = Integer.valueOf(numero);
        } catch (NumberFormatException nfe) {
            ret = -9999;
        } catch (NullPointerException nfe) {
            ret = -9999;
        }
        return ret;
    }

    public static String exceptionToString(Exception ex) {
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

    /**
     * Testa se SUB esta em Main case insensitive
     */
    public static boolean isStrInStr(String main, String sub) {
        return (main.toLowerCase().indexOf(sub.toLowerCase()) != -1);
    }

    public static String getStrRight(String main, String mark) {
        String[] split = main.split(mark);
        return split[split.length - 1];
    }

    public static DecimalFormat getMyFormatter() {
        return myFormatter;
    }

    public static void setMyFormatter(DecimalFormat aMyFormatter) {
        myFormatter = aMyFormatter;
    }

    public static long getTempoInicio() {
        return tempoInicio;
    }

    public static void setTempoInicio(long aTempoInicio) {
        tempoInicio = aTempoInicio;
    }

    public static String removeAcentos(String s) {
//        String temp = Normalizer.normalize(s, Normalizer.DECOMP, 0);
        String temp = Normalizer.normalize(s, Normalizer.NFD, 0);
        return temp.replaceAll("[^\\p{ASCII}]", "");
    }

    public static String toPrimeiraMaiuscula(String s) {
        String ret = "";
        String palavra[] = s.split(" ");
        for (int ii = 0; ii < palavra.length; ii++) {
            if (!ret.equalsIgnoreCase("")) {
                ret += " ";
            }
            try {
                ret += palavra[ii].substring(0, 1).toUpperCase() + palavra[ii].substring(1);
            } catch (java.lang.StringIndexOutOfBoundsException e) {
                //double spaces were sent, resulting in a null word. safe to ignore
            }
        }
        return ret;
    }

    public static String arrayToString(String[] vetor) {
        String ret = "";
        for (int ii = 0; ii < vetor.length; ii++) {
            if (ii == 0) {
                ret += vetor[ii];
            } else {
                ret += "#@#" + vetor[ii];
            }
        }
        return (ret);
    }

    public static String arrayToString(String[][] vetor) {
        String ret = "";
        for (int ii = 0; ii < vetor.length; ii++) {
            if (ii == 0) {
                ret += arrayToString(vetor[ii]);
            } else {
                ret += "#|#" + arrayToString(vetor[ii]);
            }
        }
        return (ret);
    }

    public static String[] stringToArray(String vetor) {
        return stringToArray(vetor, "#@#");
    }

    private static String[] stringToArray(String vetor, String separator) {
        return (vetor.split(separator));
    }

    public static String[][] stringToArrayMulti(String vetor, String spliter, String subSpliter) {
        String[] ret = vetor.split(spliter);
        int xx = ret.length;
        int yy = ret[0].split(spliter).length;
        String[][] temp = new String[xx][yy];
        for (int ii = 0; ii < ret.length; ii++) {
            temp[ii] = stringToArray(ret[ii], subSpliter);
        }
        return (temp);
    }

    public static int[] stringToArrayInt(String vetor) {
        String[] temp = vetor.split("#@#");
        int ret[] = new int[temp.length];
        for (int ii = 0; ii < ret.length; ii++) {
            ret[ii] = parseInt(temp[ii]);
        }
        return (ret);
    }

    public static String[][] stringToArray2(String vetor) {
        return stringToArrayMulti(vetor, "#|#", "#@#");
    }

    //A convenience method for creating a MaskFormatter.
    /*
     # any valid number
     ' the escape character
     ? any character
     A anycharacter or number
     * anything
     U any character, but all lowercase letters become uppercase
     L maps uppercase letters to lower case.
     H specifies a hex character. 
     */
    public static MaskFormatter createFormatter(String s) {
        MaskFormatter formatter = null;
        try {
            formatter = new MaskFormatter(s);
        } catch (java.text.ParseException exc) {
            System.err.println("formatter is bad: " + exc.getMessage());
            throw new UnsupportedOperationException(exc);
        }
        return formatter;
    }

    public static MaskFormatter createFormatterName(String s) {
        MaskFormatter formatter = null;
        try {
            formatter = new MaskFormatter(s);
            formatter.setValidCharacters("abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        } catch (java.text.ParseException exc) {
            System.err.println("formatter is bad: " + exc.getMessage());
            throw new UnsupportedOperationException(exc);
        }
        return formatter;
    }

    public static DefaultFormatterFactory createFormatterFactory(String s) {
        return new DefaultFormatterFactory(createFormatterName(s));
    }

    public static NumberFormatter createFormatterInteger(int min, int max) {
        NumberFormatter nf = new NumberFormatter();
        nf.setMinimum(min);
        nf.setMaximum(max);
        return nf;
    }

    // IndexOf search
    // ----------------------------------------------------------------------
    // Object IndexOf
    //-----------------------------------------------------------------------
    /**
     * <p>Finds the index of the given object in the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} (
     * <code>-1</code>) for a
     * <code>null</code> input array.</p>
     *
     * @param array the array to search through for the object, may * * * * * *
     * be <code>null</code>
     * @param objectToFind the object to find, may be <code>null</code>
     * @return the index of the object within the array,
     * {@link #INDEX_NOT_FOUND} (<code>-1</code>) if not found * * * * * * * *
     * or <code>null</code> array input
     */
    public static int indexOf(Object[] array, Object objectToFind) {
        return indexOf(array, objectToFind, 0);
    }

    /**
     * <p>Finds the index of the given object in the array starting at the given
     * index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} (
     * <code>-1</code>) for a
     * <code>null</code> input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the
     * array length will return {@link #INDEX_NOT_FOUND} (
     * <code>-1</code>).</p>
     *
     * @param array the array to search through for the object, may * * * * * *
     * be <code>null</code>
     * @param objectToFind the object to find, may be <code>null</code>
     * @param startIndex the index to start searching at
     * @return the index of the object within the array starting at the index,
     * {@link #INDEX_NOT_FOUND} (<code>-1</code>) if not found * * * * * * * *
     * or <code>null</code> array input
     */
    public static int indexOf(Object[] array, Object objectToFind, int startIndex) {
        int INDEX_NOT_FOUND = -1;
        try {
            if (array == null) {
                return INDEX_NOT_FOUND;
            }
            if (startIndex < 0) {
                startIndex = 0;
            }
            if (objectToFind == null) {
                for (int i = startIndex; i < array.length; i++) {
                    if (array[i] == null) {
                        return i;
                    }
                }
            } else {
                for (int i = startIndex; i < array.length; i++) {
                    if (objectToFind.equals(array[i])) {
                        return i;
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        }
        return INDEX_NOT_FOUND;
    }

//    public static String getVersionCommons() {
//        ResourceBundle rb = ResourceBundle.getBundle("version_commons");
//        String msg = "", propToken = "BUILD_COMMONS";
//        try {
//            msg = rb.getString(propToken);
//        } catch (MissingResourceException e) {
//            System.err.println("Token ".concat(propToken).concat(" not in Propertyfile!"));
//        }
//        return msg;
//    }
    /**
     * version.properties deve estar no default package. PbmCommons nao pode ter
     * um version.properties para evitar conflitos.
     *
     * @return
     */
    public static String getVersion(String fileName) {
        ResourceBundle rb = ResourceBundle.getBundle(fileName);
        String msg = "", propToken = "BUILD";
        try {
            msg = rb.getString(propToken);
        } catch (MissingResourceException e) {
            log.error("Token ".concat(propToken).concat(" not in Propertyfile!"));
        }
        return msg;
    }

    public static String pointToCoord(Integer col, Integer row) {
        String newIdentificacao;
        //converte de volta para string e retorna o Hex
        DecimalFormat formato = new DecimalFormat("00");
        newIdentificacao = formato.format(col) + formato.format(row);
        return newIdentificacao;
    }

    public static int getDistancia(int colOrigem, int colDestino, int rowOrigem, int rowDestino) {
        int ret = 0;
        int difCol = Math.abs(colOrigem - colDestino);
        int difRow = Math.abs(rowOrigem - rowDestino);
        int difRowArrendondar;
        if ((difRow % 2) > 0) {
            difRowArrendondar = (difRow / 2) + 1;
        } else {
            difRowArrendondar = difRow / 2;
        }
        int somaDifs = difRow + difCol - Math.min(difCol, difRowArrendondar);
        int difResidual = 99;
        if (rowOrigem % 2 == 0) {
            if ((rowDestino % 2 == 1) && (colOrigem > colDestino)) {
                if (colDestino < (colOrigem - (Math.abs(rowDestino - rowOrigem + Math.signum(rowOrigem - rowDestino)) / 2))) {
                    difResidual = 1;
                } else {
                    difResidual = 0;
                }
            } else {
                difResidual = 0;
            }
        } else {
            if ((rowDestino % 2 == 0) && (colOrigem < colDestino)) {
                if (colDestino > (colOrigem + (Math.abs(rowDestino - rowOrigem + Math.signum(rowOrigem - rowDestino)) / 2))) {
                    difResidual = 1;
                } else {
                    difResidual = 0;
                }
            } else {
                difResidual = 0;
            }
        }
        ret = somaDifs + difResidual;
        return ret;
    }

    public static String doParseString(String input, BundleManager labels) {
        String ret = "";
        try {
            String elemLabel, elemSubs;
            String[] texto = input.split("\n");
            Object[] temp;
            for (String linha : texto) {
                if (linha.contains("@;")) {
                    //pega o primeiro elemento
                    String[] base = linha.split("@;", 2);
                    //elemLabel
                    int start = base[0].indexOf("@"), end = base[0].indexOf("#");
                    elemSubs = linha.substring(start, end + 1);
                    elemLabel = linha.substring(start + 1, end);
                    //elemLabel = base[0].substring(1, base[0].length() - 1);
                    //coloca todos os outros em um vetor para substituicao
                    temp = base[1].split("@;");
                    //efetua a substituicao e atualiza a linha
                    try {
                        linha = base[0].replaceAll(elemSubs, String.format(labels.getString(elemLabel), temp));
//                    linha = String.format(labels.getString(elemLabel), temp);
                    } catch (java.util.MissingResourceException e) {
                        log.fatal("Missing Label: {" + elemLabel + "}", e);
                        throw new UnsupportedOperationException(e);
                    } catch (java.util.MissingFormatArgumentException e) {
                        log.fatal("linha: " + linha);
                        log.fatal("temp: " + temp);
                        log.fatal("elemLabel: " + elemLabel);
                        log.fatal("Labels: " + labels.getString(elemLabel));
                        log.fatal(e);
                        throw new UnsupportedOperationException(e);
                    } catch (java.util.IllegalFormatConversionException e) {
                        log.fatal("linha: " + linha);
                        log.fatal("temp: " + temp);
                        log.fatal("elemLabel: " + elemLabel);
                        log.fatal("Labels: " + labels.getString(elemLabel));
                        log.fatal(e);
                        throw new UnsupportedOperationException(e);
                    }
                    //parametros podem ser labels, entao decodifica de novo a linha
                }
                if (linha.contains("@")) {
                    while (linha.contains("@")) {
                        int start = linha.indexOf("@"), end = linha.indexOf("#");
                        elemSubs = linha.substring(start, end + 1);
                        elemLabel = linha.substring(start + 1, end);
                        linha = linha.replaceAll(elemSubs, labels.getString(elemLabel));
                    }
                }
                //\n removido pelo primeiro split.
                if (texto.length > 1) {
                    ret += linha + "\n";
                } else {
                    ret += linha;
                }
                //debug
//            log.fatal(linha);
            }
        } catch (NullPointerException e) {
            //just ignore null strings
        } catch (java.util.MissingResourceException e) {
            log.fatal(input, e);
            throw new UnsupportedOperationException(e);
        } catch (java.lang.StringIndexOutOfBoundsException e) {
            log.fatal(input, e);
            throw new UnsupportedOperationException(e);
        }
        //para debug: imprime o conteudo do banco de dados nos resultados.
        //ret += "\n\n=====================\n\n\n" + new String(input);

        //verifica se esta passando uma string nao substituida
        //comentar caso turno antigo (anterior a 05-feb-2011)
        if (ret.contains("%s")) {
            log.fatal("Input: " + input);
            log.fatal("Output: " + ret);
            throw new UnsupportedOperationException("Saindo, faltou substituir um %s.");
        }
        return ret;
    }

    public static String md5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            log.fatal("nao deu certo criar o MD5 de " + md5);
        }
        return "MISSING";
    }
}
