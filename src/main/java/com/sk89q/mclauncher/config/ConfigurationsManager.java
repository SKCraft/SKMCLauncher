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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * Knows about various configurations. This is definitely NOT thread-safe.
 * 
 * @author sk89q
 */
public class ConfigurationsManager implements Iterable<Configuration>, TableModel, ListModel  {
    
    private Map<String, Configuration> configurations = new HashMap<String, Configuration>();
    private List<Configuration> configurationsList = new ArrayList<Configuration>();
    private EventListenerList listenerList = new EventListenerList();
    private Configuration defaultConfiguration;

    /**
     * Get the default configuration.
     * 
     * @return default configuration
     */
    public Configuration getDefault() {
        return defaultConfiguration;
    }
    
    /**
     * Set the default configuration.
     * 
     * @param configuration configuration
     */
    public void setDefault(Configuration configuration) {
        defaultConfiguration = configuration;
    }
    
    /**
     * Get a configuration.
     * 
     * @param id configuration ID
     * @return configuration or null if not found
     */
    public Configuration get(String id) {
        return configurations.get(id);
    }
    
    /**
     * Register a configuration.
     * 
     * @param configuration configuration
     */
    public void register(Configuration configuration) {
        int index = configurationsList.indexOf(configuration);
        configurations.put(configuration.getId(), configuration);
        if (index == -1) {
            configurationsList.add(configuration);
            fireTableChanged(new TableModelEvent(this, configurationsList.size() - 1));
            fireListChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 
                    configurationsList.size() - 1, configurationsList.size() - 1));
        } else {
            fireTableChanged(new TableModelEvent(this, index));
            fireListChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 
                    0, configurationsList.size() - 1));
        }
    }
    
    /**
     * Remove a configuration.
     * 
     * @param configuration configuration
     * @return true if it was removed
     */
    public boolean remove(Configuration configuration) {
        int index = configurationsList.indexOf(configuration);
        if (index == -1) {
            return false;
        }
        configurations.remove(configuration.getId());
        configurationsList.remove(index);
        fireTableChanged(new TableModelEvent(this));
        fireListChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 
                0, configurationsList.size() - 1));
        return true;
    }
    
    /**
     * Notify that a configuration has been updated.
     * 
     * @param configuration
     * @return true if it was in the list
     */
    public boolean update(Configuration configuration) {
        int index = configurationsList.indexOf(configuration);
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
    public Configuration registerBuiltIn(String id, String name, String appDir, String urlString) {
        URL url = null;
        try {
            if (urlString != null) {
                url = new URL(urlString);
            }
        } catch (MalformedURLException e) {
        }
        Configuration config = get(id);
        if (config == null) {
            config = new Configuration(id, name, appDir, url);
            register(config);
        }
        config.setName(name);
        config.setAppDir(appDir);
        config.setUpdateUrl(url);
        config.setBuiltIn(true);
        register(config);
        return config;
    }
    
    /**
     * Get the map of configurations.
     * 
     * @return configuration map
     */
    public Map<String, Configuration> getConfigurations() {
        return configurations;
    }

    /**
     * Get a configuration at an index.
     * 
     * @param i index
     * @return configuration
     */
    public Configuration getConfigurationAt(int i) {
        return configurationsList.get(i);
    }

    /**
     * Get iterator.
     */
    @Override
    public Iterator<Configuration> iterator() {
        return configurations.values().iterator();
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
        Configuration configuration = configurationsList.get(rowIndex);
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
        Configuration configuration = configurationsList.get(rowIndex);
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

    @Override
    public int getSize() {
        return configurations.size();
    }

    @Override
    public Object getElementAt(int index) {
        return configurationsList.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listenerList.add(ListDataListener.class, l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
    }

}
