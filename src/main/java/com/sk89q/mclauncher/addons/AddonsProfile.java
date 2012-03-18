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

package com.sk89q.mclauncher.addons;

import static com.sk89q.mclauncher.util.XMLUtil.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.sk89q.mclauncher.util.SimpleNode;
import com.sk89q.mclauncher.util.Util;

/**
 * Manages the addons of a profile.
 * 
 * @author sk89q
 */
public class AddonsProfile implements TableModel {
    
    private File configFile;
    private File dir;
    private List<Addon> addons = new ArrayList<Addon>();
    private EventListenerList listenerList = new EventListenerList();
    
    /**
     * Construct a profile from a directory.
     * 
     * @param dir directory
     */
    public AddonsProfile(File dir) {
        dir.mkdirs();
        this.dir = dir;
        this.configFile = new File(dir, "addons.xml");
    }
    
    /**
     * Read the list of addons.
     * 
     * @throws IOException
     */
    public void read() throws IOException {
        addons = new ArrayList<Addon>();
        InputStream in = null;
        
        try {
            in = new BufferedInputStream(new FileInputStream(configFile));
            
            Document doc = parseXml(in);
            XPath xpath = XPathFactory.newInstance().newXPath();

            XPathExpression idExpr = xpath.compile("id/text()");
            XPathExpression versionExpr = xpath.compile("version/text()");
            XPathExpression nameExpr = xpath.compile("name/text()");
            XPathExpression filenameExpr = xpath.compile("filename/text()");
            XPathExpression urlExpr = xpath.compile("url/text()");
            XPathExpression enabledExpr = xpath.compile("enabled/text()");
            
            // Read all the <identity> elements
            for (Node node : getNodes(doc, xpath.compile("/config/addons/addon"))) {
                String filename = getStringOrNull(node, filenameExpr);
                if (filename == null) {
                    continue; // Skip this entry
                }
                String id = Util.defaultValue(getStringOrNull(node, idExpr), filename);
                String name = Util.defaultValue(getStringOrNull(node, nameExpr), filename);
                String version = getStringOrNull(node, versionExpr);
                String urlS = getStringOrNull(node, urlExpr);
                boolean enabled = getBool(node, false, enabledExpr);
                
                URL url = null;
                try {
                    url = new URL(urlS);
                } catch (MalformedURLException e) {
                }
                
                File file = new File(dir, filename);
                if (!file.exists()) {
                    file = null;
                }
                
                Addon addon = new Addon(id, name, version, file, url);
                addon.setEnabled(enabled);
                addons.add(addon);
            }
            
            fireTableChanged(new TableModelEvent(this));
        } catch (FileNotFoundException e) {
        } catch (XPathExpressionException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        } finally {
            Util.close(in);
        }
    }
    
