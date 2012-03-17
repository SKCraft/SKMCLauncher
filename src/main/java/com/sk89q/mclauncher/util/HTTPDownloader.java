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

package com.sk89q.mclauncher.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.EventObject;
import javax.swing.event.EventListenerList;

import com.sk89q.mclauncher.DownloadListener;
import com.sk89q.mclauncher.DownloadProgressEvent;


/**
 * Used for downloading files via HTTP.
 * 
 * @author sk89q
 */
public class HTTPDownloader {

    private int timeout = 10000;
    private OutputStream output;
    private URL url;
    private long length;
    private long readLength;
    private String etag;
    private HttpURLConnection conn;
    private volatile boolean running = true;
    private String etagCheck = null;
    private MessageDigest digest;
    private EventListenerList listenerList = new EventListenerList();
    
    /**
     * Construct the downloader instance.
     * 
     * @param url url to download
     * @param output output stream to write to
     */
    public HTTPDownloader(URL url, OutputStream output) {
        this.url = url;
        this.output = output;
    }
    
    /**
     * Gets the HTTP connection.
     * 
     * @return http connection
     */
    public HttpURLConnection getConnection() {
        if (conn == null) {
            throw new IllegalArgumentException("Connection does not currently exist");
        }
        return conn;
    }
    
    /**
     * Register listener for download events.
     * 
     * @param l listener
     */
    public void addDownloadListener(DownloadListener l) {
        listenerList.add(DownloadListener.class, l);
    }

    /**
     * Unregister listener for download events.
     * 
     * @param l listener
     */
    public void removeDownloadListener(DownloadListener l) {
        listenerList.remove(DownloadListener.class, l);
    }
    
    /**
     * Fire a connection start event.
     */
    private void fireConnectionStarted() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((DownloadListener) listeners[i + 1]).connectionStarted(new EventObject(this));
        }
    }
    
    /**
     * Fire a length known event.
     */
    private void fireLengthKnown() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((DownloadListener) listeners[i + 1]).lengthKnown(new EventObject(this));
        }
    }
    
    /**
     * Fire a download progress event.
     * 
     * @param downloaded number of bytes downloaded so far
     */
    private void fireDownloadProgress(long downloaded) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((DownloadListener) listeners[i + 1]).downloadProgress(new DownloadProgressEvent(this, downloaded));
        }
    }
    
    /**
     * Fire a download completed event.
     */
    private void fireDownloadCompleted() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((DownloadListener) listeners[i + 1]).downloadCompleted(new EventObject(this));
        }
    }
    
    /**
     * Get the total length of the file (once connected). This can be -1 if
     * the length is not known.
     * 
     * @return length in bytes, or -1 if not known
     */
    public long getTotalLength() {
        return length;
    }

    /**
     * Get the total downloaded length of the file.
     * 
     * @return length in bytes
     */
    public long getDownloadedLength() {
        return readLength;
    }

    /**
     * Get the etag reported by the server.
     * 
     * @return etag or null if not known
     */
    public String getEtag() {
        return etag;
    }

    /**
     * Use the given etag to check for file modification. If the server
     * returnes a 304 Not Modified, then the download will not take place.
     * 
     * @param etag etag to check, or null to disable
     */
    public void setEtagCheck(String etag) {
        etagCheck = etag;
    }
    
    /**
     * Set a message digest to calculate a digest with.
     * 
     * @param digest digest, or null to disable
     */
    public void setMessageDigest(MessageDigest digest) {
        this.digest = digest;
    }

    /**
     * Initiate and complete the download. Events will be fired appropriately
     * during this period.
     * 
     * @return true if the file was downloaded, false if it was cached
     * @throws IOException IO error
     */
    public boolean download() throws IOException {
        conn = null;
        BufferedInputStream buffInput = null;
        length = -1;

        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (etagCheck != null) {
                conn.setRequestProperty("If-None-Match", "\"" + etagCheck + "\"");
            }
            conn.setDoOutput(true);
            conn.setReadTimeout(timeout);

            conn.connect();
            
            if (conn.getResponseCode() == 304) {
                if (etagCheck == null) {
                    throw new IOException("Got unexpected 304 code");
                }
                return false;
            } else if (conn.getResponseCode() != 200) {
                throw new IOException("Did not get expected 200 code");
            }
            
            fireConnectionStarted();
            
            // Get length
            String s = conn.getHeaderField("Content-Length");
            if (s != null) {
                try {
                    length = Long.parseLong(s);
                } catch (NumberFormatException e) {
                }
            }

            // Get etag
            etag = conn.getHeaderField("Etag");
            if (etag != null) {
                if (etag.matches("^W/")) {
                    etag = null;
                } else {
                    etag = etag.substring(1, etag.length() - 1);
                }
            }
            
            fireLengthKnown();
            
            ProgressEventUpdater progressUpdater = new ProgressEventUpdater();
            (new Thread(progressUpdater)).start();
            
            try {
                buffInput = new BufferedInputStream(conn.getInputStream());
    
                byte[] data = new byte[1024];
                int len = 0;
                while ((len = buffInput.read(data, 0, 1024)) >= 0 && running) {
                    output.write(data, 0, len);
                    if (digest != null) {
                        digest.update(data, 0, len);
                    }
                    readLength += len;
                }
            } finally {
                progressUpdater.stop();
            }
            
            if (running) {
                fireDownloadCompleted();
            }
            
            output.close();
            conn.disconnect();
        } finally {
            conn.disconnect();
            buffInput = null;
            conn = null;
        }
        
        return true;
    }
    
    /**
     * Cancel the download.
     */
    public void cancel() {
        running = false;
    }
    
    /**
     * Used to fire periodical progress events.
     */
    private class ProgressEventUpdater implements Runnable {
        private volatile boolean running = true;
        
        @Override
        public void run() {
            while (running) {
                fireDownloadProgress(getDownloadedLength());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        
        public void stop() {
            running = false;
        }
    }
}
