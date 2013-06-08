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

package com.sk89q.mclauncher.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;

/**
 * Knows about various configurations. This is definitely NOT thread-safe.
 */
public class ConfigurationList implements Iterable<Configuration>, TableModel, ListModel  {
    
    private List<Configuration> configurations = new ArrayList<Configuration>();
    private EventListenerList listenerList = new EventListenerList();
    
    @XmlElement(name = "configuration")
    public List<Configuration> getConfigurations() {
        if (configurations.size() == 0) {
            Constants.register(this);
        }
        
        return configurations;
    }

    public void setConfigurations(List<Configuration> configurations) {
        this.configurations = configurations;

        if (configurations.size() == 0) {
            Constants.register(this);
        }
    }

    /**
     * Get the configuration used at startup.
     * 
     * @return the configuration
     */
    public Configuration getStartupConfiguration() {
        return getConfigurations().get(0);
    }

    /**
     * Get a configuration by a given ID.
     * 
     * @param id the ID
     * @return a configuration matching the ID, or null if there's no such configuration
     */
    public Configuration get(String id) {
        for (Configuration configuration : configurations) {
            if (configuration.getId().equals(id)) {
                return configuration;
            }
        }
        
        return null;
    }
    
    /**
     * Register a configuration.
     * 
     * @param configuration configuration
     */
    public void register(Configuration configuration) {
        int index = configurations.indexOf(configuration);
        if (index == -1) {
            configurations.add(configuration);
            fireTableChanged(new TableModelEvent(this, configurations.size() - 1));
            fireListChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 
                    configurations.size() - 1, configurations.size() - 1));
        } else {
            fireTableChanged(new TableModelEvent(this, index));
            fireListChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 
                    0, configurations.size() - 1));
        }
    }
    
    /**
     * Remove a configuration.
     * 
     * @param configuration configuration
     * @return true if it was removed
     */
    public boolean remove(Configuration configuration) {
        int index = configurations.indexOf(configuration);
        if (index == -1) {
            return false;
        }
        configurations.remove(index);
        fireTableChanged(new TableModelEvent(this));
        fireListChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 
                0, configurations.size() - 1));
        return true;
    }
    
    /**
     * Notify that a configuration has been updated.
     * 
     * @param configuration
     * @return true if it was in the list
     */
    public boolean update(Configuration configuration) {
        int index = configurations.indexOf(configuration);
        if (index == -1) {
            return false;
        }
        fireTableChanged(new TableModelEvent(this, index));
        fireListChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index, index));
        return true;
    }
    
    /**
     * Register a built-in configuration. If a configuration already exists
     * by the given ID, the old one will be updated.
     * 
     * @param id id
     * @param name name
     * @param appDir app directory
     * @param urlString update URL or null for none
     * @return configuration
     */
    @SuppressWarnings("deprecation")
    public Configuration registerBuiltIn(String id, String name, String appDir,
            String urlString) {
        URL url = null;
        try {
            if (urlString != null) {
                url = new URL(urlString);
            }
        } catch (MalformedURLException e) {
        }
        
        Configuration config = get(id);
        if (config == null) {
            config = Configuration.createGlobal(id, name, appDir, url);
            register(config);
        }
        config.setName(name);
        config.setAppDir(appDir);
        config.setUpdateUrl(url);
        config.setBuiltIn(true);
        register(config);
        return config;
    }
    
    public void sortByDate() {
        Collections.sort(configurations);
        fireTableChanged(new TableModelEvent(this));
        fireListChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 
                0, configurations.size() - 1));
    }

    public Configuration getConfigurationAt(int i) {
        return configurations.get(i);
    }
    
    @Override
    public Iterator<Configuration> iterator() {
        return configurations.iterator();
    }

    @Override
    public int getRowCount() {
        return configurations.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
        case 0:
            return "Name";
        case 1:
            return "Path";
        default:
            return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case 0:
            return String.class;
        case 1:
            return String.class;
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
            return false;
        default:
            return false;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Configuration configuration = configurations.get(rowIndex);
        if (configuration == null) {
            return null;
        }
        switch (columnIndex) {
        case 0:
            return configuration.getName();
        case 1:
            return configuration.getBaseDir();
        default:
            return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        Configuration configuration = configurations.get(rowIndex);
        if (configuration == null) {
            return;
        }
        switch (columnIndex) {
        case 0:
            configuration.setName((String) value);
            break;
        default:
            break;
        }
    }

    @Override
    public int getSize() {
        return configurations.size();
    }

    @Override
    public Object getElementAt(int index) {
        return configurations.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listenerList.add(ListDataListener.class, l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
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
                    if (listeners[i] == TableModelListener.class) {
                        ((TableModelListener) listeners[i + 1]).tableChanged(event);
                    }
                }
            }
        });
    }
    
    private void fireListChanged(final ListDataEvent event) {
        final Object[] listeners = listenerList.getListenerList();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (int i = listeners.length - 2; i >= 0; i -= 2) {
                    if (listeners[i] == ListDataListener.class) {
                        ((ListDataListener) listeners[i + 1]).contentsChanged(event);
                    }
                }
            }
        });
    }

    void afterUnmarshal(Unmarshaller u, Object parent) {
        Constants.register(this);
        sortByDate();
    }

}
