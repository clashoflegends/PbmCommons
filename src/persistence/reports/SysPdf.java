/*
 * SysPdf.java
 *
 * Created on 30 de Marco de 2007, 09:23
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package persistence.reports;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SysApoio;

/**
 *
 * @author gurgel
 */
public final class SysPdf implements Serializable {

    private static final Log log = LogFactory.getLog(SysPdf.class);
    private Document document = new Document();
    private PdfPTable tabelaAtiva;
    private PdfPTable subTabelaAtiva;
    private boolean ativo = false;
    private int border = 0;

    /**
     * Creates a new instance of SysPdf
     */
    public SysPdf(String filenameImg) {
        this.setAtivo(this.criaPdf(filenameImg));

    }

    public boolean criaPdf(String filename) {
        boolean ret = false;
        if ((new File(filename)).exists()) {
            boolean success = (new File(filename)).delete();
        }
        //cria o pdf
        try {
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file
            PdfWriter writer = PdfWriter.getInstance(getDocument(), new FileOutputStream(filename));
            //formatando exibicao
            writer.setViewerPreferences(PdfWriter.PageLayoutOneColumn);
            writer.setStrictImageSequence(true);
            writer.setInitialLeading(18);
            //adicionando meta dados
            getDocument().addTitle("Turn Results");
            getDocument().addAuthor("Clash of Legends");
            getDocument().addSubject("Turn Results");
            getDocument().addKeywords("turno, results");
            getDocument().addCreator("Clash of Legends");
            // step 3: we open the document
            getDocument().open();
            //sinaliza saida
            ret = true;
        } catch (DocumentException ex) {
            log.error(ex);
        } catch (IOException ex) {
            log.error(ex);
        }
        return (ret);
    }

    private void escreveParagrafoPdf(String texto) {
        try {
            // step 4: we add a paragraph to the document
            Paragraph p1 = new Paragraph(new Chunk(
                    texto, FontFactory.getFont(FontFactory.HELVETICA, 12)));
            this.getDocument().add(p1);
//            document.add(new Paragraph(texto));

//            fox.setTextRise(superscript);
//            fox.setBackground(new Color(0xFF, 0xDE, 0xAD));
//            dog.setTextRise(subscript);
//            dog.setUnderline(new Color(0xFF, 0x00, 0x00), 3.0f, 0.0f, -5.0f + subscript, 0.0f, PdfContentByte.LINE_JOIN_BEVEL);
//            p1.add("The leading of this paragraph is calculated automagically. ");
//            p1.add("The default leading is 1.5 times the fontsize. ");
//            p1.add(new Chunk("You can add chunks "));
//            p1.add(new Phrase("or you can add phrases. "));
//            p1.add(new Phrase(
//                    "Unless you change the leading with the method setLeading, the leading doesn't change if you add " +
//                    "text with another leading. This can lead to some problems.",
//                    FontFactory.getFont(FontFactory.HELVETICA, 18)));
//            document.add(p1);
//            Paragraph p2 = new Paragraph(new Phrase(
//                    "This is my second paragraph. ", FontFactory.getFont(
//                    FontFactory.HELVETICA, 14)));
//            p2.add("As you can see, it started on a new line.");
//            document.add(p2);
//            Paragraph p3 = new Paragraph("This is my third paragraph.",
//                    FontFactory.getFont(FontFactory.HELVETICA, 12));
//            document.add(p3);
        } catch (DocumentException de) {
            System.err.println(de.getMessage());
        }
    }

    /**
     * imprime paragrafo padrao
     */
    public void imp(String texto) {
        imp(texto, false);
    }

