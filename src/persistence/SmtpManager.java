/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistence;

import baseLib.SysProperties;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * sendfile will create a multipart message with the second block of the message
 * being the given file.<p>
 *
 * This demonstrates how to use the FileDataSource to send a file via mail.<p>
 *
 * usage: <code>java sendfile <i>to from smtp file true|false</i></code> where
 * <i>to</i> and <i>from</i> are the destination and origin email addresses,
 * respectively, and <i>smtp</i> is the hostname of the machine that has smtp
 * server running. <i>file</i> is the file to send. The next parameter either
 * turns on or turns off debugging during sending.
 *
 * @author	Christopher Cotton
 */
public class SmtpManager implements Serializable {

    //Connection mandatory to be set.
    private boolean gmail = false;
    private String from = "NA";
    private String hostSmtp = "smtp.pobox.com";
    private String loginNameSmtp = "gurgel@pobox.com";
    private String pwdSmtp = "raistlin";
    //ask the user
    private String toMain = "clashoflegends.thegame@gmail.com";
    private final List<InternetAddress> toCcList = new ArrayList<InternetAddress>();
    //prepared by the application
    private final List<File> attachmentList = new ArrayList<File>();
    private String body = "Sending a file.\n";
    private String subject = "Sending a file 5 + timestamp!";
    //fixed/hidden
    private String protocol = "smtp";
    //former local
    private final boolean mailDebug = false;
    private Session session;
    private Transport transport;
    private MimeMessage msg;
    //other
    private static final Log log = LogFactory.getLog(SmtpManager.class);
    private static final BundleManager label = SettingsManager.getInstance().getBundleManager();

    public SmtpManager() {
    }

    public void clear() {
        toMain = "NA";
        toCcList.clear();
        attachmentList.clear();
        body = "NA";
        subject = "NA";
    }

