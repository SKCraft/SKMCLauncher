/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010, 2011 Albert Pham <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.sk89q.skmcl.swing;

import com.sk89q.skmcl.Launcher;
import lombok.NonNull;
import lombok.extern.java.Log;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static com.sk89q.skmcl.util.SharedLocale._;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Swing utility methods.
 */
@Log
public final class SwingHelper {

    private static String[] monospaceFontNames = {
            "Consolas",
            "DejaVu Sans Mono",
            "Bitstream Vera Sans Mono",
            "Lucida Console"};

    private SwingHelper() {
    }
    
    /**
     * Open a system file browser window for the given path.
     * 
     * @param file the path
     * @param parentComponent the component from which to show any errors
     */
    public static void browseDir(@NonNull File file, @NonNull Component parentComponent) {
        try {
            Desktop.getDesktop().browse(
                    new URL("file://" + file.getAbsolutePath()).toURI());
        } catch (IOException e) {
            showErrorDialog(parentComponent,
                    _("errors.unableOpenDir", file.getAbsolutePath()),
                    _("errors.errorTitle"));
        } catch (URISyntaxException e) {
        }
    }
    
    /**
     * Opens a system web browser for the given URL.
     * 
     * @param url the URL
     * @param parentComponent the component from which to show any errors
     */
    public static void openURL(@NonNull String url, @NonNull Component parentComponent) {
        try {
            openURL(new URL(url), parentComponent);
        } catch (MalformedURLException e) {
        }        
    }
    
    /**
     * Opens a system web browser for the given URL.
     *
     * @param url the URL
     * @param parentComponent the component from which to show any errors
     */
    public static void openURL(URL url, Component parentComponent) {
        try {
            Desktop.getDesktop().browse(url.toURI());
        } catch (IOException e) {
            showErrorDialog(parentComponent,
                    _("errors.unableOpenURL", url.toString()),
                    _("errors.errorTitle"));
        } catch (URISyntaxException e) {
        }        
    }

    /**
     * Shows an popup error dialog, with potential extra details shown either immediately
     * or available on the dialog.
     *
     * @param parentComponent the frame from which the dialog is displayed, otherwise
     *                        null to use the default frame
     * @param message the message to display
     * @param title the title string for the dialog
     * @see #showMessageDialog(java.awt.Component, String, String, String, int) for details
     */
    public static void showErrorDialog(Component parentComponent, @NonNull String message,
                                       @NonNull String title) {
        showErrorDialog(parentComponent, message, title, null);
    }

    /**
     * Shows an popup error dialog, with potential extra details shown either immediately
     * or available on the dialog.
     *
     * @param parentComponent the frame from which the dialog is displayed, otherwise
     *                        null to use the default frame
     * @param message the message to display
     * @param title the title string for the dialog
     * @param throwable the exception, or null if there is no exception to show
     * @see #showMessageDialog(java.awt.Component, String, String, String, int) for details
     */
    public static void showErrorDialog(Component parentComponent, @NonNull String message,
                                       @NonNull String title, Throwable throwable) {
        String detailsText = null;

        // Get a string version of the exception and use that for
        // the extra details text
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            detailsText = sw.toString();
        }

