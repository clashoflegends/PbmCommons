/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.components;

import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author jmoura
 */
public final class ButtonLink extends JLabel {

    private static final long serialVersionUID = 8273875024682878518L;
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private String text;
    private URI uri;

    public ButtonLink(String text, URI uri) {
        super();
        setup(text, uri);
    }

    public ButtonLink(String text, String uri) {
        super();
        URI oURI;
        try {
            oURI = new URI(uri);
        } catch (URISyntaxException e) {
            // converts to runtime exception for ease of use
            // if you cannot be sure at compile time that your
            // uri is valid, construct your uri manually and
            // use the other constructor.
            throw new RuntimeException(e);
        }
        setup(text, oURI);
    }

    public void setup(String t, URI u) {
        text = t;
        uri = u;
        setText(text);
        setToolTipText(uri.toString());
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                open(uri);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setText(text, false);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setText(text, true);
            }
        });
    }

    @Override
    public void setText(String text) {
        setText(text, true);
    }

    public void setText(String text, boolean ul) {
        String link = ul ? "<u>" + text + "</u>" : text;
        super.setText("<html><span style=\"color: #000099;\">"
                + link + "</span></html>");
        this.text = text;
    }

    public String getRawText() {
        return text;
    }

    private static void open(URI uri) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(uri);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        labels.getString("BUTTONLINK.ERROR.SYSTEM"),
                        labels.getString("BUTTONLINK.ERROR.TITLE"),
                        JOptionPane.WARNING_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null,
                    labels.getString("BUTTONLINK.ERROR.JAVA"),
                    labels.getString("BUTTONLINK.ERROR.TITLE"),
                    JOptionPane.WARNING_MESSAGE);
        }
    }
}