    public boolean open() {
        if (session != null) {
            return true;
        }
        // create some properties and get the default Session
        Authenticator authenticator = null;
        Properties props = System.getProperties();
        props.put("mail.smtp.host", getHostSmtp());
        props.put("mail." + getProtocol() + ".auth", "true");
        if (isGmail()) {
            setFrom("clashoflegends.thegame@gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.starttls.enable", "true");
            authenticator = new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(getLoginNameSmtp(), getPwdSmtp());
                }
            };
        }
        session = Session.getInstance(props, authenticator);
        session.setDebug(mailDebug);
        return session != null;
    }

    private boolean createMsg() throws PersistenceException {
        boolean ret = false;
        try {
            // create a message
            msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(getFrom()));
            //InternetAddress[] address = {new InternetAddress(toMain), new InternetAddress(getToCc())};
            List<InternetAddress> addressList = new ArrayList();
            addressList.add(new InternetAddress(toMain));
            if (!getToCcList().isEmpty()) {
                addressList.addAll(getToCcList());
            }
            msg.setRecipients(Message.RecipientType.TO, addressList.toArray(new InternetAddress[0]));
            msg.setSubject(getSubject());

            // create the Multipart and add its parts to it
            Multipart mp = new MimeMultipart();

            // create and fill the first message part
            MimeBodyPart mbp1 = new MimeBodyPart();
            mbp1.setText(getBody());
            mp.addBodyPart(mbp1);

            // create the second message part
            if (!getAttachmentList().isEmpty()) {
                for (File file : getAttachmentList()) {
                    MimeBodyPart mbp2 = new MimeBodyPart();
                    mbp2.attachFile(file);
                    mp.addBodyPart(mbp2);
                }
            } else {
                MimeBodyPart mbp2 = new MimeBodyPart();
                mbp2.setText(label.getString("MISSING.ATTACHMENT"));
                mp.addBodyPart(mbp2);
            }

            // add the Multipart to the message
            msg.setContent(mp);

            // set the Date: header
            msg.setSentDate(new Date());

            msg.saveChanges();
            ret = true;

        } catch (FileNotFoundException ex) {
            throw new PersistenceException(ex.getMessage());
        } catch (MessagingException mex) {
            //log.fatal(mex);
            Exception ex = null;
            if ((ex = mex.getNextException()) != null) {
                throw new PersistenceException(ex.getMessage());
            }
        } catch (IOException ex) {
            throw new PersistenceException(ex.getMessage());
        }
        return ret;
    }

    public boolean sendMsg() throws PersistenceException {
        boolean ret = createMsg();

        // send the message
        try {

            transport = session.getTransport(getProtocol());
            transport.connect(getLoginNameSmtp(), getPwdSmtp());
            transport.sendMessage(msg, msg.getAllRecipients());
            ret = true;

        } catch (SendFailedException ex) {
            Exception mex = null;
            if ((mex = ex.getNextException()) != null) {
                throw new PersistenceException(mex.getMessage());
            } else {
                throw new PersistenceException(ex.getMessage());
            }
        } catch (MessagingException mex) {
            //log.fatal(mex);
            Exception ex = null;
            if ((ex = mex.getNextException()) != null) {
                throw new PersistenceException(ex.getMessage());
            } else {
                throw new PersistenceException(mex.getMessage());
            }
        } catch (Exception ex) {
            log.info("Erro no envio SmtpManager. Verifique espacos no email.", ex);
        } finally {
            clear();
        }
        return ret;
    }

    public boolean close() throws PersistenceException {
        try {
            transport.close();
        } catch (NullPointerException ex) {
            //transport not initialized, just ignore.
        } catch (MessagingException mex) {
            //log.fatal(mex);
            Exception ex = null;
            if ((ex = mex.getNextException()) != null) {
                throw new PersistenceException(ex.getMessage());
            }
        }
        return true;
    }

    public boolean sendCounselor() throws PersistenceException {
        boolean ret = false;
        try {
            doLoadSmtpProperties();
            // create some properties and get the default Session
            Authenticator authenticator = null;
            Properties props = System.getProperties();
            props.put("mail.smtp.host", getHostSmtp());
            props.put("mail." + getProtocol() + ".auth", "true");
            session = Session.getInstance(props, authenticator);
            session.setDebug(mailDebug);
        } catch (NullPointerException ex) {
            log.fatal(ex, ex.fillInStackTrace());
            throw new PersistenceException(ex);
        }
        try {
            // create a message
            msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(getFrom()));
            //InternetAddress[] address = {new InternetAddress(toMain), new InternetAddress(getToCc())};
            List<InternetAddress> addressList = new ArrayList();
            addressList.add(new InternetAddress(toMain));
            if (!getToCcList().isEmpty()) {
                addressList.addAll(getToCcList());
            }
            msg.setRecipients(Message.RecipientType.TO, addressList.toArray(new InternetAddress[0]));
            msg.setSubject(getSubject());

            // create the Multipart and add its parts to it
            Multipart mp = new MimeMultipart();

            // create and fill the first message part
            MimeBodyPart mbp1 = new MimeBodyPart();
            mbp1.setText(getBody());
            mp.addBodyPart(mbp1);

            // create the second message part
            if (!getAttachmentList().isEmpty()) {
                for (File file : getAttachmentList()) {
                    MimeBodyPart mbp2 = new MimeBodyPart();
                    mbp2.attachFile(file);
                    mp.addBodyPart(mbp2);
                }
            } else {
                MimeBodyPart mbp2 = new MimeBodyPart();
                mbp2.setText(label.getString("MISSING.ATTACHMENT"));
                mp.addBodyPart(mbp2);
            }

            // add the Multipart to the message
            msg.setContent(mp);

            // set the Date: header
            msg.setSentDate(new Date());

            // send the message
            transport = session.getTransport(getProtocol());
            try {
                transport.connect(getLoginNameSmtp(), getPwdSmtp());
                transport.sendMessage(msg, msg.getAllRecipients());
                ret = true;

            } catch (SendFailedException ex) {
                Exception mex = null;
                if ((mex = ex.getNextException()) != null) {
                    throw new PersistenceException(mex.getMessage());
                } else {
                    throw new PersistenceException(ex.getMessage());
                }
            } catch (Exception ex) {
                log.info(ex);
            } finally {
                transport.close();
            }

            //done
        } catch (FileNotFoundException ex) {
            throw new PersistenceException(ex.getMessage());
        } catch (MessagingException mex) {
            //log.fatal(mex);
            Exception ex = null;
            if ((ex = mex.getNextException()) != null) {
                throw new PersistenceException(ex.getMessage());
            }
        } catch (IOException ex) {
            throw new PersistenceException(ex.getMessage());
        }
        return ret;
    }

    private List<InternetAddress> getToCcList() {
        return toCcList;
    }

    public void addToCc(String toCc) throws AddressException {
        this.toCcList.add(new InternetAddress(toCc));
    }

    public void addToCcOptional(String toCc) {
        try {
            this.toCcList.add(new InternetAddress(toCc));
        } catch (AddressException ex) {
            log.fatal(ex);
        }
    }

    public void setToMain(String toMain) {
        this.toMain = toMain;
    }

    /**
     * @return the from
     */
    public String getFrom() {
        return from;
    }

    /**
     * @param from the from to set
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * @return the body
     */
    public String getBody() {
        return body;
    }

    /**
     * @param body the body to set
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * @return the subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @param subject the subject to set
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * @return the attachmentList
     */
    public List<File> getAttachmentList() {
        return attachmentList;
    }

    /**
     * Use this for multiple attachements for now.
     *
     * @param attachmentList the attachmentList to set
     */
    public void addAttachment(File attachment) {
        this.attachmentList.add(attachment);
    }

    /**
     * @return the hostSmtp
     */
    public String getHostSmtp() {
        return hostSmtp;
    }

    /**
     * @param hostSmtp the hostSmtp to set
     */
    public void setHostSmtp(String hostSmtp) {
        this.hostSmtp = hostSmtp;
    }

    /**
     * @return the loginNameSmtp
     */
    public String getLoginNameSmtp() {
        return loginNameSmtp;
    }

    /**
     * @param loginNameSmtp the loginNameSmtp to set
     */
    public void setLoginNameSmtp(String loginNameSmtp) {
        this.loginNameSmtp = loginNameSmtp;
    }

    /**
     * @return the pwdSmtp
     */
    public String getPwdSmtp() {
        return pwdSmtp;
    }

    /**
     * @param pwdSmtp the pwdSmtp to set
     */
    public void setPwdSmtp(String pwdSmtp) {
        this.pwdSmtp = pwdSmtp;
    }

    /**
     * @return the isGmail
     */
    public boolean isGmail() {
        return gmail;
    }

    /**
     * @param isGmail the isGmail to set
     */
    public void setGmail(boolean isGmail) {
        this.gmail = isGmail;
    }

    private void doLoadSmtpProperties() {
        if (SysProperties.isSet("mail.smtp.server")) {
            setHostSmtp(SysProperties.getProps("mail.smtp.server"));
            setLoginNameSmtp(SysProperties.getProps("mail.smtp.user"));
            setPwdSmtp(SysProperties.getProps("mail.smtp.passwd"));
        }
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
