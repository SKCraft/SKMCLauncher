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

package com.sk89q.mclauncher;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.sk89q.mclauncher.util.SwingHelper;

public final class WebpagePanel extends JPanel {

    private static final Logger logger = 
            Logger.getLogger(WebpagePanel.class.getCanonicalName());
    private static final long serialVersionUID = -1280532243823315833L;
    
    private final WebpagePanel self = this;
    private final URL url;
    
    private JEditorPane documentView;
    private JProgressBar progressBar;
    
    public static WebpagePanel forURL(URL url, boolean lazy) {
        return new WebpagePanel(url, lazy);
    }
    
    public static WebpagePanel forHTML(String html) {
        return new WebpagePanel(html);
    }

    private WebpagePanel(URL url, boolean lazy) {
        this.url = url;
        
        setLayout(new BorderLayout());
        
        if (lazy) {
            setPlaceholder();
        } else {
            setDocument();
            update(url, documentView, progressBar);
        }
    }

    private WebpagePanel(String text) {
        this.url = null;
        
        setLayout(new BorderLayout());
        
        setDocument();
        setDisplay(documentView, progressBar, text);
    }
    
    private void setDocument() {
        JLayeredPane panel = new JLayeredPane();
        panel.setLayout(new WebpageLayoutManager());
        
        documentView = new JEditorPane();
        documentView.setEditable(false);
        documentView.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (e.getURL() != null) {
                        SwingHelper.openURL(e.getURL(), self);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(documentView);
        panel.add(scrollPane, new Integer(1));
        
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        panel.add(progressBar, new Integer(2));
        
        add(panel, BorderLayout.CENTER);
    }
    
    private void setPlaceholder() {
        JLayeredPane panel = new JLayeredPane();
        panel.setBorder(new CompoundBorder(
                BorderFactory.createEtchedBorder(), BorderFactory
                        .createEmptyBorder(4, 4, 4, 4)));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        final JButton showButton = new JButton("Load page");
        showButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        showButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showButton.setVisible(false);
                setDocument();
                update(url, documentView, progressBar);
            }
        });
        
        // Center the button vertically.
        panel.add(new Box.Filler(
                new Dimension(0, 0),
                new Dimension(0, 0),
                new Dimension(1000, 1000)));
        panel.add(showButton);
        panel.add(new Box.Filler(
                new Dimension(0, 0),
                new Dimension(0, 0),
                new Dimension(1000, 1000)));
        
        add(panel, BorderLayout.CENTER);
    }

    /**
     * Update the page. This has to be run in the Swing event thread.
     * 
     * @param url the URL
     * @param display component to display the page inside
     * @param progress progress bar to hide or show
     */
    private static void update(final URL url, final JEditorPane display, 
            final JProgressBar progress) {
        
        progress.setVisible(true);
        
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;

                try {
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setUseCaches(false);
                    conn.setDoInput(true);
                    conn.setDoOutput(false);
                    conn.setReadTimeout(5000);

                    conn.connect();

                    if (conn.getResponseCode() != 200) {
                        throw new IOException(
                                "Did not get expected 200 code, got "
                                        + conn.getResponseCode());
                    }

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(),
                                    "UTF-8"));

                    StringBuilder s = new StringBuilder();
                    char[] buf = new char[1024];
                    int len = 0;
                    while ((len = reader.read(buf)) != -1) {
                        s.append(buf, 0, len);
                    }
                    String result = s.toString();

                    setDisplay(display, progress, result);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to fetch page", e);
                    setError(display, progress,
                            "Failed to fetch page: " + e.getMessage());
                } finally {
                    if (conn != null)
                        conn.disconnect();
                    conn = null;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void setDisplay(final JEditorPane display,
            final JProgressBar progress, final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progress.setVisible(false);
                display.setContentType("text/html");
                display.setText(text);
                display.setCaretPosition(0);
            }
        });
    }

    private static void setError(final JEditorPane display,
            final JProgressBar progress, final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progress.setVisible(false);
                display.setContentType("text/plain");
                display.setText(text);
                display.setCaretPosition(0);
            }
        });
    }

}
