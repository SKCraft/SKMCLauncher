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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages the server hot list.
 * 
 * @author sk89q
 */
public class ServerHotListManager {
    
    private Map<String, String> hosts = new HashMap<String, String>();
    private Set<String> builtIn = new HashSet<String>();
    
    /**
     * Register a server.
     * 
     * @param name name of server
     * @param address address
     * @param isBuiltIn true if built in
     */
    public void register(String name, String address, boolean isBuiltIn) {
        hosts.put(name, address);
        if (isBuiltIn) {
            builtIn.add(name);
        }
    }
    
    /**
     * Returns whether the entry is built in.
     * 
     * @param name name of server
     * @return true if built in
     */
    public boolean isBuiltIn(String name) {
        return builtIn.contains(name);
    }
    
    /**
     * Get the address.
     * 
     * @param name name
     * @return address or null
     */
    public String get(String name) {
        return hosts.get(name);
    }
    
    /**
     * Get a list of server names.
     * 
     * @return list of server names
     */
    public Set<String> getServerNames() {
        return hosts.keySet();
    }

}
