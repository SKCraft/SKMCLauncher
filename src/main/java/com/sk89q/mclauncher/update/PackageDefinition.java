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

import static com.sk89q.mclauncher.util.Util.defaultValue;
import static com.sk89q.mclauncher.util.Util.uppercaseValueOf;
import static com.sk89q.mclauncher.util.XMLUtil.getAttr;
import static com.sk89q.mclauncher.util.XMLUtil.getAttrOrNull;
import static com.sk89q.mclauncher.util.XMLUtil.getNodes;
import static com.sk89q.mclauncher.util.XMLUtil.getString;
import static com.sk89q.mclauncher.util.XMLUtil.getValue;
import static com.sk89q.mclauncher.util.XMLUtil.parseXml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.Platform;
import com.sk89q.mclauncher.update.PackageFile.MessageDigestAlgorithm;

/**
 * Handles and parses a package definition file.
 * 
 * @author sk89q
 */
public class PackageDefinition {
    
    private static final String EXPECTED_DEF_VERSION = "1.1";
    private List<PackageFile> files;
    private long totalEstimatedSize;
    
    private PackageDefinition() {
    }
    
    /**
     * Parses a definition .xml file from the given input stream.
     * 
     * @param rootDir root dir to base file set from
     * @param in input stream of package .xml
     * @return package definition
     * @throws IOException on I/O error
     */
    public static PackageDefinition parse(File rootDir, InputStream in) throws IOException {
        try {
            List<PackageFile> files = new ArrayList<PackageFile>();
            long totalEstimatedSize = 0;
            
            Document doc = parseXml(in);
            XPath xpath = XPathFactory.newInstance().newXPath();
            
            // Check whether we support this package version
            String defVersion = getString(doc, xpath.compile("/package/@version"));
            if (!defVersion.equals(EXPECTED_DEF_VERSION)) {
                throw new IOException("Unknown package definition version '" + defVersion + "'. Either the package .xml file is corrupt, or you need to update your launcher.");
            }
            
            // Read all the <filegroup> elements
            for (Node node : getNodes(doc, xpath.compile("/package/filegroup"))) {
                URL baseURL = new URL(getAttr(node, "source"));
                File dest = new File(rootDir, getAttr(node, "dest"));
                MessageDigestAlgorithm hashType = uppercaseValueOf(MessageDigestAlgorithm.class, getAttrOrNull(node, "verify"));
    
                // Read all the <file> sub-elements
                for (Node fileNode : getNodes(node, xpath.compile("*"))) {
                    String nodeName = fileNode.getNodeName().toLowerCase();
    
                    // Check platform and skip files for other platforms
                    Platform platform = uppercaseValueOf(
                            Platform.class, getAttrOrNull(fileNode, "platform"));
                    if (platform != null && platform != Launcher.getPlatform()) {
                        continue;
                    }
    
                    // Parse path
                    String urlName = getValue(fileNode);
                    String filename = defaultValue(getAttrOrNull(fileNode, "filename"), urlName);
                    long size = Long.parseLong(defaultValue(getAttrOrNull(fileNode, "size"), "1000"));
    
                    URL url = new URL(baseURL.toString() + filename);
                    File target = new File(dest, filename);
                    File temp = new File(dest, filename + ".tmp");
                    PackageFile packageFile;
                    
                    if (!target.toString().startsWith(rootDir.toString())) {
                        throw new IOException(String.format("%s not in %s; invalid path. Package file may be corrupt.",
                                target.toString(), rootDir.toString()));
                    }
                    
                    // Add the file as long as it's not trust.xml in the root directory.
                    // trust.xml is a record of user-trusted files and certificates.  It
                    // should only be written by the UpdateTrustManager.  If it were modified
                    // by an update, that would be a major security flaw.
                    if(!(dest.getAbsolutePath().equals(rootDir.getAbsolutePath()) && filename.equals("trust.xml"))) {
                        if (nodeName == "file") {
                            packageFile = new SingleFile(url, temp, target, size);
                        } else if (nodeName == "archive") {
                            packageFile = new ArchiveFile(url, temp, target, size);
                        } else {
                            throw new IOException("Unknown file type '" + nodeName + "'. Launcher update may be required.");
                        }
                    
                        packageFile.setVerifyType(hashType);

                        files.add(packageFile);

                        totalEstimatedSize += size;
                    }
                }
            }
            
            PackageDefinition def = new PackageDefinition();
            def.files = files;
            def.totalEstimatedSize = totalEstimatedSize;
            return def;
        } catch (NumberFormatException e) {
            throw new IOException(e);
        } catch (XPathExpressionException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }
    
    /**
     * Get the package list.
     * 
     * @return list of files
     */
    public List<PackageFile> getFileList() {
        return files;
    }
    
    /**
     * Get the estimated total file size.
     * 
     * @return file size in bytes
     */
    public long getEstimatedTotalSize() {
        return totalEstimatedSize;
    }
    
}
