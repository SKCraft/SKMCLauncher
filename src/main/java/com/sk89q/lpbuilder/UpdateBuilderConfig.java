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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.sk89q.mclauncher.model.PackageManifest;
import com.sk89q.mclauncher.model.UpdateManifest;

@XmlRootElement(name = "config")
public class UpdateBuilderConfig {
    
    private List<FilePattern> filePatterns = new ArrayList<FilePattern>();
    private Templates templates = new Templates();
    
    @XmlElementWrapper(name = "files")
    @XmlElement(name = "pattern")
    public List<FilePattern> getFilePatterns() {
        return filePatterns;
    }
    
    public void setFilePatterns(List<FilePattern> filePatterns) {
        this.filePatterns = filePatterns;
    }

    @XmlElement(name = "templates")
    public Templates getTemplates() {
        return templates;
    }

    public void setTemplates(Templates templates) {
        this.templates = templates;
    }

    public static class Templates {
        private UpdateManifest updateManifest;
        private PackageManifest packageManifest;
    
        @XmlElement(name = "update")
        public UpdateManifest getUpdateManifest() {
            return updateManifest != null ? updateManifest : new UpdateManifest();
        }
        
        public void setUpdateManifest(UpdateManifest updateManifest) {
            this.updateManifest = updateManifest;
        }
    
        @XmlElement(name = "package")
        public PackageManifest getPackageManifest() {
            return packageManifest != null ? packageManifest : new PackageManifest();
        }
        
        public void setPackageManifest(PackageManifest packageManifest) {
            this.packageManifest = packageManifest;
        }
    }

}
