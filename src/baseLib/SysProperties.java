/*
 * SysProperties.java
 *
 * Created on 28 de Marco de 2007, 13:02
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package baseLib;

import java.io.*;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author gurgel
 */
public class SysProperties implements Serializable {

    private static final Log log = LogFactory.getLog(SysProperties.class);
    private static Properties props;
    private static SysProperties instance;
    private static final String propArqName = "properties.config";
    private static final String comentario = "Counselor config file\n"
            + "filtro.default=0|1 -> All|Own\n"
            + "SortAllCombos=0|1 -> Off|On\n"
            + "maximizeWindowOnStart = 0|1 -> normal|maximize\n"
            + "minimizeMapOnStart = 0|1 -> view|hide\n"
            + "saveDir =/folder/folder/, default folder to save orders (use / as opposed to \\).\n"
            + "loadDir =/folder/folder/, default folder to load results and orders (use / as opposed to \\).\n"
            + "autoLoad =/folders/file file name that you want for the Results to be loaded every time (use / as opposed to \\).\n"
            + "autoLoadActions =/folders/file file name for the Action that you want to be loaded every time (use / as opposed to \\).\n"
            //            + "doneDir = server side use, no effect on the Counselor\n"
            //            + "local = server side use, no effect on the Counselor\n"
            //            + "database = server side use, no effect on the Counselor\n"
            + "language = en/pt/it, to define the GUI language\n"
            + "splitSize = 300, define the size of the left side screen split.\n"
            + "TableColumnAdjust = 0|1 -> Yes|No, adjust columns during \"Load Action\".\n"
            + "CopyActionsPopUp = 0|1 -> Yes|No, displays a popup when copying Actions to clipboard.\n"
            + "CopyActionsOrder = 0|1 -> Displays the list of actions per character.\n"
            + "HexTagStyle = 0-3, changes the Hex Tag Style.\n"
            + "HexTagFrame = 0|1, changes the Hex Tag Border Style (0 or 1).\n"
            + "KeepPopupOpen = 0|1 -> Yes|No, open multiple hex's info popups.\n"
            + "MyEmail=user@domain, define your email address.\n"
            + "OverrideElimination=0|1 -> allows you to send actions past elimination.\n"
            + "SendOrderConfirmationPopUp=0|1 - Show pop-up message with confirmation or not.\n"
            + "SendOrderReceiptRequest=0|1 - Request site to send a confirmation receipt or not.\n"
            + "ShowArmyMovPath=0|1|2 -> allows you to see all possible movement paths for an army(1) or navy(2). (0) disable it.\n"
            + "MapTiles = 2d | 2a | 3d, changes the basic hex terrain tiles.\n"
            + "AutoMoveNextAction = 0|1, changes the behavior entering actions. If =1, then move to next available slot.\n"
            + "mail.smtp.server=smtp.myserver.com, smtp server name to be used.\n"
            + "mail.smpt.port=25, smtp port to be used. Only port 25 is supported right now.\n"
            + "mail.smtp.user=myuser, username for smtp authentication.\n"
            + "mail.smtp.passwd=my password, password for smtp authentication\n"
            + "\n";

    public static boolean isSet(String key) {
        return !getProps().getProperty(key, "SBRIFTS").equals("SBRIFTS");
    }

    private void setPropDefault() {
        getProps().setProperty("filtro.default", "0");
        getProps().setProperty("TableColumnAdjust", "0");
        getProps().setProperty("maximizeWindowOnStart", "1");
        getProps().setProperty("minimizeMapOnStart", "0");
        getProps().setProperty("saveDir", "/tmpbm/xml/");
        getProps().setProperty("loadDir", "/tmpbm/saves/");
        //getProps().setProperty("autoLoad", "partida_62_3.lucio.rr.egf");
//        getProps().setProperty("doneDir", "/tmpbm/xml/Done/");
//        getProps().setProperty("local", "Unknow"); //Desenv?
//        getProps().setProperty("database", "ProdTeste");
//        getProps().setProperty("tempZipFiles", "1");
        getProps().setProperty("language", "en");
        getProps().setProperty("splitSize", "660");
        getProps().setProperty("CopyActionsPopUp", "1");
        getProps().setProperty("CopyActionsOrder", "1");
        getProps().setProperty("MyEmail", "none");
        getProps().setProperty("KeepPopupOpen", "0");
        getProps().setProperty("HexTagStyle", "0");
        getProps().setProperty("HexTagFrame", "0");
        getProps().setProperty("ShowArmyMovPath", "1");
        getProps().setProperty("MapTiles", "2d");
        getProps().setProperty("KeepPopupOpen", "0");
        getProps().setProperty("AutoMoveNextAction", "1");
        getProps().setProperty("SendOrderConfirmationPopUp", "1");
        getProps().setProperty("SendOrderReceiptRequest", "1");
    }

    /**
     * Creates a new instance of SysProperties
     */
    private SysProperties() {
        props = new Properties();
        if (!this.carrega()) {
            this.novo(true);
        }
    }

    public synchronized static SysProperties getInstance() {
        if (SysProperties.instance == null) {
            SysProperties.instance = new SysProperties();
        }
        return SysProperties.instance;
    }

    private static Properties getProps() {
        if (props == null) {
            SysProperties sysProperties = new SysProperties();
        }
        return props;
    }

    public static String getProps(String key) {
        return getProps().getProperty(key);
    }

    public static String getProps(String key, String defaultValue) {
        return getProps().getProperty(key, defaultValue);
    }

    /**
     * para ler um arquivo de propriedades
     *
     */
    private boolean carrega() {
        boolean ret = false;
        try {
            InputStream propIn = new FileInputStream(
                    new File(getPropArq()));
            getProps().load(propIn);
            ret = true;
        } catch (IOException e) {
            //Criar arquivo de configuracao, arquivo nao encontrado
            ret = false;
        }
        return (ret);
    }

    private boolean novo(boolean novo) {
        boolean ret = false;
        //para gravar um arquivo de propriedades
        try {
            log.info("Criando arquivo de configuracao");
            OutputStream propFile = new FileOutputStream(new File(getPropArq()));
            if (novo) {
                this.setPropDefault();
            }
            getProps().store(propFile, comentario);
            ret = true;
        } catch (IOException e) {
            log.error("Erro criando arquivo de configuracao.");
        }
        return (ret);
    }

    private String getPropArq() {
        return propArqName;
    }

    public void setProp(String key, String value) {
        log.info("Salvando arquivo de configuracao");
        getProps().setProperty(key, value);
        try {
            OutputStream propFile = new FileOutputStream(new File(getPropArq()));
            getProps().store(propFile, comentario);
        } catch (IOException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    public Properties loadPropertiesFile(String fileName) {
        Properties properties = new Properties();
        try {
            InputStream propIn = new FileInputStream(new File(fileName));
            properties.load(propIn);
        } catch (IOException e) {
            //Arquivo nao encontrado
        }
        return properties;
    }
}