    /**
     * Write the list of addons.
     * 
     * @throws IOException
     */
    public void write() throws IOException {
        try {
            Document doc = newXml();
            SimpleNode root = start(doc, "config");
            
            SimpleNode addonsNode = root.addNode("addons");
            
            for (Addon addon : addons) {
                SimpleNode addonNode = addonsNode.addNode("addon");
                addonNode.addNode("id").addValue(addon.getId());
                addonNode.addNode("version").addValue(addon.getVersion());
                addonNode.addNode("name").addValue(addon.getName());
                addonNode.addNode("filename").addValue(addon.getFile().getName());
                addonNode.addNode("url").addValue(addon.getUrl() != null ? addon.getUrl().toString() : null);
                addonNode.addNode("enabled").addValue(addon.isEnabled());
            }
            
            writeXml(doc, configFile);
        } catch (TransformerException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    /**
     * Save the list of addons.
     * 
     * @return true if successful
     */
    public boolean save() {
        try {
            write();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Get the directory.
     * 
     * @return directory
     */
    public File getDirectory() {
        return dir;
    }
    
    /**
     * Returns whether an installed addon is using the given file.
     * 
     * @param file file to check
     * @return true if it is used
     */
    public boolean isUsing(File file) {
        for (Addon other : addons) {
            if (file.equals(other.getFile())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if an addon is installed with the given ID.
     * 
     * @param id id to check
     * @return true if installed
     */
    public boolean hasInstalled(String id) {
        for (Addon other : addons) {
            if (id.equals(other.getId())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Get a list of addons with the given ID.
     * 
     * @param id id
     * @return list of addons
     */
    public List<Addon> getAddonsOfId(String id) {
        List<Addon> ret = new ArrayList<Addon>();
        
        for (Addon addon : addons) {
            if (id.equals(addon.getId())) {
                ret.add(addon);
            }
        }
        
        return ret;
    }
    
    /**
     * Add an addon. More than one addon can share the same ID.
     * 
     * @param addon addon
     * @return true if the addon object wasn't already in the list
     */
    public boolean add(Addon addon) {
        if (addons.add(addon)) {
            fireTableChanged(new TableModelEvent(this, addons.size() - 1));
            return true;
        }
        
        return false;
    }
    
    /**
     * Move an addon up in the order.
     * 
     * @param addon addon
     * @return true if it could be moved
     * @throws IllegalArgumentException if the addon is not in the list
     */
    public boolean moveUp(Addon addon) {
        int index = addons.indexOf(addon);
        if (index == -1) {
            throw new IllegalArgumentException("Addon is not in list");
        }
        
        // Can't move up anymore, silly goose
        if (index == 0) {
            return false;
        }
        
        Collections.rotate(addons.subList(index - 1, index + 1), -1);
        fireTableChanged(new TableModelEvent(this, index - 1, index + 1));
        return true;
    }

    /**
     * Move an addon up in the order.
     * 
     * @param lower lower index, 0 to n-1
     * @param upper upper index, 0 to n-1
     * @return true if it could be moved
     * @throws IllegalArgumentException if the addon is not in the list
     * @throws IllegalArgumentException if the bounds are outside the scope of the list
     */
    public boolean moveUp(int lower, int upper) {
        if (upper < lower) {
            throw new IllegalArgumentException("Upper bound must be greater or equal to lower bound");
        }
        
        if (lower < 0 || upper > addons.size()) {
            throw new IllegalArgumentException("Bounds are outside the scope of the list");
        }
        
        // Can't move up anymore, silly goose
        if (lower == 0) {
            return false;
        }

        Collections.rotate(addons.subList(lower - 1, upper + 1), -1);
        fireTableChanged(new TableModelEvent(this, lower - 1, upper));
        return true;
    }

    /**
     * Move an addon down in the order.
     * 
     * @param addon addon
     * @return true if it could be moved
     * @throws IllegalArgumentException if the addon is not in the list
     */
    public boolean moveDown(Addon addon) {
        int index = addons.indexOf(addon);
        if (index == -1) {
            throw new IllegalArgumentException("Addon is not in list");
        }
        
        // Can't move down anymore, silly goose
        if (index == addons.size() - 1) {
            return false;
        }
        
        Collections.rotate(addons.subList(index, index + 1), -1);
        fireTableChanged(new TableModelEvent(this, index, index + 1));
        return true;
    }

    /**
     * Move an addon down in the order.
     * 
     * @param lower lower index, 0 to n-1
     * @param upper upper index, 0 to n-1
     * @return true if it could be moved
     * @throws IllegalArgumentException if the addon is not in the list
     * @throws IllegalArgumentException if the bounds are outside the scope of the list
     */
    public boolean moveDown(int lower, int upper) {
        if (upper < lower) {
            throw new IllegalArgumentException("Upper bound must be greater or equal to lower bound");
        }
        
        if (lower < 0 || upper > addons.size()) {
            throw new IllegalArgumentException("Bounds are outside the scope of the list");
        }
        
        // Can't move down anymore, silly goose
        if (upper == addons.size() - 1) {
            return false;
        }

        Collections.rotate(addons.subList(lower, upper + 2), 1);
        fireTableChanged(new TableModelEvent(this, lower, upper + 1));
        return true;
    }
    
    /**
     * Remove an addon. This also deletes the file.
     * 
     * @param addon addon
     * @return true if it was in the list
     */
    public boolean remove(Addon addon) {
        int index = addons.indexOf(addon);
        if (index == -1) {
            return false;
        }
        
        if (addons.remove(addon)) {
            fireTableChanged(new TableModelEvent(this));
            if (addon.getFile() != null) {
                addon.getFile().delete();
            }
        } else {
            // Should never happen
            throw new IllegalArgumentException("Tried to remove addon that was supposed to existed");
        }
        return true;
    }

    /**
     * Get a list of enabled addons.
     * 
     * @return addon list
     */
    public List<Addon> getEnabledAddons() {
        List<Addon> enabled = new ArrayList<Addon>();
        
        for (Addon addon : addons) {
            if (addon.isEnabled()) {
                enabled.add(addon);
            }
        }
        
        return enabled;
    }

    /**
     * Get the addon at a location.
     * 
     * @param i index of addon
     * @return addon, or null
     */
    public Addon getAddonAt(int i) {
        return addons.get(i);
    }

    @Override
    public int getRowCount() {
        return addons.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
        case 0:
            return "Enabled";
        case 1:
            return "Name";
        case 2:
            return "URL";
        default:
            return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case 0:
            return Boolean.class;
        case 1:
            return String.class;
        case 2:
            return URL.class;
        default:
            return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        switch (columnIndex) {
        case 0:
            return true;
        case 1:
            return true;
        case 2:
            return false;
        default:
            return false;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Addon addon = addons.get(rowIndex);
        if (addon == null) {
            return null;
        }
        switch (columnIndex) {
        case 0:
            return addon.isEnabled();
        case 1:
            return (addon.getFile() == null ? "<Missing>" : "") + addon.getName();
        case 2:
            return addon.getUrl();
        default:
            return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        Addon addon = addons.get(rowIndex);
        if (addon == null) {
            return;
        }
        switch (columnIndex) {
        case 0:
            addon.setEnabled((Boolean) value);
            break;
        case 1:
            addon.setName((String) value);
            break;
        case 2:
        default:
            break;
        }
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listenerList.add(TableModelListener.class, l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listenerList.remove(TableModelListener.class, l);
    }
    
    private void fireTableChanged(final TableModelEvent event) {
        final Object[] listeners = listenerList.getListenerList();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (int i = listeners.length - 2; i >= 0; i -= 2) {
                    ((TableModelListener) listeners[i + 1]).tableChanged(event);
                }
            }
        });
    }

}