        showMessageDialog(parentComponent, message, title,
                detailsText, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Show a message dialog using
     * {@link JOptionPane#showMessageDialog(java.awt.Component, Object, String, int)}.
     *
     * <p>The dialog will be shown from the Event Dispatch Thread, regardless of the
     * thread it is called from. In either case, the method will block until the
     * user has closed the dialog (or dialog creation fails for whatever reason).</p>
     *
     * @param parentComponent the frame from which the dialog is displayed, otherwise
     *                        null to use the default frame
     * @param message the message to display
     * @param title the title string for the dialog
     * @param messageType see {@link JOptionPane#showMessageDialog(java.awt.Component, Object, String, int)}
     *                    for available message types
     */
    public static void showMessageDialog(final Component parentComponent,
                                         @NonNull final String message,
                                         @NonNull final String title,
                                         final String detailsText,
                                         final int messageType) {

        if (SwingUtilities.isEventDispatchThread()) {
            // To force the label to wrap, convert the message to broken HTML
            String htmlMessage = "<html><div style=\"width: 350px\">" + message
                    .replace(">", "&gt;")
                    .replace("<", "&lt;")
                    .replace("&", "&amp;");

            JPanel panel = new JPanel(new BorderLayout(0, detailsText != null ? 10 : 0));

            // Add the main message
            panel.add(new JLabel(htmlMessage), BorderLayout.NORTH);

            // Add the extra details
            if (detailsText != null) {
                JTextArea textArea = new JTextArea(
                        _("errors.detailsForDeveloper", detailsText));
                textArea.setTabSize(2);
                textArea.setEditable(false);
                textArea.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);

                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(350, 150));
                panel.add(scrollPane, BorderLayout.CENTER);
            }

            JOptionPane.showMessageDialog(
                    parentComponent, panel, title, messageType);
        } else {
            // Call method again from the Event Dispatch Thread
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        showMessageDialog(
                                parentComponent, message, title,
                                detailsText, messageType);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Asks the user a binary yes or no question.
     * 
     * @param parentComponent the component
     * @param message the message to display
     * @param title the title string for the dialog
     * @return whether 'yes' was selected
     */
    public static boolean confirmDialog(final Component parentComponent,
                                        @NonNull final String message,
                                        @NonNull final String title) {
        if (SwingUtilities.isEventDispatchThread()) {
            return JOptionPane.showConfirmDialog(
                    parentComponent, message, title, JOptionPane.YES_NO_OPTION) ==
                    JOptionPane.YES_OPTION;
        } else {
            // Use an AtomicBoolean to pass the result back from the
            // Event Dispatcher Thread
            final AtomicBoolean yesSelected = new AtomicBoolean();

            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        yesSelected.set(confirmDialog(parentComponent, title, message));
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            
            return yesSelected.get();
        }
    }

    /**
     * Equalize the width of the given components.
     *
     * @param component component
     */
    public static void equalWidth(Component ... component) {
        double widest = 0;
        for (Component comp : component) {
            Dimension dim = comp.getPreferredSize();
            if (dim.getWidth() > widest) {
                widest = dim.getWidth();
            }
        }

        for (Component comp : component) {
            Dimension dim = comp.getPreferredSize();
            comp.setPreferredSize(new Dimension((int) widest, (int) dim.getHeight()));
        }
    }

    /**
     * Remove all the opaqueness of the given components and child components.
     * 
     * @param components list of components
     */
    public static void removeOpaqueness(@NonNull Component ... components) {
        for (Component component : components) {
            if (component instanceof JComponent) {
                JComponent jComponent = (JComponent) component;
                jComponent.setOpaque(false);
                removeOpaqueness(jComponent.getComponents());
            }
        }
    }
    
    /**
     * Get a supported monospace font.
     * 
     * @return a font
     */
    public static Font getMonospaceFont() {
        for (String fontName : monospaceFontNames) {
            Font font = Font.decode(fontName + "-11");
            if (!font.getFamily().equalsIgnoreCase("Dialog"))
                return font;
        }

        return new Font("Monospace", Font.PLAIN, 11);
    }

    /**
     * Try to read an embedded image.
     * 
     * @param path path
     * @return the image
     */
    public static BufferedImage readIconImage(String path) {
        InputStream in = null;
        try {
            in = SwingHelper.class.getResourceAsStream(path);
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (IOException e) {
        } finally {
            closeQuietly(in);
        }
        return null;
    }

    /**
     * Try to set the icon on a frame from an embedded image.
     * 
     * @param frame the frame
     * @param path path
     */
    public static void setIconImage(JFrame frame, String path) {
        BufferedImage image = readIconImage(path);
        if (image != null) {
            frame.setIconImage(image);
        }
    }

    /**
     * Try to set a safe look and feel.
     */
    public static void setSafeLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Throwable e) {
        }
    }

    /**
     * Try to set the default look and feel.
     */
    public static void setLookAndFeel() {
        try {
            UIManager.getLookAndFeelDefaults().put("ClassLoader",
                    Launcher.class.getClassLoader());
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
            System.setProperty("sun.awt.noerasebackground", "true");
            System.setProperty("substancelaf.windowRoundedCorners", "false");
            UIManager.setLookAndFeel("com.sk89q.skmcl.skin.LauncherLookAndFeel");
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to set LAF", e);
        }
    }
    
    /**
     * Focus a component.
     * 
     * <p>The focus call happens in {@link SwingUtilities#invokeLater(Runnable)}.</p>
     * 
     * @param component the component
     */
    public static void focusLater(@NonNull final Component component) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (component instanceof JTextComponent) {
                    ((JTextComponent) component).selectAll();
                }
                component.requestFocusInWindow();
            }
        });
    }
    
}
