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

package com.sk89q.lpbuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.sk89q.mclauncher.model.PackageFile;
import com.sk89q.mclauncher.update.UninstallLog;

public class FilePattern extends PackageFile {

    private String archiveName;
    private List<PathPattern> pathPatterns = new ArrayList<PathPattern>();

    @XmlAttribute(name = "archive")
    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    @XmlElements({
        @XmlElement(name = "include", type = PathPattern.Include.class),
        @XmlElement(name = "exclude", type = PathPattern.Exclude.class)
    })
    public List<PathPattern> getPathPatterns() {
        return pathPatterns;
    }

    public void setPathPatterns(List<PathPattern> pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    public boolean matchesPath(String path) {
        return PathPattern.matches(getPathPatterns(), path);
    }

    @Override
    public void deploy(UninstallLog log) throws IOException {
        throw new UnsupportedOperationException("This object cannot be used to deploy");
    }

    @Override
    public String toString() {
        return String.format(
                "Pattern(component=%s, overwrite=%s, platform=%s, archive=%s)",
                getComponentFilter(), getOverwrite(), getPlatform(), getArchiveName());
    }
    
}