    /**
     * imprime paragrafo padrao
     */
    public void imp(String texto, boolean bold) {
        texto = SysApoio.toLinereakSpaceFromLinebreakTab(texto);
        try {
            Font font;
            if (bold) {
                font = FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD);
            } else {
                font = FontFactory.getFont(FontFactory.TIMES, 12);
            }
            Paragraph p1 = new Paragraph(new Chunk(
                    texto, font));
            p1.setAlignment(Element.ALIGN_JUSTIFIED);
            this.getDocument().add(p1);
        } catch (DocumentException de) {
            SysApoio.imp(de.getMessage());
        }
    }

    /**
     * imprime paragrafo de titulo
     */
    public void impTitulo(String texto) {
        try {
            texto = SysApoio.toLinereakSpaceFromLinebreakTab(texto);
            this.getDocument().add(this.sImpTitulo(texto));
        } catch (DocumentException de) {
            SysApoio.imp(de.getMessage());
        }
    }

    /**
     * imprime paragrafo de titulo
     */
    public Paragraph sImpTitulo(String texto) {
        texto = SysApoio.toLinereakSpaceFromLinebreakTab(texto);
        Paragraph p1 = new Paragraph(new Chunk(
                texto + "\n", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD)));
        //p.add(new Chunk("abcdefghijklmnopqrstuvwxyz", new Font(Font.ZAPFDINGBATS, 12, Font.BOLD)));
        p1.setAlignment(Element.ALIGN_CENTER);
        return (p1);
    }

    public void imp() {
        try {
            Paragraph p1 = new Paragraph(new Chunk(
                    "", FontFactory.getFont(FontFactory.TIMES, 12)));
            this.getDocument().add(p1);
        } catch (DocumentException de) {
            SysApoio.imp(de.getMessage());
        }
    }

    public void fechaPdf() {
        // step 5: we close the document
        this.getDocument().close();
    }

    public void escreveTabela(float[] qtColunas) {
        this.setTabelaAtiva(new PdfPTable(qtColunas));
        this.prepTabela(this.getTabelaAtiva());
    }

    public void escreveTabela(int qtColunas) {
        this.setTabelaAtiva(new PdfPTable(qtColunas));
        this.prepTabela(this.getTabelaAtiva());
    }

    public void escreveCelula(String texto, String filename) {
        texto = SysApoio.toLinereakSpaceFromLinebreakTab(texto);
        Image png = null;
        try {
            png = Image.getInstance(filename);
        } catch (BadElementException ex) {
            log.error(ex);
        } catch (IOException ex) {
            log.error(ex);
        }
        if (png == null) {
            log.error(filename + " - image not found.");
            return;
        }
        float width = png.getWidth();
        PdfPCell cell = new PdfPCell();
        prepCelula(cell);
        png.setWidthPercentage(66);
        cell.addElement(png);
        Paragraph para = new Paragraph(texto, FontFactory.getFont(FontFactory.TIMES, 12));
        cell.addElement(para);
        this.getTabelaAtiva().addCell(cell);
    }

    public void escreveCelula(String texto, String filename, boolean temp) {
        texto = SysApoio.toLinereakSpaceFromLinebreakTab(texto);
        PdfPTable tabela = new PdfPTable(1);
        tabela.setKeepTogether(true);
        tabela.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
        tabela.getDefaultCell().setVerticalAlignment(Element.ALIGN_TOP);
        tabela.getDefaultCell().setBorder(getBorder());
        tabela.getDefaultCell().setUseBorderPadding(false);

        Image png = null;
        try {
            png = Image.getInstance(filename);
        } catch (BadElementException ex) {
            log.error(ex);
        } catch (IOException ex) {
            log.error(ex);
        }
        if (png == null) {
            log.error(filename + " - image not found.");
            return;
        } else {
            PdfPCell cell = new PdfPCell();
            cell.addElement(png);
            png.setWidthPercentage(66);
            tabela.addCell(cell);
        }
        Paragraph para = new Paragraph(texto, FontFactory.getFont(FontFactory.TIMES, 12));
        tabela.addCell(para);
        this.getTabelaAtiva().addCell(tabela);
    }

    public void escreveCelula(String texto) {
//        PdfPCell cell =
//                new PdfPCell(new Paragraph(texto, FontFactory.getFont(FontFactory.TIMES, 12)));
        texto = SysApoio.toLinereakSpaceFromLinebreakTab(texto);
        this.getTabelaAtiva().addCell(new Paragraph(texto, FontFactory.getFont(FontFactory.TIMES, 12)));
    }

    public void fechaTabela() {
        try {
            this.getDocument().add(this.getTabelaAtiva());
            this.setTabelaAtiva(null);
        } catch (DocumentException ex) {
            log.error(ex);
        }
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

//    public void escreveImagem(String filename) {
    public void escreveImagem(BufferedImage imagem) {
        try {
//            Image imagem = Image.getInstance(filename);
            Image photo = Image.getInstance(imagem, null);
            //photo.setAlignment(Image.MIDDLE | Image.TEXTWRAP);
            if (photo.getWidth() > PageSize.A4.getWidth() - 50 || photo.getHeight() > PageSize.A4.getHeight() - 50) {
                photo.scaleToFit(PageSize.A4.getWidth() - 50, PageSize.A4.getHeight() - 50);
            }
            getDocument().add(photo);
        } catch (BadElementException ex) {
            log.error(ex);
        } catch (IOException ex) {
            log.error(ex);
        } catch (DocumentException ex) {
            log.error(ex);
        }
    }

    public void escreveCelulaImagem(BufferedImage imagem) {
        try {
            Image photo = Image.getInstance(imagem, null);
            photo.setAlignment(Image.TEXTWRAP);
//            photo.setBorder(Image.BOX);
//            photo.setBorderWidth(3);
            if (photo.getWidth() > PageSize.A4.getWidth() - 50 || photo.getHeight() > PageSize.A4.getHeight() - 50) {
                photo.scaleToFit(PageSize.A4.getWidth() - 50, PageSize.A4.getHeight() - 50);
            }
            PdfPCell cell;
            cell = new PdfPCell(photo);
            cell.setRowspan(99);
            getTabelaAtiva().addCell(cell);
        } catch (BadElementException ex) {
            log.error(ex);
        } catch (IOException ex) {
            log.error(ex);
        }
    }

    public void imp(int linha) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void imp(long linha) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void imp(java.util.List linha) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void novaPagina() {
        this.getDocument().newPage();
    }

    public PdfPTable getTabelaAtiva() {
        return tabelaAtiva;
    }

    public void setTabelaAtiva(PdfPTable tabelaAtiva) {
        this.tabelaAtiva = tabelaAtiva;
    }

    private void prepTabela(PdfPTable tabela) {
        tabela.setWidthPercentage(100F);
        tabela.setSpacingBefore(12F);
        tabela.setSpacingAfter(12F);
        tabela.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabela.setKeepTogether(true);
        this.prepCelula(tabela.getDefaultCell());
    }

    private void prepCelula(PdfPCell celula) {
        //Caracteristicas da celula default (quando adiciona apenas texto, sem declarar o cell)
        celula.setHorizontalAlignment(Element.ALIGN_LEFT);
        celula.setVerticalAlignment(Element.ALIGN_BASELINE);
        //celula.setGrayFill(0.8f);
        celula.setBorderColor(new BaseColor(255, 0, 0));
        //celula.setBackgroundColor(new Color(255, 0, 0));
        celula.setBorder(getBorder());
        //celula.setNoWrap(true);
        celula.setUseBorderPadding(true);
        celula.setPadding(2F);
    }

    public PdfPTable getSubTabelaAtiva() {
        return subTabelaAtiva;
    }

    public void setSubTabelaAtiva(PdfPTable subTabelaAtiva) {
        this.subTabelaAtiva = subTabelaAtiva;
    }

    public void escreveSubTabela(float[] qtColunas) {
        this.setSubTabelaAtiva(new PdfPTable(qtColunas));
        //this.prepTabela(this.getSubTabelaAtiva());
        this.getSubTabelaAtiva().setSpacingBefore(0F);
        this.getSubTabelaAtiva().setSpacingAfter(0F);
        this.getSubTabelaAtiva().getDefaultCell().setBorder(getBorder());
        this.getSubTabelaAtiva().setWidthPercentage(100F);
    }

    public void escreveSubTabela(int qtColunas) {
        this.setSubTabelaAtiva(new PdfPTable(qtColunas));
        //this.prepTabela(this.getSubTabelaAtiva());
        this.getSubTabelaAtiva().setSpacingBefore(0F);
        this.getSubTabelaAtiva().setSpacingAfter(0F);
        this.getSubTabelaAtiva().getDefaultCell().setBorder(getBorder());
        this.getSubTabelaAtiva().setWidthPercentage(100F);
    }

    public void fechaSubTabela() {
        this.getTabelaAtiva().addCell(this.getSubTabelaAtiva());
        this.setSubTabelaAtiva(null);
    }

    public void escreveSubCelula(String texto) {
        texto = SysApoio.toLinereakSpaceFromLinebreakTab(texto);
        this.getSubTabelaAtiva().addCell(new Paragraph(texto, FontFactory.getFont(FontFactory.TIMES, 12)));
    }

    public void escreveSubCelulaBold(String texto) {
        texto = SysApoio.toLinereakSpaceFromLinebreakTab(texto);
        this.getSubTabelaAtiva().addCell(new Paragraph(texto, FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    void escreveCelulaBold(String texto) {
        texto = SysApoio.toLinereakSpaceFromLinebreakTab(texto);
        this.getTabelaAtiva().addCell(new Paragraph(texto, FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
    }

    void escreveCelulaTitulo(String texto) {
        texto = SysApoio.toLinereakSpaceFromLinebreakTab(texto);
        PdfPCell cell;
        cell = new PdfPCell(new Paragraph(texto, FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD)));
        cell.setColspan(getTabelaAtiva().getNumberOfColumns());
        cell.setBorder(getBorder());
        getTabelaAtiva().addCell(cell);
    }

    /**
     * @return the border
     */
    public int getBorder() {
        return border;
    }

    /**
     * @param border the border to set
     */
    public void setBorder(int border) {
        this.border = border;
    }
}
