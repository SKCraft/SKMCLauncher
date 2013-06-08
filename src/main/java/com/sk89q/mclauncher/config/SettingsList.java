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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.sk89q.mclauncher.util.Util;

/**
 * Stores a list of settings that can inherit from a parent settings list if
 * this settings list does not contain the value. When setting values, it will
 * always be set to the settings list that it was called upon.
 */
@XmlRootElement(name = "settings")
public class SettingsList {

    private List<Setting> internalList = new ArrayList<Setting>();
    private SettingsList[] parents;
    private Map<String, String> settings = new HashMap<String, String>();
    
    public SettingsList() {
    }
    
    public SettingsList(SettingsList ... parent) {
        this.parents = parent;
    }
    
    @XmlElement(name = "setting")
    List<Setting> getInternalList() {
        internalList.clear();
        
        // Unable to get XmlAdapter to work correctly, so here's a hack

        // Copy list to map
        for (Entry<String, String> entry : settings.entrySet()) {
            Setting setting = new Setting();
            setting.key = entry.getKey();
            setting.value = entry.getValue();
            internalList.add(setting);
        }
        
        return internalList;
    }

    void setInternalList(List<Setting> internalList) {
        // Note! The JAXB library bundled with Java does NOT call this method when
        // unmarshalling -- it calls getInternalList() and then adds to the collection
        this.internalList = internalList;
    }

    @XmlTransient
    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    @XmlTransient
    public SettingsList[] getParents() {
        return parents;
    }

    public void setParents(SettingsList ... parent) {
        this.parents = parent;
    }
    
    public void unset(String name) {
        settings.remove(name);
    }

    public boolean has(String name) {
        return settings.containsKey(name); 
    }

    public void set(String name, String val) {
        if (val == null) {
            unset(name);
            return;
        }
        settings.put(name, val);
    }

    public void set(String name, boolean val) {
        set(name, val ? "true" : "false");
    }

    public void set(String name, int val) {
        set(name, String.valueOf(val));
    }

    public void set(String name, long val) {
        set(name, String.valueOf(val));
    }

    public void set(String name, double val) {
        set(name, String.valueOf(val));
    }

    public void set(String name, float val) {
        set(name, String.valueOf(val));
    }

    public void set(String name, Object val) {
        set(name, String.valueOf(val));
    }
    
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

    public String get(String name, String def) {
        return Util.defaultValue(get(name), def);
    }

    public boolean getBool(String name, boolean def) {
        String val = get(name);
        if (val == null) return def;
        return val.equalsIgnoreCase("true");
    }

    public int getInt(String name, int def) {
        String val = get(name);
        if (val == null) return def;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public long getLong(String name, long def) {
        String val = get(name);
        if (val == null) return def;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public double getDouble(String name, double def) {
        String val = get(name);
        if (val == null) return def;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }
    
    public float getFloat(String name, float def) {
        String val = get(name);
        if (val == null) return def;
        try {
            return Float.parseFloat(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    void afterUnmarshal(Unmarshaller u, Object parent) {
        settings.clear();
        for (Setting setting : internalList) {
            settings.put(setting.key, setting.value);
        }
    }
    
}
