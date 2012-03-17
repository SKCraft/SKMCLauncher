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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a file in a package.
 * 
 * @author sk89q
 */
public abstract class PackageFile {
    private String originalName;
    private String[] filterExts;
    private URL url;
    private File tempFile;
    private long totalEstimatedSize;
    private File file;
    private MessageDigestAlgorithm verifyType;
    private boolean ignored = false;
    private boolean verifySignature = true;
    
    /**
     * Construct the package file base.
     * 
     * @param url url that the package file can be fetched from
     * @param tempFile temp file 
     * @param file target file
     * @param totalEstimatedSize estimated total file size
     */
    public PackageFile(URL url, File tempFile, File file, long totalEstimatedSize) {
        this.url = url;
        this.tempFile = tempFile;
        this.file = file;
        this.totalEstimatedSize = totalEstimatedSize;
        
        String[] parts = getFilename().split("\\.");
        List<String> filterExts = new ArrayList<String>();
        StringBuilder s = new StringBuilder();
        for (int i = parts.length - 1; i > 0; i--) {
            String part = parts[i];
            if (FileStreamFilters.get(part) != null) {
                filterExts.add(part);
            } else {
                for (int j = 0; j <= i; j++) {
                    if (j != 0) {
                        s.append(".");
                    }
                    s.append(parts[j]);
                }
                break;
            }
        }
        
        this.filterExts = new String[filterExts.size()];
        this.filterExts = filterExts.toArray(this.filterExts);
        this.originalName = s.toString();
    }
    
    /**
     * Get the URL to download from.
     * 
     * @return url
     */
    public URL getURL() {
        return url;
    }
    
    /**
     * Get the temporary file.
     * 
     * @return file
     */
    public File getTempFile() {
        return tempFile;
    }
    
    /**
     * Get the file.
     * 
     * @return file
     */
    public File getFile() {
        return file;
    }
    
    /**
     * Get the total estimated file size.
     * 
     * @return size
     */
    public long getTotalEstimatedSize() {
        return totalEstimatedSize;
    }
    
    /**
     * Get whether the file is filtered (i.e compressed).
     * 
     * @return true is filtered
     */
    public boolean isFiltered() {
        return filterExts.length > 0;
    }
    
    /**
     * Get the verification type.
     * 
     * @return type, or null if none
     */
    public MessageDigestAlgorithm getVerifyType() {
        return verifyType;
    }

    /**
     * Set the verification type.
     * 
     * @param verifyType type or null
     */
    public void setVerifyType(MessageDigestAlgorithm verifyType) {
        this.verifyType = verifyType;
    }

    /**
     * Get the destination filename, possibly with extraneous extensions
     * for compressed files.
     * 
     * @return name
     */
    public String getFilename() {
        return getFile().getName();
    }
    
    /**
     * Get the destination filename, without extra .extensions for compressed
     * files (i.e. file.lzma).
     * 
     * @return name
     */
    public String getOriginalFilename() {
        return originalName;
    }
    
    /**
     * Gets whether this file has been set to be ignored.
     * 
     * @return ignore status
     */
    public boolean isIgnored() {
        return ignored;
    }

    /**
     * Set ignore status.
     * 
     * @param ignored ignored
     */
    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    /**
     * Return whether signature verification is on.
     * 
     * @return whether signature verification is on
     */
    public boolean verifySignatures() {
        return verifySignature;
    }

    /**
     * Set signature verification status.
     * 
     * @param verifySignature true to verify signatures
     */
    public void setVerifySignature(boolean verifySignature) {
        this.verifySignature = verifySignature;
    }

    /**
     * Get an input stream to read the file.
     * 
     * @return input stream
     * @throws IOException on I/O error
     */
    public InputStream getInputStream() throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(getTempFile()));
        for (String filterExt : filterExts) {
            in = FileStreamFilters.get(filterExt).filter(in);
        }
        return in;
    }
    
    /**
     * Deploys the file, installing it.
     * 
     * @param log log of added items
     * @throws IOException on I/O error
     */
    public abstract void deploy(UninstallLog log) throws IOException;
    
    /**
     * Verifies that a file is properly signed and trusted.
     * 
     * @param verifier verifier
     * @throws SecurityException thrown on signature verification error
     * @throws IOException thrown on I/O error
     */
    public abstract void verify(SignatureVerifier verifier) throws SecurityException, IOException;

    /**
     * Hash types for e-tag verification.
     */
    public static enum MessageDigestAlgorithm {
        MD5("MD5"),
        SHA1("SHA-1"),
        SHA512("SHA-512");
        
        private String javaDigestName;
        
        MessageDigestAlgorithm(String javaDigestName) {
            this.javaDigestName = javaDigestName;
        }
        
        public String getJavaDigestName() {
            return javaDigestName;
        }
    }
}