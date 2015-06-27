/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence;

import baseLib.SysApoio;
import baseLib.SysProperties;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import model.Jogador;
import model.Partida;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author jmoura
 */
public class WebCounselorManager {

    private static final Log log = LogFactory.getLog(WebCounselorManager.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static WebCounselorManager instance;
    private static final String siteUrl = "http://clashlegends.com/PbmSite/%s.php";
    private final String counselorToken = "4vHZA0EfimmurFsLLXO6Aj9MXAmNk7fvB23b7x43";
    public static final int OK = 202;
    public static final int ERROR_GAMECLOSED = 403;
    public static final int ERROR_TURN = 406;
    public static final int ERROR_UNKOWN = 0;
    private int lastStatusCode;
    private String lastResponseString;

    private WebCounselorManager() {
    }

    public synchronized static WebCounselorManager getInstance() {
        if (instance == null) {
            log.debug("Criou instancia do WebManager.");
            instance = new WebCounselorManager();
        }
        return instance;
    }

    public int doSendViaPost(File attachment, Partida partida, String textBody) throws PersistenceException {
        Jogador jogador = partida.getJogadorAtivo();
        try {
            HttpClient client = new DefaultHttpClient();

            MultipartEntity entity = new MultipartEntity();
            // o primeiro parametro eh o nome do "campo" onde se espera enviar o arquivo. Deve
            // ser igual ao determinado no PHP. O segundo eh o arquivo local
            entity.addPart("userfile", new FileBody(attachment));
            entity.addPart("pToken", new StringBody(counselorToken));
            entity.addPart("pPartida", new StringBody(partida.getId() + ""));
            entity.addPart("pTurno", new StringBody(partida.getTurno() + ""));
            entity.addPart("pJogador", new StringBody(jogador.getId() + ""));
            entity.addPart("pJogadorLogin", new StringBody(jogador.getLogin()));
            entity.addPart("pJavaVersion", new StringBody(SysApoio.getVersionJava()));
            entity.addPart("pOsVersion", new StringBody(SysApoio.getVersionOs()));
            entity.addPart("pCounselorVersion", new StringBody(SysApoio.getVersionClash("version_counselor")));
            entity.addPart("pCommonsVersion", new StringBody(SysApoio.getVersionClash("version_commons")));
            entity.addPart("pScreenSize", new StringBody(SysApoio.getScreenSize()));
            if (SysProperties.getProps("SendOrderReceiptRequest", "1").equals("1")) {
                entity.addPart("pTextBody", new StringBody(textBody));
                entity.addPart("pPartidaName", new StringBody(partida.getNome()));
                entity.addPart("pJogadorEmail", new StringBody(jogador.getEmail()));
            }

            // aqui define a URL
            HttpPost post = new HttpPost(getUrl("CounselorUploadTurn"));
            post.setEntity(entity);

            // faz a chamada......
            log.debug("Executing request: " + post.getRequestLine());
            //String response = client.execute(post, new BasicResponseHandler());
            HttpResponse response = client.execute(post);
            setLastStatusCode(response.getStatusLine().getStatusCode());
            setLastResponseString(responseToString(response));
            //process responses
            if (response.getStatusLine().getStatusCode() == OK) {
                return OK;
            } else if (response.getStatusLine().getStatusCode() == ERROR_GAMECLOSED) {
                log.debug(getLastResponseString());
                return ERROR_GAMECLOSED;
            } else if (response.getStatusLine().getStatusCode() == ERROR_TURN) {
                log.debug(getLastResponseString());
                List<String> ret = new ArrayList<String>();
                ret.addAll(Arrays.asList(SysApoio.stringToArray(getLastResponseString())));
                setLastResponseString(ret.get(0));
                return ERROR_TURN;
            } else {
                log.error(response.getProtocolVersion());
                log.error(response.getStatusLine().getStatusCode());
                log.error(response.getStatusLine().getReasonPhrase());
                log.error(getLastResponseString());
            }
        } catch (URISyntaxException ex) {
            throw new PersistenceException("Can't connect to site (http://clashlegends.com/PbmSite/): " + ex.toString());
        } catch (IOException ex) {
            throw new PersistenceException("Can't read file to send.");
        }
        return ERROR_UNKOWN;
    }

    private URI getUrl(String webpage) throws URISyntaxException {
        return new URI(String.format(siteUrl, webpage));
    }

    private String responseToString(HttpResponse response) throws ParseException, IOException {
        return EntityUtils.toString(response.getEntity());
    }

    public int getLastStatusCode() {
        return lastStatusCode;
    }

    private void setLastStatusCode(int lastStatusCode) {
        this.lastStatusCode = lastStatusCode;
    }

    public String getLastResponseString() {
        return lastResponseString;
    }

    private void setLastResponseString(String lastResponseString) {
        this.lastResponseString = lastResponseString;
    }
}
