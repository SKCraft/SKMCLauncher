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

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.util.LauncherUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Swing utility methods.
 */
public final class SwingHelper {

    private static String[] monospaceFontNames = {
        "Consolas", "DejaVu Sans Mono", "Bitstream Vera Sans Mono", "Lucida Console"};
    
    private static boolean confirmResult;
    
    private SwingHelper() {
    }
    
    /**
     * Browse to a folder.
     * 
     * @param file the path
     * @param component the component
     */
    public static void browseDir(File file, Component component) {
        try {
            Desktop.getDesktop().browse(new URL("file://" + file.getAbsolutePath()).toURI());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(component, "Unable to open '" +
                    file.getAbsolutePath() + "'. Maybe it doesn't exist?",
                    "Open failed", JOptionPane.ERROR_MESSAGE);
        } catch (URISyntaxException e) {
        }
    }
    
    /**
     * Opens a URL.
     * 
     * @param url
     * @param component
     */
    public static void openURL(String url, Component component) {
        try {
            openURL(new URL(url), component);
        } catch (MalformedURLException e) {
        }        
    }
    
    /**
     * Opens a URL.
     * 
     * @param url
     * @param component
     */
    public static void openURL(URL url, Component component) {
        try {
            Desktop.getDesktop().browse(url.toURI());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(component, "Unable to open '" +
                    url + "'",
                    "Open failed", JOptionPane.ERROR_MESSAGE);
        } catch (URISyntaxException e) {
        }        
    }
    
    /**
     * Shows an error dialog.
     * 
     * <p>This can be called from a different thread from the event dispatch
     * thread, and it will be made thread-safe.</p>
     * 
     * @param component component
     * @param title title
     * @param message message
     */
    public static void showError(final Component component, 
            final String title, final String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        showError(component, title, message);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        
        String newMessage = message
                .replace(">", "&gt;")
                .replace("<", "&lt;")
                .replace("&", "&amp;");
        newMessage = "<html>" + newMessage;
        
        JOptionPane.showMessageDialog(
                component, newMessage, title, 
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Asks the user a yes or no question.
     * 
     * @param component the component
     * @param title the title
     * @param message the message
     * @return true if 'yes' was selected
     */
    public static boolean confirm(final Component component, 
            final String title, final String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        confirmResult = confirm(component, title, message);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            
            return confirmResult;
        }
        
        return JOptionPane.showConfirmDialog(
                component, message, title, JOptionPane.YES_NO_OPTION) == 0;
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
    public static void removeOpaqueness(Component ... components) {
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
     * @return font
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
            in = Launcher.class.getResourceAsStream(path);
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (IOException e) {
        } finally {
            LauncherUtils.close(in);
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
     * Try to set the default look and feel.
     */
    public static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
    }
    
    /**
     * Focus a component.
     * 
     * <p>The focus call happens in {@link SwingUtilities#invokeLater(Runnable)}.</p>
     * 
     * @param component the component
     */
    public static void focusLater(final Component component) {
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

    /**
     * Invoke, wait, and ignore errors.
     * 
     * @param runnable a runnable
     */
    public static void invokeAndWait(Runnable runnable) {
        try {
            SwingUtilities.invokeAndWait(runnable);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    
}
