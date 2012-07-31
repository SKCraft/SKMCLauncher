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

import java.util.ArrayList;
import java.io.*;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import static com.sk89q.mclauncher.util.XMLUtil.*;
import com.sk89q.mclauncher.config.LauncherOptions;
import com.sk89q.mclauncher.util.SimpleNode;

/**
 * Manages user-trusted certificates and file extensions for an update from a custom location.
 */
public class UpdateTrustManager {
    
    private Collection<String> localCertificateHashes = new ArrayList<String>();
    private Collection<String> localFileExtensions = new ArrayList<String>();
    
    private File trustFile;
    
    /**
     * Construct the Trust Manager.
     * 
     * @param baseDir the base directory where trust.xml will be read and written
     * @throws IOException if reading trust.xml fails
     */
    public UpdateTrustManager(File baseDir) throws IOException {
        trustFile = new File(baseDir, "trust.xml");
        
        if(trustFile.exists())
            readLocal();
        else 
            if(!save())
                throw new IOException("Failed to create trust.xml.");
    }
    
    /**
     * Reads and parses trust.xml, loading Certificate Hashes and File Extensions.
     * 
     * @throws IOException if reading trust.xml fails
     */
    private void readLocal() throws IOException {
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(trustFile));
            
            Document doc = parseXml(in);
            XPath xpath = XPathFactory.newInstance().newXPath();
            
            for(Node n : getNodes(doc, xpath.compile("/trust/certificates/certificate")))
                localCertificateHashes.add(getValue(n).toLowerCase());
            
            for(Node n : getNodes(doc, xpath.compile("/trust/fileextensions/fileextension")))
                localCertificateHashes.add(getValue(n).toLowerCase());
        } 
        catch(ParserConfigurationException e)
            { throw new IOException(e); }
        catch(SAXException e)
            { throw new IOException(e); }
        catch(XPathExpressionException e)
            { throw new IOException(e); }
    }
    
    /**
     * Attempts to save trust.xml, returning a boolean indiciating whether or not the save was successful.
     * 
     * @return true if writing trust.xml was successful, false otherwise
     */
    public final boolean save() {
        try {
            write();
            return true;
        } catch(IOException e) {
            Logger.getLogger(LauncherOptions.class.getCanonicalName()).log(Level.WARNING, "Failed to write trust.xml", e);
            return false;
        }
    }
    
    /**
     * Private method that actually does the work of serializing this Trust Manager's fields to XML.
     * 
     * @throws IOException if writing trust.xml failed
     */
    private void write() throws IOException {
        try {
            Document doc = newXml();
            SimpleNode root = start(doc, "trust");
            
            SimpleNode certificates = root.addNode("certificates");
            for(String s : localCertificateHashes) {
                SimpleNode certificate = certificates.addNode("certificate");
                certificate.addValue(s.toLowerCase());
            }
            
            SimpleNode exts = root.addNode("fileextensions");
            for(String s : getLocalFileExtensions()) {
                SimpleNode ext = exts.addNode("fileextension");
                ext.addValue(s.toLowerCase().toLowerCase());
            }
            
            writeXml(doc, trustFile);
        }
        catch(ParserConfigurationException e)
            { throw new IOException(e); }
        catch(TransformerException e)
            { throw new IOException(e); }        
    }
    /**
     * Returns true if all the passed in Certificates are contained within the Trust Manager's
     * internal collection of already-trusted Certificates, false otherwise.
     */
    public boolean containsAllCertificates(Collection<String> remoteCertificates) {
        for(String s : remoteCertificates)
            if(!localCertificateHashes.contains(s.toLowerCase()))
                return false;
        
        return true;
    }
    
    /**
     * Returns true if all the passed in File Extensions are contained within the Trust Manager's
     * internal collection of already-trusted File Extensions, false otherwise.
     */
    public boolean containsAllFileExtensions(Collection<String> remoteFileExtensions) {
        for(String s : remoteFileExtensions)
            if(!localFileExtensions.contains(s.toLowerCase()))
                return false;
        
        return true;
    }

    /**
     * Gets a collection of Hashes of Certificates the user has trusted.
     * 
     * @return the Certificate Hashes
     */
    public Collection<String> getLocalCertificateHashes() {
        return localCertificateHashes;
    }

    /**
     * Sets a collection of Hashes of Certificates the user has trusted.
     * 
     * @param the Certificate Hashes
     */
    public void setLocalCertificateHashes(Collection<String> localCertificateHashes) {
        this.localCertificateHashes = localCertificateHashes;
    }

    /**
     * Gets a collection of File Extensions the user has trusted.
     * 
     * @return the File Extensions
     */
    public Collection<String> getLocalFileExtensions() {
        return localFileExtensions;
    }

    /**
     * Sets a collection of File Extensions the user has trusted.
     * 
     * @param the File Extensions
     */
    public void setLocalFileExtensions(Collection<String> localFileExtensions) {
        this.localFileExtensions = localFileExtensions;
    }
}
