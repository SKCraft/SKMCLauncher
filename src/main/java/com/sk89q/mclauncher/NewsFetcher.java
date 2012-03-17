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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JEditorPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.sk89q.mclauncher.config.Constants;

/**
 * Fetches news.
 * 
 * @author sk89q
 */
public class NewsFetcher {

    private static final Logger logger = Logger.getLogger(NewsFetcher.class.getCanonicalName());
    
    private NewsFetcher() {
    }
    
    /**
     * Update the news. This has to be run in the Swing event thread.
     * 
     * @param display component to display the news inside
     * @param progress progress bar to hide or show
     */
    public static void update(final JEditorPane display, final JProgressBar progress) {
        progress.setVisible(true);
        
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                        
                try {
                    conn = (HttpURLConnection) Constants.NEWS_URL.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setUseCaches(false);
                    conn.setDoInput(true);
                    conn.setDoOutput(false);
                    conn.setReadTimeout(5000);

                    conn.connect();
                    
                    if (conn.getResponseCode() != 200) {
                        throw new IOException("Did not get expected 200 code, got " + conn.getResponseCode());
                    }
                    
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

                    StringBuilder s = new StringBuilder();
                    char[] buf = new char[1024];
                    int len = 0;
                    while ((len = reader.read(buf)) != -1) {
                        s.append(buf, 0, len);
                    }
                    String result = s.toString();
                    
                    setDisplay(display, progress, result);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to fetch news", e);
                    setError(display, progress, "Failed to fetch news: " + e.getMessage());
                } finally {
                    if (conn != null) conn.disconnect();
                    conn = null;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    private static void setDisplay(final JEditorPane display, final JProgressBar progress, final String text) {
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
    
    private static void setError(final JEditorPane display, final JProgressBar progress, final String text) {
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
