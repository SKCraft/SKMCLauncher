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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sk89q.mclauncher.util.LauncherUtils;

/**
 * Holds a list of entries to uninstall.
 * 
 * @author sk89q
 */
public class UninstallLog {
    
    private Map<String, Set<String>> entries = new HashMap<String, Set<String>>();
    private Set<String> cache = new HashSet<String>();
    private File baseDir;
    
    /**
     * Get the base directory.
     * 
     * @return dir
     */
    public File getBaseDir() {
        return baseDir;
    }

    /**
     * Set the base dir for relative paths.
     * 
     * @param baseDir base dir
     */
    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Add an entry.
     * 
     * @param group group name
     * @param entry entry
     */
    public void add(String group, String entry) {
        cache.add(entry);
        Set<String> subentries = entries.get(group);
        if (subentries == null) {
            subentries = new HashSet<String>();
            entries.put(group, subentries);
        }
        subentries.add(entry);
    }

    /**
     * Add an entry.
     * 
     * @param group name
     * @param entry entry
     */
    public void add(File group, File entry) {
        add(relativize(group), relativize(entry));
    }
    
    /**
     * Returns whether an entry exists in the log.
     * 
     * @param entry entry
     * @return 
     */
    public boolean has(String entry) {
        return cache.contains(entry);
    }
    
    /**
     * Returns whether an entry exists in the log.
     * 
     * @param entry entry
     * @return 
     */
    public boolean has(File entry) {
        return has(relativize(entry));
    }

    /**
     * Copy a group from another log.
     * 
     * @param other other log
     * @param group group name
     * @return true if the group existed
     */
    public boolean copyGroupFrom(UninstallLog other, String group) {
        Set<String> otherSet = other.entries.get(group);
        if (otherSet == null) {
            return false;
        }
        for (String entry : otherSet) {
            add(group, entry);
        }        
        return true;
    }

    /**
     * Copy a group from another log.
     * 
     * @param other other log
     * @param entry group name
     * @return true if the group existed
     */
    public boolean copyGroupFrom(UninstallLog other, File entry) {
        return copyGroupFrom(other, relativize(entry));
    }
    
    /**
     * Get the entry set.
     * 
     * @return entry set
     */
    public Set<Map.Entry<String, Set<String>>> getEntrySet() {
        return entries.entrySet();
    }
    
    /**
     * Returns whether this log has a given group defined.
     * 
     * @param group group name
     * @return whether the group exists
     */
    public boolean hasGroup(String group) {
        return entries.containsKey(group);
    }
    
    /**
     * Read the log from a file.
     * 
     * @param file file
     * @throws IOException on I/O error
     */
    public void read(File file) throws IOException {
        entries = new HashMap<String, Set<String>>();
        cache = new HashSet<String>();
        
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                if (!line.contains(":")) continue;
                add(line.substring(0, line.indexOf(':')),
                        line.substring(line.indexOf(':') + 1, line.length()));
            }
        } finally {
            LauncherUtils.close(reader);
        }
    }

    /**
     * Write the log to a file.
     * 
     * @param file file
     * @throws IOException on I/O error
     */
    public void write(File file) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (Map.Entry<String, Set<String>> entry : entries.entrySet()) {
                for (String subentry : entry.getValue()) {
                    writer.append(entry.getKey());
                    writer.append(":");
                    writer.append(subentry);
                    writer.newLine();
                }
            }
        } finally {
            LauncherUtils.close(writer);
        }
    }
    
    /**
     * Gets the relative path.
     * 
     * @param child child
     * @return path string
     * @throws IllegalArgumentException child is not in base
     */
    private String relativize(File child) {
        if (baseDir == null) {
            throw new IllegalArgumentException("base directory is not set");
        }
        URI uri = child.toURI();
        String relative = baseDir.toURI().relativize(uri).getPath();
        if (relative == uri.toString()) {
            throw new IllegalArgumentException("Child path not in base");
        }
        return relative;
    }
}
