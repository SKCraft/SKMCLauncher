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

import java.io.File;
import java.net.URL;

public class Addon {
    
    private String id;
    private String version;
    private String name;
    private File file;
    private URL url;
    private boolean enabled = true;
    
    public Addon(String id, String name, String version, File file, URL url) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.file = file;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public int hashCode() {
        return file.getName().hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Addon)) {
            return false;
        }
        Addon other = (Addon) obj;
        return other.getFile().getName().equals(getFile().getName());
    }
    
}
