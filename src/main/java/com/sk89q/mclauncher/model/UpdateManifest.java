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

package com.sk89q.mclauncher.model;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.sk89q.mclauncher.config.Configuration;

@XmlRootElement(name = "update")
public class UpdateManifest {
    
    private String id;
    private String name;
    private String latestVersion;
    private String packageUrl;
    
    @XmlElement
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }

    @XmlElement
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "latest")
    public String getLatestVersion() {
        return latestVersion;
    }
    
    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    @XmlElement(name = "packageurl")
    public String getPackageURL() {
        return packageUrl;
    }
    
    public void setPackageURL(String packageUrl) {
        this.packageUrl = packageUrl;
    }

    public URL toPackageURL() throws MalformedURLException {
        return new URL(getPackageURL());
    }

    public boolean isValidForInstall() {
        return getId() != null && Configuration.isValidId(getId()) &&
                getName() != null && getName().length() > 0 && getName().length() <= 60;
    }
    
}