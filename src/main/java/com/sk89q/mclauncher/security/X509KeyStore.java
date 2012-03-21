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

package com.sk89q.mclauncher.security;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.X509TrustManager;

import com.sk89q.mclauncher.util.Util;

/**
 * Presents a key store.
 * 
 * @author sk89q
 */
public class X509KeyStore implements X509TrustManager {
    
    private final Set<X509Certificate> rootCerts = new HashSet<X509Certificate>();
    private final Set<X509Certificate> intermediateCerts = new HashSet<X509Certificate>();
    
    /**
     * Add a root certificate.
     * 
     * @param cert certificate
     */
    public void addRootCertificate(X509Certificate cert) {
        rootCerts.add(cert);
    }

    /**
     * Add root certificates from an input stream.
     * 
     * @param in
     *            input
     * @throws CertificateException
     *             on error
     * @throws IOException
     *             on I/O error
     */
    public void addRootCertificates(InputStream in)
            throws CertificateException, IOException {
        try {
            BufferedInputStream bufferedIn = new BufferedInputStream(in);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            while (bufferedIn.available() > 0) {
                Certificate cert = cf.generateCertificate(bufferedIn);
                addRootCertificate((X509Certificate) cert);
            }
        } finally {
            Util.close(in);
        }
    }

    /**
     * Add a intermediate certificate.
     * 
     * @param cert
     *            certificate
     */
    public void addIntermediateCertificate(X509Certificate cert) {
        intermediateCerts.add(cert);
    }

    /**
     * Add root certificates from an input stream.
     * 
     * @param in
     *            input
     * @throws CertificateException
     *             on error
     * @throws IOException
     *             on I/O error
     */
    public void addIntermediateCertificate(InputStream in)
            throws CertificateException, IOException {
        try {
            BufferedInputStream bufferedIn = new BufferedInputStream(in);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
    
            while (bufferedIn.available() > 0) {
                Certificate cert = cf.generateCertificate(bufferedIn);
                addIntermediateCertificate((X509Certificate) cert);
            }
        } finally {
            Util.close(in);
        }
    }

    /**
     * Verify that a given certificate is trusted.
     * 
     * @param chain certificate chain
     * @throws CertPathBuilderException thrown on verification error
     * @throws CertificateVerificationException thrown on any error
     */
    public void verify(X509Certificate[] chain) 
            throws CertificateVerificationException, CertPathBuilderException {
        try {
            X509CertSelector selector = new X509CertSelector();
            selector.setCertificate(chain[0]);

            // Root certificates
            Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();
            for (X509Certificate rootCert : rootCerts) {
                trustAnchors.add(new TrustAnchor(rootCert, null));
            }

            PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(
                    trustAnchors, selector);
            
            pkixParams.setRevocationEnabled(true);

            // Built-in intermediate certificates
            pkixParams.addCertStore(CertStore.getInstance(
                    "Collection", new CollectionCertStoreParameters(
                            intermediateCerts)));
            
            // Additional intermediate certificates
            pkixParams.addCertStore(CertStore.getInstance(
                    "Collection", new CollectionCertStoreParameters(Arrays.asList(chain))));

            CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
            builder.build(pkixParams); // Will error on failure to verify
        } catch (InvalidAlgorithmParameterException e) {
            throw new CertificateVerificationException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertificateVerificationException(e);
        }
    }

    /**
     * Check if a client certificate chain is trusted. This is
     * not used in this situation.
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        throw new UnsupportedOperationException("Client certificates are supported");
    }

    /**
     * Check if a server certificate chain is trusted.
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        for (X509Certificate cert : chain) {
            cert.checkValidity();
            if (cert.hasUnsupportedCriticalExtension()) {
                throw new CertificateException("Unsupported critical extension found");
            }
        }
        
        try {
            verify(chain);
        } catch (CertificateVerificationException e) {
            throw new CertificateException("Verification error: " + e.getMessage(), e);
        } catch (CertPathBuilderException e) {
            throw new CertificateException(e.getMessage(), e);
        }
    }

    /**
     * Get a list of accepted issuers for client certificates. This is
     * not used in this situation.
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

}
