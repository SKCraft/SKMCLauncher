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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "filegroup")
public class FileGroup {
    
    public enum VerificationMethod {
        @XmlEnumValue("md5") MD5;
    }
    
    private String source;
    private String dest;
    private VerificationMethod verify;

    private List<PackageFile> files = new ArrayList<PackageFile>();

    @XmlAttribute
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @XmlAttribute
    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    @XmlAttribute
    public VerificationMethod getVerify() {
        return verify;
    }

    public void setVerify(VerificationMethod verify) {
        this.verify = verify;
    }

    @XmlElements({ 
            @XmlElement(name = "archive", type = Archive.class),
            @XmlElement(name = "file", type = SingleFile.class) })
    public List<PackageFile> getFiles() {
        return files;
    }

    public void setFiles(List<PackageFile> files) {
        this.files = files;
    }

    public long getTotalSize() {
        long totalSize = 0;
        for (PackageFile file : files) {
            totalSize += file.getSize();
        }
        return totalSize;
    }

    public URL getURL(PackageFile file) {
        try {
            return new URL(getSource() + file.getFilename());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setDestDir(File dir) {
        for (PackageFile file : files) {
            file.setDestDir(new File(dir, getDest()));
        }
    }
    
    public MessageDigest createMessageDigest() throws NoSuchAlgorithmException {
        if (getVerify() == null) {
            return null;
        }
        return MessageDigest.getInstance(getVerify().name());
    }

}
