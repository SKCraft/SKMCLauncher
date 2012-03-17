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
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.sk89q.mclauncher.util.SimpleNode;

import static com.sk89q.mclauncher.util.XMLUtil.*;

/**
 * Stores update versioning information.
 * 
 * @author sk89q
 */
public class UpdateCache {
    
    private File file;
    private String lastUpdateId;
    private Map<String, String> hashCache;
    
    public UpdateCache(File file) {
        this.file = file;
        
        try {
            read();
        } catch (IOException e) {
        }
    }
    
    public void read() throws IOException {
        hashCache = new HashMap<String, String>();
        InputStream in;
        
        try {
            in = new BufferedInputStream(new FileInputStream(file));

            Document doc = parseXml(in);
            XPath xpath = XPathFactory.newInstance().newXPath();
            
            // Read all the <entry> elements
            for (Node node : getNodes(doc, xpath.compile("/cache/entry"))) {
                String path = getValue(node);
                String hash = getAttrOrNull(node, "hash");
                hashCache.put(path, hash);
            }
            
            lastUpdateId = getStringOrNull(doc, xpath.compile("/cache/current/text()"));
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void write() throws IOException {
        try {
            Document doc = newXml();
            SimpleNode root = start(doc, "cache");
    
            if (lastUpdateId != null) {
                root.addNode("current").addValue(lastUpdateId);
            }
            
            for (Map.Entry<String, String> entry : hashCache.entrySet()) {
                root.addNode("entry")
                        .addValue(entry.getKey())
                        .setAttr("hash", entry.getValue());
            }

            writeXml(doc, file);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }
    
    public String getLastUpdateId() {
        return lastUpdateId;
    }

    public void setLastUpdateId(String lastUpdateId) {
        this.lastUpdateId = lastUpdateId;
    }

    public String getCachedHash(String path) {
        return hashCache.get(path);
    }
    
    public void putCachedHash(String path, String hash) {
        hashCache.put(path, hash);
    }
    
}
