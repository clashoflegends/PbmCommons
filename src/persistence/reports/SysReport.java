/*
 * SysReport.java
 *
 * Created on 30 de Marco de 2007, 10:33
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package persistence.reports;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SysApoio;
import persistenceLocal.PathFactory;

/**
 *
 * @author gurgel
 */
public class SysReport implements Serializable {

    private static final Log log = LogFactory.getLog(SysReport.class);
    private PrintStream console = System.out;
    private SysPdf pdf;
    private String baseDir;
    private boolean printToRoot = false;
    private boolean fileOpen = false;
    private boolean convertNewLine = false;
    private boolean nowrap = true;
    private int border = 0;
    private final String tableHeader = "<table cellspacing=%d cellpadding=%d border=%d align=center width=100%%>";

    /**
     * Creates a new instance of SysReport
     */
    public SysReport() {
    }

    public void consoleDesvia(String nmFolder, String nmFile, int turno, boolean temPdf) {
        doCreateFolders(nmFolder, turno);
        String nmBaseFile = getBasedir() + nmFile;
        String nmFileHtml = nmBaseFile + ".htm";
        //prepara HTML
        try {
            if ((new File(nmFileHtml)).exists()) {
                boolean success = (new File(nmFileHtml)).delete();
            }
            System.setOut(new PrintStream(new FileOutputStream(nmFileHtml, true)));
        } catch (FileNotFoundException ex) {
            log.error("Problemas gerando arquivo HTML.");
            throw new UnsupportedOperationException("Problemas gerando arquivo HTML.");
        }
        this.impHtml("<html><head>"
                + "<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"/>"
                + "</head><body><pre>");
        //prepara PDF
        if (temPdf) {
            String nmFilePdf = nmBaseFile + ".pdf";
            this.setPdf(new SysPdf(nmFilePdf));
        }
        this.setFileOpen(true);
    }

    /**
     * this one makes sure the folders are created, but does not create a file
     *
     * @param nmFolder
     * @param turno
     */
    public void doCreateFolders(String nmFolder, int turno) {
        setBasedir(nmFolder, turno);
        PathFactory.getInstance().criaBaseDir(nmFolder, turno);
    }

    public void consoleDesviaClose() {
        if (this.isFileOpen()) {
            this.impHtml("</pre></body></html>");
            if (this.isAtivo()) {
                this.getPdf().fechaPdf();
            }
        }
        System.setOut(this.getConsole());
        this.setFileOpen(false);
    }

    private void setBasedir(String nmFolder, int turno) {
        //diretorio para a partida/turno e mapas HTM
        if (isPrintToRoot()) {
            this.baseDir = nmFolder;
        } else {
            this.baseDir = nmFolder + SysApoio.lpad(turno + "", '0', 3) + "/";
        }
    }

    public String getMapadir() {
        return getBasedir() + "mapas/";
    }

    public void salvaImagem(RenderedImage rendImage, String filename) {
        // Write generated image to a file
        try {
            // Save as PNG
            File file = new File(this.getMapadir() + filename);
            ImageIO.write(rendImage, "png", file);
            // Save as JPG
//            file = new File(this.getBasedir() + filename + ".jpg");
//            ImageIO.write(rendImage, "jpg", file);
        } catch (java.io.FileNotFoundException ex) {
            log.error("Problemas no SysReport");
            throw new UnsupportedOperationException("Problemas no SysReport", ex);
        } catch (IOException ex) {
            log.error("Problemas no SysReport", ex);
        }
    }

    public DecimalFormat getMyFormatter() {
        return (SysApoio.getMyFormatter());
    }

    public PrintStream getConsole() {
        return console;
    }

    public void setConsole(PrintStream console) {
        this.console = console;
    }

    public SysPdf getPdf() {
        return pdf;
    }

    public void setPdf(SysPdf pdf) {
        this.pdf = pdf;
    }

