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

package com.sk89q.mclauncher.update;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.swing.event.EventListenerList;
import com.sk89q.mclauncher.util.URLConnectionDownloader;
import com.sk89q.mclauncher.ProgressListener;
import com.sk89q.mclauncher.StatusChangeEvent;

import com.sk89q.mclauncher.util.Util;

/**
 * Downloads Certificates as specified by a custom update location.
 */
public class CertificateDownloader {
    
    private EventListenerList listenerList = new EventListenerList();    
    private List<String> certificateUrls;
    private Map<String, File> certificateFiles = new HashMap<String, File>();
    private File rootDir;
    private File certificateDir;
    
    /**
     * Construct the Certificate Downloader.
     * 
     * @param urls the URLs of certificates to download
     * @param root the base directory where a "certificates" folder will be created to store downloaded certificates
     */
    public CertificateDownloader(List<String> urls, File root) {
        this.certificateUrls = urls;
        this.rootDir = root;
    }

    /**
     * Downloads and Hashes certificates specified in the urls argument of the constructor.
     * 
     * @throws UpdateException if downloading or hashing fails
     */
    public void download() throws UpdateException {
        certificateDir = new File(rootDir, "certificates");
        
        if(!certificateDir.exists())
            if(!certificateDir.mkdirs())
                throw new UpdateException("Couldn't create certificates directory.");
        
        int certnum = 1;
        for(String s : certificateUrls) {
            URL certURL;
            try {
                certURL = new URL(s);
            } catch (MalformedURLException e) {
                throw new UpdateException("Remote update XML contained malformed certificate URLs.", e);
            }
            
            File certFile = new File(certificateDir.toString(), "cer" + certnum + ".cer");
            
            BufferedOutputStream out;
            try {
                out = new BufferedOutputStream(new FileOutputStream(certFile));
            } catch (IOException e) {
                throw new UpdateException("Could not write to " +
                        certFile.getAbsolutePath() + ".", e);
            }
            
            fireStatusChange("Downloading Certificates... " + certnum + "/" +certificateUrls.size());
            
            URLConnectionDownloader downloader = new URLConnectionDownloader(certURL, out);
            
            try {
                downloader.download();
            } catch(IOException e) {
                throw new UpdateException("Failed to download certificates.", e);
            } finally {
                downloader = null;
                Util.close(out);
            }
            
            String hash;
            try {
                hash = computeSHA1(certFile);
            } catch(NoSuchAlgorithmException e) {
                throw new UpdateException("Failed to SHA-1 certificates.", e);
            }
            
            certificateFiles.put(hash, certFile);
            certnum++;
        }
    }
    
    /**
     * Computes a SHA-1 Hash of a given file.
     * 
     * @param f the file to hash
     * @return the hash String
     * @throws NoSuchAlgorithmException if the SHA-1 hash algorithm was not present on the machine
     * @throws UpdateException if the downloaded certificate could not be read for hashing
     */
    private String computeSHA1(File f) throws NoSuchAlgorithmException, UpdateException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        Formatter formatter = new Formatter();
        byte[] arr = new byte[(int)f.length()];
        try {
            new FileInputStream(f).read(arr);
        } catch(IOException e) {
            throw new UpdateException("Failed to read downloaded certificates.", e);
        }

        for(byte b : md.digest(arr))
            formatter.format("%02x", b);

        return formatter.toString();
    }
    
    /**
     * Gets the Hashes of the downloaded Certificates.
     * 
     * @return a Collection of Certificate Hashes
     */
    public Collection<String> getCertificateHashes() {
        return certificateFiles.keySet();
    }
    
    /**
     * Gets the Certificates downloaded by this Certificate Downloader.
     * 
     * @return a Collection of downloaded Files
     */
    public Collection<File> getFiles() {
        return certificateFiles.values();
    }

    /**
     * Registers a progress listener.
     *
     * @param l listener
     */
    public void addProgressListener(ProgressListener l) {
        listenerList.add(ProgressListener.class, l);
    }
    
    /**
     * Unregister a progress listener.
     *
     * @param l listener
     */
    public void removeProgressListener(ProgressListener l) {
        listenerList.remove(ProgressListener.class, l);
    }    
    
    /**
     * Fire a status change.
     *
     * @param message new status
     */
    private void fireStatusChange(String message) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((ProgressListener) listeners[i + 1]).statusChanged(new StatusChangeEvent(this, message));
        }
    }
}
