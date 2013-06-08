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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public class ServerEntry {
    
    private String name;
    private String address;
    private transient boolean temporary;
    
    public ServerEntry() {
    }
    
    public ServerEntry(String name, String address) {
        setName(name);
        setAddress(address);
    }

    @XmlElement
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    @XmlElement
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    @XmlTransient
    public boolean isTemporary() {
        return temporary;
    }
    
    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().toLowerCase().hashCode() : -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof ServerEntry) {
            return getName() != null ? getName().toLowerCase().equals(
                    ((ServerEntry) obj).getName().toLowerCase()) : false;
        } else {
            return false;
        }
    }

}
