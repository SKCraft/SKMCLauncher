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

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public class ServerList {

    private Set<ServerEntry> servers = new HashSet<ServerEntry>();

    @XmlTransient
    public Set<ServerEntry> getServers() {
        return servers;
    }

    public void setServers(Set<ServerEntry> servers) {
        this.servers = servers;
    }
    
    public int size() {
        return servers.size();
    }

    public boolean contains(Object o) {
        return servers.contains(o);
    }

    public boolean add(ServerEntry e) {
        return servers.add(e);
    }

    public boolean remove(Object o) {
        return servers.remove(o);
    }

    public void clear() {
        servers.clear();
    }

    /**
     * For internal use. Don't call.
     * 
     * @return the list of saved servers
     */
    @XmlElement(name = "server")
    @Deprecated
    public Set<ServerEntry> getSavedServers() {
        Set<ServerEntry> saved = new HashSet<ServerEntry>();
        for (ServerEntry entry : servers) {
            if (!entry.isTemporary()) {
                saved.add(entry);
            }
        }
        
        return saved;
    }

    /**
     * For internal use. Don't call.
     * 
     * @param servers the list of saved servers
     */
    @Deprecated
    public void setSavedServers(Set<ServerEntry> servers) {
        setServers(servers);
    }

    public void register(String name, String address, boolean temporary) {
        ServerEntry entry = new ServerEntry();
        entry.setName(name);
        entry.setAddress(address);
        entry.setTemporary(temporary);
        getServers().add(entry);
    }

}
