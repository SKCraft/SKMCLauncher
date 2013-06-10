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

package com.sk89q.lpbuilder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sk89q.mclauncher.util.LauncherUtils;

/**
 * Builds a signature of a file in order to give it a "version" by the update process.
 */
public class FileSignatureBuilder {
    
    private final Logger logger = Logger.getLogger(
            FileSignatureBuilder.class.getCanonicalName());
    private static final boolean DEBUG = 
            System.getProperty("com.sk89q.lpbuilder.FileSignatureBuilder.debug", "false")
                    .equalsIgnoreCase("true");
    private static final Pattern ZIP_NAMES = Pattern.compile(
            "^.*\\.(zip|jar)$", Pattern.CASE_INSENSITIVE);

    public FileSignatureBuilder() {
    }
    
    public MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    public byte[] fromZipContents(File file) throws IOException {
        SignatureList list = new SignatureList();
        InputStream fis = null;
        ZipInputStream zip = null;
        try {
            fis = new FileInputStream(file);
            zip = new ZipInputStream(fis);
            
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                list.add(entry.getName(), fromInputStream(zip));
            }
            
            return list.toDigest();
        } finally {
            LauncherUtils.close(zip);
            LauncherUtils.close(fis);
        }
    }

    public byte[] smartFromFile(File file) throws IOException {
        try {
            if (ZIP_NAMES.matcher(file.getName()).matches()) {
                return fromZipContents(file);
            }
        } catch (IOException e) {
        }
        
        return fromFile(file);
    }

    public byte[] fromFile(File file) throws IOException {
        InputStream fis = null;
        BufferedInputStream bis = null;
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            return fromInputStream(bis);
        } finally {
            LauncherUtils.close(bis);
            LauncherUtils.close(fis);
        }
    }
    
    public byte[] fromInputStream(InputStream is) throws IOException {
        MessageDigest digest = createDigest();
        byte[] buf = new byte[1024 * 2];
        int len;
        while ((len = is.read(buf, 0, buf.length)) != -1) {
            digest.update(buf, 0, len);
        }
        return digest.digest();
    }
    
    public SignatureList createList() {
        return new SignatureList();
    }
    
    public class SignatureList {
        private final List<FileSignature> files = new ArrayList<FileSignature>();
        
        public void add(String key, byte[] hash) {
            MessageDigest digest = createDigest();
            digest.update(hash);
            files.add(new FileSignature(key, hash));
        }
        
        public byte[] toDigest() {
            if (DEBUG) {
                logger.info("------------------- Signature List -------------------");
            }
            Collections.sort(files);
            MessageDigest digest = createDigest();
            for (FileSignature file : files) {
                digest.update(file.key.getBytes());
                digest.update((byte) 0);
                digest.update(file.digest);
                if (DEBUG) {
                    logger.info(LauncherUtils.getHexString(file.digest) + " " + file.key);
                }
            }
            if (DEBUG) {
                logger.info("------------------------------------------------------");
            }
            return digest.digest();
        }
    }
    
    private static class FileSignature implements Comparable<FileSignature> {
        private final String key;
        private final byte[] digest;
        
        public FileSignature(String key, byte[] digest) {
            this.key = key;
            this.digest = digest;
        }

        @Override
        public int compareTo(FileSignature o) {
            return key.compareTo(o.key);
        }
    }

}