    public boolean isAtivo() {
        try {
            return this.getPdf().isAtivo();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public void writeImg(BufferedImage imagem, String fileName) {
        this.salvaImagem(imagem, fileName);
        this.impHtml("<p align='center'><img src=mapas/" + fileName + "></p>");
        if (this.isAtivo()) {
            this.getPdf().escreveImagem(imagem);
        }
    }

    public void writeImgToPdfOnly(BufferedImage imagem, String fileName) {
        if (this.isAtivo()) {
            this.getPdf().escreveCelulaImagem(imagem);
        }
    }

    public void write(String linha) {
        if (!linha.equals("")) {
            this.impHtml(linha);
            if (this.isAtivo()) {
                this.getPdf().imp(linha);
            }
        }
    }

    public void writeBold(String linha) {
        if (!linha.equals("")) {
            this.impHtml(String.format("<b>%s</b>", linha));
            if (this.isAtivo()) {
                this.getPdf().imp(linha, true);
            }
        }
    }

    public void writeBoldFirstLine(String linha) {
        if (!linha.equals("")) {
            String[] linhas = linha.split("\n", 2);
            boolean first = true;
            for (String texto : linhas) {
                if (first) {
                    this.writeBold(texto);
                    first = false;
                } else {
                    this.write(texto);
                }
            }
        }
    }

    public void writeTitle(String linha) {
        this.impHtml("<h2><div align=Center>" + linha + "</div></h2>");
        if (this.isAtivo()) {
            this.getPdf().impTitulo(linha);
        }
    }

    public void newLine() {
        SysApoio.imp();
        if (this.isAtivo()) {
            this.getPdf().imp();
        }
    }

    public void newPage() {
        if (this.isAtivo()) {
            this.getPdf().novaPagina();
        }
    }

    public void lineSeparator() {
        String lineSeparator = "\n<hr>\n";
        this.impHtml(lineSeparator);
        if (this.isAtivo()) {
            this.getPdf().imp();
        }
    }

    public void writeTabela(float[] qtColunas) {
        this.impHtml(String.format(tableHeader, 2, 2, getBorder()));
        if (this.isAtivo()) {
            this.getPdf().escreveTabela(qtColunas);
        }
        this.writeTabelaLine();
    }

    public void writeTabela(int qtColunas) {
        this.impHtml(String.format(tableHeader, 2, 2, getBorder()));
        if (this.isAtivo()) {
            this.getPdf().escreveTabela(qtColunas);
        }
        this.writeTabelaLine();
    }

    public void writeCelulaPre(String texto) {
        this.impHtml("<td " + getNowrap() + "><pre>" + texto + "</pre></td>");
        if (this.isAtivo()) {
            this.getPdf().escreveCelula(texto);
        }
    }

    public void writeCelula(String texto) {
        this.impHtml("<td " + getNowrap() + ">" + texto + "</td>");
        if (this.isAtivo()) {
            this.getPdf().escreveCelula(texto);
        }
    }

    public void writeCelulaPortrait(String texto, BufferedImage imagem, String filename) {
//        this.salvaImagem(imagem, filename);
        this.impHtml("<td nowrap valign=top>");
        this.impHtml("<img src='" + PathFactory.getPortraitDirName() + filename + "'>");
        this.impHtml("<div align=left>" + texto + "</div>");
        if (this.isAtivo()) {
            this.getPdf().escreveCelula(texto, PathFactory.getPortraitDirName() + filename);
        }
    }

    public void writeTabelaLine() {
        this.impHtml("<tr>");
//        if(this.isAtivo()) {
//            this.getPdf().imp();
//        }
    }

    public void writeTabelaFecha() {
        this.impHtml("</table>");
        if (this.isAtivo()) {
            this.getPdf().fechaTabela();
        }
    }

    public void writeSubTabela(float[] qtColunas) {
        this.impHtml(String.format("<td>" + tableHeader, 0, 0, getBorder()));
        if (this.isAtivo()) {
            this.getPdf().escreveSubTabela(qtColunas);
        }
        this.writeSubTabelaLine();
    }

    public void writeSubTabela(int qtColunas) {
        this.impHtml(String.format("<td>" + tableHeader, 0, 0, getBorder()));
        if (this.isAtivo()) {
            this.getPdf().escreveSubTabela(qtColunas);
        }
        this.writeSubTabelaLine();
    }

    public void writeSubCelulaPre(String texto) {
        this.impHtml("<td " + getNowrap() + "><pre>" + texto + "</pre></td>");
        if (this.isAtivo()) {
            this.getPdf().escreveCelula(texto);
        }
    }

    public void writeSubCelula(String texto) {
        this.impHtml("<td " + getNowrap() + ">" + texto + "</td>");
        if (this.isAtivo()) {
            this.getPdf().escreveSubCelula(texto);
        }
    }

    public void writeSubCelulaBold(String texto) {
        this.impHtml("<td " + getNowrap() + "><b>" + texto + "</b></td>");
        if (this.isAtivo()) {
            this.getPdf().escreveSubCelulaBold(texto);
        }
    }

    public void writeSubTabelaLine() {
        this.impHtml("<tr>");
//        if(this.isAtivo()) {
//            this.getPdf().imp();
//        }
    }

    public void writeSubTabelaFecha() {
        this.impHtml("</table>");
        if (this.isAtivo()) {
            this.getPdf().fechaSubTabela();
        }
    }

    public void writeCelulaBold(String texto) {
        this.impHtml("<td " + getNowrap() + "><b>" + texto + "</b></td>");
        if (this.isAtivo()) {
            this.getPdf().escreveCelulaBold(texto);
        }
    }

    public void writeCelulaTitle(String texto) {
        this.impHtml("<td " + getNowrap() + " colspan=10><b>" + texto + "</b></td>");
        if (this.isAtivo()) {
            this.getPdf().escreveCelulaTitulo(texto);
        }
        writeTabelaLine();
    }

    public String getBasedir() {
        return this.baseDir;
    }

    /**
     * @return the fileOpen
     */
    private boolean isFileOpen() {
        return fileOpen;
    }

    /**
     * @param isfileopen the fileOpen to set
     */
    private void setFileOpen(boolean isfileopen) {
        this.fileOpen = isfileopen;
    }

    /**
     * @return the printToRoot
     */
    public boolean isPrintToRoot() {
        return printToRoot;
    }

    /**
     * @param printToRoot the printToRoot to set
     */
    public void setPrintToRoot(boolean printToRoot) {
        this.printToRoot = printToRoot;
    }

    /**
     * @return the border
     */
    public int getBorder() {
        return border;
    }

    /**
     * can only be user after cosoleDesvia
     *
     * @param border the border to set
     */
    public void setBorder(int border) {
        this.border = border;
        if (this.isAtivo()) {
            this.getPdf().setBorder(border);
        }
    }

    /**
     * @return the nowrap
     */
    public boolean isNowrap() {
        return nowrap;
    }

    /**
     * @param nowrap the nowrap to set
     */
    public void setNowrap(boolean nowrap) {
        this.nowrap = nowrap;
    }

    private String getNowrap() {
        if (nowrap) {
            return "nowrap";
        } else {
            return "";
        }
    }

    private void impHtml(String linha) {
        System.out.println(this.ascToHtml(linha));
    }

    private void impHtml(int linha) {
        System.out.println(linha);
    }

    private void impHtml(long linha) {
        System.out.println(linha);
    }

    private String ascToHtml(String original) {
        String ret = original;
        //s = s.replaceAll("[ÀÂ]","A");
        //s = s.replaceAll("Ô","O");
        ret = ret.replaceAll("À", "&Agrave;");
        ret = ret.replaceAll("Á", "&Aacute;");
        ret = ret.replaceAll("Â", "&Acirc;");
        ret = ret.replaceAll("Ã", "&Atilde;");
        ret = ret.replaceAll("Ä", "&Auml;");
        ret = ret.replaceAll("Å", "&Aring;");
        ret = ret.replaceAll("Æ", "&AElig;");
        ret = ret.replaceAll("Ç", "&Ccedil;");
        ret = ret.replaceAll("È", "&Egrave;");
        ret = ret.replaceAll("É", "&Eacute;");
        ret = ret.replaceAll("Ê", "&Ecirc;");
        ret = ret.replaceAll("Ë", "&Euml;");
        ret = ret.replaceAll("Ì", "&Igrave;");
        ret = ret.replaceAll("Í", "&Iacute;");
        ret = ret.replaceAll("Î", "&Icirc;");
        ret = ret.replaceAll("Ï", "&Iuml;");
        ret = ret.replaceAll("Ð", "&ETH;");
        ret = ret.replaceAll("Ñ", "&Ntilde;");
        ret = ret.replaceAll("Œ", "&OElig;");
        ret = ret.replaceAll("Ò", "&Ograve;");
        ret = ret.replaceAll("Ó", "&Oacute;");
        ret = ret.replaceAll("Ô", "&Ocirc;");
        ret = ret.replaceAll("Õ", "&Otilde;");
        ret = ret.replaceAll("Ö", "&Ouml;");
        ret = ret.replaceAll("Ø", "&Oslash;");
        ret = ret.replaceAll("Š", "&Scaron;");
        ret = ret.replaceAll("Ù", "&Ugrave;");
        ret = ret.replaceAll("Ú", "&Uacute;");
        ret = ret.replaceAll("Û", "&Ucirc;");
        ret = ret.replaceAll("Ü", "&Uuml;");
        ret = ret.replaceAll("Ý", "&Yacute;");
        ret = ret.replaceAll("Þ", "&THORN;");
        ret = ret.replaceAll("Ÿ", "&Yuml;");
        ret = ret.replaceAll("à", "&agrave;");
        ret = ret.replaceAll("á", "&aacute;");
        ret = ret.replaceAll("â", "&acirc;");
        ret = ret.replaceAll("ã", "&atilde;");
        ret = ret.replaceAll("ä", "&auml;");
        ret = ret.replaceAll("å", "&aring;");
        ret = ret.replaceAll("æ", "&aelig;");
        ret = ret.replaceAll("ç", "&ccedil;");
        ret = ret.replaceAll("è", "&egrave;");
        ret = ret.replaceAll("é", "&eacute;");
        ret = ret.replaceAll("ê", "&ecirc;");
        ret = ret.replaceAll("ë", "&euml;");
        ret = ret.replaceAll("ì", "&igrave;");
        ret = ret.replaceAll("í", "&iacute;");
        ret = ret.replaceAll("î", "&icirc;");
        ret = ret.replaceAll("ï", "&iuml;");
        ret = ret.replaceAll("ð", "&eth;");
        ret = ret.replaceAll("ñ", "&ntilde;");
        ret = ret.replaceAll("œ", "&oelig;");
        ret = ret.replaceAll("ò", "&ograve;");
        ret = ret.replaceAll("ó", "&oacute;");
        ret = ret.replaceAll("ô", "&ocirc;");
        ret = ret.replaceAll("õ", "&otilde;");
        ret = ret.replaceAll("ö", "&ouml;");
        ret = ret.replaceAll("ø", "&oslash;");
        ret = ret.replaceAll("š", "&scaron;");
        ret = ret.replaceAll("ù", "&ugrave;");
        ret = ret.replaceAll("ú", "&uacute;");
        ret = ret.replaceAll("û", "&ucirc;");
        ret = ret.replaceAll("ü", "&uuml;");
        ret = ret.replaceAll("ý", "&yacute;");
        ret = ret.replaceAll("þ", "&thorn;");
        ret = ret.replaceAll("ÿ", "&yuml;");
        ret = ret.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        if (isConvertNewLine()) {
            ret = ret.replaceAll("\n", "<br>");
        }
        return ret;
    }

    /**
     * @return the convertNewLine
     */
    public boolean isConvertNewLine() {
        return convertNewLine;
    }

    /**
     * @param convertNewLine the convertNewLine to set
     */
    public void setConvertNewLine(boolean convertNewLine) {
        this.convertNewLine = convertNewLine;
    }
}
