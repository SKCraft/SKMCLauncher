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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.Platform;
import com.sk89q.mclauncher.update.FileStreamFilters;
import com.sk89q.mclauncher.update.UninstallLog;

public abstract class PackageFile {
    
    public enum ExistingFilePolicy {
        @XmlEnumValue("never") NEVER_OVERWRITE;
    }
    
    private long size;
    private Platform platform;
    private String filename;
    private String finalFilename;
    private String version;
    private ExistingFilePolicy overwrite;

    private transient boolean ignored;
    private transient String[] filterExts;
    private transient File file;
    private transient File tempFile;
    
    @XmlAttribute
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }

    @XmlAttribute
    public Platform getPlatform() {
        return platform;
    }
    
    public void setPlatform(Platform platform) {
        this.platform = platform;
    }
    
    @XmlValue
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
        parseName();
    }
    
    @XmlTransient
    public String getFinalFilename() {
        return finalFilename;
    }

    @XmlAttribute
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @XmlAttribute
    public ExistingFilePolicy getOverwrite() {
        return overwrite;
    }

    public void setOverwrite(ExistingFilePolicy overwrite) {
        this.overwrite = overwrite;
    }

    private void parseName() {
        String[] parts = getFilename().split("\\.");
        List<String> filterExts = new ArrayList<String>();
        StringBuilder s = new StringBuilder();
        for (int i = parts.length - 1; i > 0; i--) {
            String part = parts[i];
            if (FileStreamFilters.get(part) != null) {
                filterExts.add(part);
            } else {
                for (int j = 0; j <= i; j++) {
                    if (j != 0) {
                        s.append(".");
                    }
                    s.append(parts[j]);
                }
                break;
            }
        }
        
        this.filterExts = new String[filterExts.size()];
        this.filterExts = filterExts.toArray(this.filterExts);
        this.finalFilename = s.toString();
    }

    @XmlTransient
    public boolean isFiltered() {
        return filterExts.length > 0;
    }

    @XmlTransient
    public File getFile() {
        return file;
    }

    @XmlTransient
    public File getTempFile() {
        return tempFile;
    }

    @XmlTransient
    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public boolean matchesEnvironment() {
        return getPlatform() == null || getPlatform() == Platform.UNKNOWN
                || getPlatform() == Launcher.getPlatform();
    }

    @XmlTransient
    public InputStream getInputStream() throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(getTempFile()));
        for (String filterExt : filterExts) {
            in = FileStreamFilters.get(filterExt).filter(in);
        }
        return in;
    }

    public abstract void deploy(UninstallLog log) throws IOException;

    public void setDestDir(File dir) {
        file = new File(dir, getFinalFilename());

        if (!file.toString().startsWith(dir.toString())) {
            throw new RuntimeException("Path '" + file.getAbsolutePath()
                    + "' is not in '" + dir.getAbsolutePath() + "'");
        }

        tempFile = new File(file.getAbsolutePath() + ".tmp");
    }

}
