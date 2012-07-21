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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sk89q.mclauncher.Launcher;

/**
 * Used for downloading files via HTTP 1.0 manually with a {@link Socket}.
 * Re-inventin' the wheel to keep the launcher small. No SSL support yet.
 * 
 * @author sk89q
 */
public class SocketDownloader extends AbstractDownloader {

    private static final int HTTP_LINE_BUFFER_SIZE = 1024 * 4;
    private static final int READ_BUFFER_SIZE = 1024 * 4;
    
    private long length;
    private long readLength;
    private String etag;
    private Socket conn;
    private Pattern statusPattern = Pattern.compile("^HTTP/([0-9\\.]+) ([0-9]+) (.+)$");
    
    /**
     * Construct the downloader instance.
     * 
     * @param url url to download
     * @param output output stream to write to
     */
    public SocketDownloader(URL url, OutputStream output) {
        super(url, output);
    }
    
    public Socket getConnection() {
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
    
    private String dbgStr(String s) {
        if (s.length() < 200) {
            return s;
        } else {
            return "<string too long>";
        }
    }

    @Override
    public boolean download() throws IOException {
        conn = null;
        length = -1;

        try {
            int port = getUrl().getPort();
            if (port == -1) port = getUrl().getDefaultPort();
            if (port == -1) port = 80;
            
            conn = new Socket(getUrl().getHost(), port);
            conn.setSoTimeout(getTimeout());
            
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            MixedDataBufferedInputStream in = new MixedDataBufferedInputStream(conn.getInputStream());
            
            out.write("GET ");
            out.write(new URI(getUrl().getPath()).toASCIIString());
            out.write(" HTTP/1.0\r\n");
            out.write("Host: " + getUrl().getHost() + "\r\n");
            out.write("User-Agent: SKMCLauncher/" + Launcher.VERSION);
            out.write("Accept: */*\r\n");
            if (getEtagCheck() != null) {
                // Should encode/munge this...
                out.write("If-None-Match: \"" + getEtagCheck() + "\"");
            }
            out.write("Connection: close\r\n");
            out.write("\r\n");
            out.flush();
            
            String line = in.readLine();
            Matcher m = statusPattern.matcher(line.trim());
            if (!m.matches()) {
                // This is a problem!
                throw new IOException("HTTP status line was unexpected ('" + dbgStr(line) + "')");
            }
            
            // Check HTTP version
            String httpVersion = m.group(1);
            if (!httpVersion.equals("0.9") && !httpVersion.equals("1.0") && !httpVersion.equals("1.1")) {
                throw new IOException("Unsupported HTTP version from server ('" + dbgStr(httpVersion) + "')");
            }
            
            // Check HTTP response code
            int code = Integer.parseInt(m.group(2));
            if (code == 304) {
                if (getEtagCheck() == null) {
                    throw new IOException("Got unexpected 304 code");
                }
                return false;
            } else if (code != 200) {
                throw new IOException("Server did not give a 200 'EVERYTHING OK' code (server gave code " + code + " instead)");
            }
            
            fireConnectionStarted();
            
            // Read headers
            while (true) {
                line = in.readLine();
                
                // No more headers
                if (line.trim().length() == 0) {
                    break;
                }
                
                int colonIndex = line.indexOf(':');
                if (colonIndex == -1) {
                    throw new IOException("Malformed HTTP response header ('" + dbgStr(line) + "')");
                }
                String key = line.substring(0, colonIndex).trim(); // SHOULD decode this, but we won't
                String value = line.substring(colonIndex + 1).trim(); // SHOULD decode this, but we won't
                
                if (key.equalsIgnoreCase("Content-Length")) {
                    try {
                        length = Long.parseLong(value);
                    } catch (NumberFormatException e) {
                    }
                }
                
                if (key.equalsIgnoreCase("Etag")) {
                    if (value.matches("^W/")) {
                        etag = null;
                    } else if (value.length() > 2) {
                        etag = value.substring(1, value.length() - 1);
                    }
                }
            }
            
            fireLengthKnown();
            
            ProgressEventUpdater progressUpdater = new ProgressEventUpdater();
            (new Thread(progressUpdater)).start();
            
            try {
                byte[] data = new byte[READ_BUFFER_SIZE];
                int len = 0;
                while ((len = in.read(data, 0, READ_BUFFER_SIZE)) >= 0 && isRunning()) {
                    getOutput().write(data, 0, len);
                    getOutput().flush();
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
        } catch (URISyntaxException e) {
            throw new IOException("Failed to parse path", e);
        } finally {
            conn.close();
            conn = null;
        }
        
        return true;
    }
    
    public static void main(String[] args) throws Throwable {
        URL url = new URL(args[0]);
        File file = new File(args[1]);
        
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        SocketDownloader downloader = new SocketDownloader(url, out);
        downloader.download();
        
        out.close();
        
        System.err.println("Done!");
    }
}
