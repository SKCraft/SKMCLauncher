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

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertPathBuilderException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import com.sk89q.mclauncher.security.CertificateVerificationException;
import com.sk89q.mclauncher.security.X509KeyStore;
import com.sk89q.mclauncher.util.Util;

/**
 * Verifies digital signatures.
 * 
 * @author sk89q
 */
public class SignatureVerifier {

    private X509KeyStore keyStore;
    
    /**
     * Constructs the verifier.
     * 
     * @param keyStore key store
     */
    public SignatureVerifier(X509KeyStore keyStore) {
        this.keyStore = keyStore;
    }
    
    /**
     * Verify a list of certificates to be trusted.
     * 
     * @param certs list of certificates
     * @throws SecurityException thrown on verification failure
     */
    public void verify(Certificate[] certs) throws SecurityException {
        try {
            keyStore.verify((X509Certificate[]) certs);
        } catch (CertPathBuilderException e) {
            throw new SecurityException("Digital signature verification failed: " + e.getMessage());
        } catch (CertificateVerificationException e) {
            throw new SecurityException("Digital signature verification failed: " + e.getMessage());
        }
    }
    
    /**
     * Attempt to verify that a file is signed.
     * 
     * @param in input stream
     * @param ext file extension
     * @throws SecurityException throw on verification failure
     * @throws IOException on I/O error
     */
    public void verify(InputStream in, String ext) throws SecurityException, IOException {
        try {
            if (ext.equalsIgnoreCase("jar") || ext.equalsIgnoreCase("zip")) {
                verifyJar(in);
            } else {
                throw new SecurityException("Not sure how to verify the signature for '" + ext + "'");
            }
        } finally {
            Util.close(in);
        }
    }

    /**
     * Attempt to verify that a Jar file is signed. All files (aside from
     * ones in META-INF) must be signed and trusted.
     * 
     * @param in input stream
     * @throws SecurityException throw on verification failure
     * @throws IOException on I/O error
     */
    public void verifyJar(InputStream in) throws IOException {
        JarInputStream jarFile = new JarInputStream(in);
        Manifest manifest = jarFile.getManifest();
        if (manifest == null) {
            throw new SecurityException("The given file was not digitally signed");
        }

        // Ensure all the entries' signatures verify correctly
        byte[] buffer = new byte[8192];
        JarEntry entry;
        while ((entry = jarFile.getNextJarEntry()) != null) {
            if (entry.isDirectory()) continue;

            do {
            } while (jarFile.read(buffer, 0, buffer.length) != -1);
            
            Certificate[] certs = entry.getCertificates();
            if (isMetaInf(entry.getName())) {
                continue;
            } else if (certs == null || certs.length == 0) {
                throw new SecurityException("The archive contains files that are not digitally signed");
            } else {
                int i = 0;
                boolean verified = false;
                while (i < certs.length) {
                    X509Certificate[] chain = findChain(certs, i);
                    try {
                        verify(chain);
                        verified = true;
                        break;
                    } catch (SecurityException e) {
                    }
                    i += chain.length;
                }
                
                if (!verified) {
                    // throw new SecurityException("The file(s) are signed by an entity that is not registered as 'trusted' with the launcher");
                }
            }
        }
    }
    
    /**
     * Tries to find a chain given a list of certificates and a start index. It
     * is assumed that the array contains a sequential list of certificates,
     * one chain after another, with the later entries in each group being
     * higher up in the chain.
     * 
     * @param seq list of certificates
     * @param startIndex start index to search from
     * @return list of certificates
     */
    public static X509Certificate[] findChain(Certificate[] seq, int startIndex) {
        int i = 0;
        for (i = startIndex; i < seq.length - 1; i++) {
            if (((X509Certificate) seq[i + 1]).getSubjectDN().equals(
                    ((X509Certificate) seq[i]).getIssuerDN())) {
                break;
            }
        }
        
        X509Certificate[] chain = new X509Certificate[i - startIndex + 1];
        for (int j = 0; j < chain.length; j++) {
            chain[j] = (X509Certificate) seq[startIndex + j];
        }
        
        return chain;
    }
    
    /**
     * Returns whether the given path is in META-INF.
     * 
     * @param name name of entry
     * @return true if META-INF
     */
    private static boolean isMetaInf(String name) {
        for (String part : name.split("[/\\\\]")) {
            if (part.equalsIgnoreCase("META-INF")) {
                return true;
            }
        }
        
        return false;
    }

}
