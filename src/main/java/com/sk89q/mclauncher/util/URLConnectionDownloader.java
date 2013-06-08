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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;



/**
 * Used for downloading files via HTTP.
 * 
 * @author sk89q
 */
public class URLConnectionDownloader extends AbstractDownloader {

    private static final int READ_BUFFER_SIZE = 1024 * 8;
    
    private long length;
    private long readLength;
    private String etag;
    private HttpURLConnection conn;
    
    /**
     * Construct the downloader instance.
     * 
     * @param url url to download
     * @param output output stream to write to
     */
    public URLConnectionDownloader(URL url, OutputStream output) {
        super(url, output);
    }
    
    public HttpURLConnection getConnection() {
        if (conn == null) {
            throw new IllegalArgumentException("Connection does not currently exist");
        }
        return conn;
    }
    
    @Override
    public long getTotalLength() {
        return length;
    }

    @Override
    public long getDownloadedLength() {
        return readLength;
    }

    @Override
    public String getEtag() {
        return etag;
    }
    
    @Override
    public boolean download() throws IOException {
        conn = null;
        BufferedInputStream buffInput = null;
        length = -1;

        try {
            conn = (HttpURLConnection) fixURL(getUrl()).openConnection();
            conn.setRequestMethod("GET");
            if (getEtagCheck() != null) {
                conn.setRequestProperty("If-None-Match", "\"" + getEtagCheck() + "\"");
            }
            conn.setDoOutput(true);
            conn.setReadTimeout(getTimeout());

            conn.connect();
            
            if (conn.getResponseCode() == 304) {
                if (getEtagCheck() == null) {
                    throw new IOException("Got unexpected 304 code");
                }
                return false;
            } else if (conn.getResponseCode() != 200) {
                throw new IOException("Did not get expected 200 code, got " + 
                        conn.getResponseCode());
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
    
                byte[] data = new byte[READ_BUFFER_SIZE];
                int len = 0;
                while ((len = buffInput.read(data, 0, READ_BUFFER_SIZE)) >= 0 && isRunning()) {
                    getOutput().write(data, 0, len);
                    if (getDigest() != null) {
                        getDigest().update(data, 0, len);
                    }
                    readLength += len;
                }
            } finally {
                progressUpdater.stop();
            }
            
            if (isRunning()) {
                fireDownloadCompleted();
            }
            
            getOutput().close();
            conn.disconnect();
        } finally {
            conn.disconnect();
            buffInput = null;
            conn = null;
        }
        
        return true;
    }

    /**
     * URL may contain spaces and other nasties that will cause a failure.
     * 
     * @param existing the existing URL to transform
     * @return the new URL, or old one if there was a failure
     */
    private static URL fixURL(URL existing) {
        try {
            URL url = new URL(existing.toString());
            URI uri = new URI(
                    url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), 
                    url.getPath(), url.getQuery(), url.getRef());
            url = uri.toURL();
            return url;
        } catch (MalformedURLException e) {
            return existing;
        } catch (URISyntaxException e) {
            return existing;
        }
    }
}
