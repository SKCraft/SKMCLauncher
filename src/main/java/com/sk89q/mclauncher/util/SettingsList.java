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

import static com.sk89q.mclauncher.util.XMLUtil.getNodes;
import static com.sk89q.mclauncher.util.XMLUtil.parseXml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Stores a list of settings that can inherit from a parent settings list if
 * this settings list does not contain the value. When setting values, it will
 * always be set to the settings list that it was called upon.
 * 
 * @author sk89q
 */
public class SettingsList {

    private SettingsList[] parents;
    private Map<String, String> settings = new HashMap<String, String>();
    
    /**
     * Construct the settings list.
     */
    public SettingsList() {
    }
    
    /**
     * Construct the settings list and set a parent. The last list gets
     * priority.
     * 
     * @param parent parent settings list
     */
    public SettingsList(SettingsList ... parent) {
        this.parents = parent;
    }
    
    /**
     * Get the parent settings list. The last list gets
     * priority.
     * 
     * @return parent settings list, or null
     */
    public SettingsList[] getParents() {
        return parents;
    }

    /**
     * Set the parent settings list.
     * 
     * @param parent parent settings list, or null for none
     */
    public void setParents(SettingsList ... parent) {
        this.parents = parent;
    }
    
    /**
     * Unset a setting.
     * 
     * @param name setting
     */
    public void unset(String name) {
        settings.remove(name);
    }

    /**
     * Returns whether a setting set
     * 
     * @param name name
     * @return true if set
     */
    public boolean has(String name) {
        return settings.containsKey(name); 
    }

    /**
     * Set a value.
     * 
     * @param name name of setting
     * @param val value
     */
    public void set(String name, String val) {
        if (val == null) {
            unset(name);
            return;
        }
        settings.put(name, val);
    }

    /**
     * Set a value.
     * 
     * @param name name of setting
     * @param val value
     */
    public void set(String name, boolean val) {
        set(name, val ? "true" : "false");
    }

    /**
     * Set a value.
     * 
     * @param name name of setting
     * @param val value
     */
    public void set(String name, int val) {
        set(name, String.valueOf(val));
    }

    /**
     * Set a value.
     * 
     * @param name name of setting
     * @param val value
     */
    public void set(String name, long val) {
        set(name, String.valueOf(val));
    }

    /**
     * Set a value.
     * 
     * @param name name of setting
     * @param val value
     */
    public void set(String name, double val) {
        set(name, String.valueOf(val));
    }

    /**
     * Set a value.
     * 
     * @param name name of setting
     * @param val value
     */
    public void set(String name, float val) {
        set(name, String.valueOf(val));
    }

    /**
     * Set a value.
     * 
     * @param name name of setting
     * @param val value
     */
    public void set(String name, Object val) {
        set(name, String.valueOf(val));
    }
    
    /**
     * Get a value.
     * 
     * @param name name of setting
     * @return value, or null
     */
    public String get(String name) {
        String val = settings.get(name);
        if (val == null && parents != null) {
            for (int i = parents.length - 1; i >= 0; i--) {
                val = parents[i].get(name);
                if (val != null) return val;
            }
            return null;
        }
        return val;
    }

    /**
     * Get a value.
     * 
     * @param name name of setting
     * @param def default value
     * @return value, or default
     */
    public String get(String name, String def) {
        return Util.defaultValue(get(name), def);
    }

    /**
     * Get a value.
     * 
     * @param name name of setting
     * @param def default value
     * @return value, or default
     */
    public boolean getBool(String name, boolean def) {
        String val = get(name);
        if (val == null) return def;
        return val.equalsIgnoreCase("true");
    }

    /**
     * Get a value.
     * 
     * @param name name of setting
     * @param def default value
     * @return value, or default
     */
    public int getInt(String name, int def) {
        String val = get(name);
        if (val == null) return def;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Get a value.
     * 
     * @param name name of setting
     * @param def default value
     * @return value, or default
     */
    public long getLong(String name, long def) {
        String val = get(name);
        if (val == null) return def;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Get a value.
     * 
     * @param name name of setting
     * @param def default value
     * @return value, or default
     */
    public double getDouble(String name, double def) {
        String val = get(name);
        if (val == null) return def;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Get a value.
     * 
     * @param name name of setting
     * @param def default value
     * @return value, or default
     */
    public float getFloat(String name, float def) {
        String val = get(name);
        if (val == null) return def;
        try {
            return Float.parseFloat(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Read from an XML node.
     * 
     * @param top node to read from
     * @throws IOException 
     */
    public void read(Node top) throws IOException {
        Map<String, String> settings = new HashMap<String, String>();

        try {
            XPath xpath = XPathFactory.newInstance().newXPath();

            for (Node node : getNodes(top, xpath.compile("setting"))) {
                String key = XMLUtil.getAttrOrNull(node, "key");
                if (key == null) continue;
                String value = XMLUtil.getValue(node);
                settings.put(key, value);
            }
            
            this.settings = settings;
        } catch (XPathExpressionException e) {
            throw new IOException(e);
        }
    }

    /**
     * Read from an XML file.
     * 
     * @param in
     * @throws IOException 
     */
    public void read(InputStream in) throws IOException {
        try {
            Document doc = parseXml(in);
            read(doc.getDocumentElement());
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    /**
     * Write to an XML node, replacing its contents.
     * 
     * @param top node to read from
     * @throws IOException 
     */
    public void write(Element top) throws IOException {
        Node child;
        while ((child = top.getFirstChild()) != null) {
            top.removeChild(child);
        }

        SimpleNode topNode = new SimpleNode(top.getOwnerDocument(), top);
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            SimpleNode settingNode = topNode.addNode("setting");
            settingNode.setAttr("key", entry.getKey());
            settingNode.addValue(entry.getValue());
        }
    }
    
}
