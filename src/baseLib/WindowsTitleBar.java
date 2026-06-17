package baseLib;

import com.formdev.flatlaf.ui.FlatNativeWindowsLibrary;
import com.formdev.flatlaf.util.SystemInfo;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Makes the native Windows title bar of every window follow the Windows app-mode (dark/light) setting
 * instead of the FlatLaf theme.
 * <p>
 * FlatLaf forces the native title bar to match its own theme (light title bar on a light theme even
 * when Windows is in dark mode). That made Counselor the odd one out next to native apps (Notepad++,
 * Explorer, Judge) whose title bars follow the system. This re-applies the system setting after each
 * window opens, so Counselor behaves like a native Windows app regardless of the active FlatLaf theme.
 * <p>
 * No-op on non-Windows or when the FlatLaf native library is unavailable.
 */
public final class WindowsTitleBar {

    private static final Log log = LogFactory.getLog(WindowsTitleBar.class);
    private static Boolean systemDark; // cached result of the registry probe

    private WindowsTitleBar() {
    }

    /**
     * Install a global listener that applies the Windows system title-bar mode to every window as it
     * opens (after FlatLaf has set its theme-based value, so this wins). Call once at startup.
     */
    public static void installSystemTitleBarTracking() {
        if (!SystemInfo.isWindows) {
            return;
        }
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent e) {
                if (e.getID() == WindowEvent.WINDOW_OPENED && e.getSource() instanceof Window) {
                    apply((Window) e.getSource());
                }
            }
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    private static void apply(Window w) {
        try {
            if (!FlatNativeWindowsLibrary.isLoaded()) {
                return; // native border not active (e.g. non-FlatLaf L&F) - leave the OS title bar alone
            }
            long hwnd = FlatNativeWindowsLibrary.getHWND(w);
            if (hwnd != 0) {
                FlatNativeWindowsLibrary.dwmSetWindowAttributeBOOL(hwnd,
                        FlatNativeWindowsLibrary.DWMWA_USE_IMMERSIVE_DARK_MODE, isSystemDark());
            }
        } catch (Throwable t) {
            log.debug("Could not set system title-bar mode: " + t);
        }
    }

    private static boolean isSystemDark() {
        if (systemDark == null) {
            systemDark = readSystemDark();
        }
        return systemDark;
    }

    /**
     * Reads HKCU\...\Themes\Personalize\AppsUseLightTheme: REG_DWORD 0 = dark apps, 1 = light apps.
     * Defaults to light (false) on any failure.
     */
    private static boolean readSystemDark() {
        try {
            Process p = new ProcessBuilder("reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme").redirectErrorStream(true).start();
            boolean dark = false;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("AppsUseLightTheme")) {
                        dark = line.trim().endsWith("0x0"); // 0x0 = apps use dark mode
                    }
                }
            }
            p.waitFor();
            return dark;
        } catch (Exception e) {
            log.debug("Could not read Windows app theme: " + e);
            return false;
        }
    }
}
