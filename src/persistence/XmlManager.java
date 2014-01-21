/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence;

import baseLib.SysApoio;
import baseLib.SysProperties;
import com.thoughtworks.xstream.XStream;
//import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Singleton para gerenciar o xml que deve ser único.
 *
 * @author gurgel
 */
public class XmlManager implements Serializable {

    private static final Log log = LogFactory.getLog(XmlManager.class);
    private static final BundleManager label = SettingsManager.getInstance().getBundleManager();
    private static XmlManager instance;

    private XmlManager() {
    }

    public synchronized static XmlManager getInstance() {
        if (instance == null) {
            log.debug("Criou instancia do XmlManager.");
            instance = new XmlManager();
        }
        return instance;
    }

    public Object get(File inFile) throws PersistenceException {
        String inFileName = inFile.getName();
        log.debug("Abrindo arquivo: " + inFile.getName());
        InputStream is = null;
        InputStreamReader reader = null;
        try {
            File workFile = null;
            //Verifica se eh o XML ou o Gzip(egf)
            if (getExtension(inFileName).equalsIgnoreCase("egf")) {
                workFile = ZipManager.getInstance().doUncompressGzip(inFile);
            } else {
                workFile = inFile;
            }

            is = new BufferedInputStream(new FileInputStream(workFile));
            reader = new InputStreamReader(is, "UTF-8");
            XStream xstream = new XStream();
            return xstream.fromXML(reader);

//            XStream xstream = new XStream();
//            reader = xstream.createObjectInputStream(new FileInputStream(file));
//            return xstream.fromXML(reader);
        } catch (FileNotFoundException ex) {
            throw new PersistenceException(label.getString("ARQUIVO.NAO.ENCONTRADO") + inFile.getAbsolutePath());
        } catch (UnsupportedEncodingException ex) {
            throw new PersistenceException(label.getString("ARQUIVO.CORROMPIDO") + inFile.getAbsolutePath());
        } catch (IOException e) {
            throw new PersistenceException(label.getString("IO.ERROR"));
        } catch (PersistenceException e) {
            throw new PersistenceException(e.getMessage());
        } catch (com.thoughtworks.xstream.converters.ConversionException ex) {
            log.error(ex);
            throw new PersistenceException(
                    String.format(label.getString("ARQUIVO.INCOMPATIVEL"),
                    inFile.getAbsolutePath()));
        } catch (Exception e) {
            throw new PersistenceException(e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                log.error(ex);
            }
        }
    }

    /**
     * Cria o arquivo temporario para gerar o XML Chama o Zip para gerar o
     * arquivo final
     *
     * @param world
     * @param finalFile
     * @throws PersistenceException
     */
    public void save(Object world, File finalFile) throws PersistenceException {
        log.debug("Gravando XML. File: " + finalFile.getPath());
        String msgBuild = SysApoio.sprintf("commonsBuild=%s",
                SysApoio.getVersion("version_commons"));
        try {
            //cria temp file para o XML
            File tempFile;
            //verifica se o properties esta definindo que os arquivos temporarios devem ser apagados ou nao(debug?)
            if (SysProperties.getProps("tempZipFiles", "1").equals("1")) {
                String tempFileName = finalFile.getName();
                if (getExtension(tempFileName).equalsIgnoreCase("egf")) {
                    tempFileName = getFileName(tempFileName);
                }
                tempFile = File.createTempFile(tempFileName, null);
                tempFile.deleteOnExit();
            } else {
                String tempFileName = finalFile.getAbsolutePath();
                if (getExtension(tempFileName).equalsIgnoreCase("egf")) {
                    tempFileName = getFileName(tempFileName);
                }
                tempFile = new File(tempFileName);
            }
            FileWriter fw = new FileWriter(tempFile);

            //XStream xstream = new XStream(new DomDriver());
            XStream xstream = new XStream();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Writer writer = new OutputStreamWriter(outputStream, "UTF-8"); //UTF-8  //"ISO-8859-1"
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
            writer.write("<!-- " + msgBuild + "-->\n");
            xstream.toXML(world, writer);
            String xml = outputStream.toString("UTF-8");

            fw.write(xml);
            fw.close();
            ZipManager.getInstance().doCompressGzip(tempFile, finalFile);
            log.info("Arquivo gravado:" + finalFile.getAbsolutePath());
        } catch (IOException ex) {
            log.error("Problemas com o arquivo...", ex);
            throw new PersistenceException(ex);
        }
    }
//
//    private Object getOld(File file) throws PersistenceException {
//        log.info("Carregando " + file.getName());
//        Reader fileReader = null;
//        try {
//            // XStream 'e o nosso leitor de XML to Bean
//            XStream xstream = new XStream(new DomDriver("UTF-8"));
//            fileReader = new FileReader(file);
//            return xstream.fromXML(fileReader);
//        } catch (FileNotFoundException ex) {
//            throw new PersistenceException(label.getString("ARQUIVO.NAO.ENCONTRADO"), ex);
//        } catch (Exception ex) {
//            throw new PersistenceException(label.getString("ARQUIVO.CORROMPIDO"), ex);
//        } finally {
//            try {
//                if (fileReader != null) {
//                    fileReader.close();
//                }
//            } catch (IOException ex) {
//                log.error(ex);
//            }
//        }
//    }
//
//    private void saveOld(Object world, File appFile) throws PersistenceException {
//        log.info("Gravando XML. File: " + appFile.getPath());
//        FileWriter fw = null;
//        try {
//            XStream xstream = new XStream(new DomDriver("UTF-8"));
//            fw = new FileWriter(appFile);
//            fw.write(xstream.toXML(world));
//            log.info("Arquivo gravado:" + appFile.getAbsolutePath());
//        } catch (IOException ex) {
//            log.error("Problemas com o arquivo...", ex);
//            throw new PersistenceException(ex);
//        } finally {
//            try {
//                if (fw != null) {
//                    fw.close();
//                }
//            } catch (IOException ex) {
//                log.error("texto...", ex);
//            }
//        }
//    }

    /**
     * Used to extract and return the extension of a given file.
     *
     * @param f Incoming file to get the extension of
     * @return <code>String</code> representing the extension of the incoming
     * file.
     */
    private static String getExtension(String f) {
        String ext = "";
        int i = f.lastIndexOf('.');

        if (i > 0 && i < f.length() - 1) {
            ext = f.substring(i + 1);
        }
        return ext;
    }

    /**
     * Used to extract the filename without its extension.
     *
     * @param f Incoming file to get the filename
     * @return <code>String</code> representing the filename without its
     * extension.
     */
    private static String getFileName(String f) {
        String fname = "";
        int i = f.lastIndexOf('.');

        if (i > 0 && i < f.length() - 1) {
            fname = f.substring(0, i);
        }
        return fname;
    }

    public String toXml(Object object) {
        XStream xstream = new XStream();
        return xstream.toXML(object);
    }

    public Object fromXml(String xml) {
        try {
            if (xml != null) {
                XStream xstream = new XStream();
                return xstream.fromXML(xml);
            }
        } catch (com.thoughtworks.xstream.io.StreamException e) {
            //ignore the empty stream, return null
        }
        return null;
    }
}
