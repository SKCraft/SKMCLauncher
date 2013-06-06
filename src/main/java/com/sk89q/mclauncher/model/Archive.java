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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.sk89q.mclauncher.update.UninstallLog;
import com.sk89q.mclauncher.util.Util;

public class Archive extends PackageFile {

    @Override
    public void deploy(UninstallLog log) throws IOException {
        File parent = getFile().getParentFile();
        
        if (getFinalFilename().endsWith(".zip") || getFinalFilename().endsWith(".jar")) {
            InputStream inputStream = null;
            JarInputStream zip = null;
            
            try {
                inputStream = getInputStream();
                zip = new JarInputStream(inputStream);
                
                JarEntry entry;
                while ((entry = zip.getNextJarEntry()) != null) {
                    if (entry.isDirectory() || isMetaInf(entry.getName())) {
                        continue;
                    }
                    
                    BufferedOutputStream out = null;
                    try {
                        File target = new File(getFile().getParentFile(), entry.getName());
                        checkSubchild(parent, target);
                        log.add(getFile(), target);
                        target.getParentFile().mkdirs();
                        out = new BufferedOutputStream(
                                new FileOutputStream(target));
            
                        byte[] buffer = new byte[8192];
            
                        int ret = zip.read(buffer, 0, buffer.length);
                        while (ret >= 1) {
                            out.write(buffer, 0, ret);
                            ret = zip.read(buffer, 0, buffer.length);
                        }
                    } finally {
                        Util.close(out);
                    }
                }
            } finally {
                Util.close(zip);
                Util.close(inputStream);
            }
            
            getTempFile().delete();
        } else {
            throw new IOException("Do not know how to extract " + getFilename());
        }
    }
    
    /**
     * Returns whether the given path is in META-INF.
     * 
     * @param name name of entry
     * @return true if META-INF
     */
    private static boolean isMetaInf(String name) {
        for (String part : name.split("[/\\\\]")) {
            if (part.equalsIgnoreCase("META-INF")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check to see whether the given file is within another give path.
     * 
     * @param root folder that the child has to be under
     * @param child child file
     * @throws IOException on I/O error
     */
    private void checkSubchild(File root, File child) throws IOException {
        if (!child.toString().startsWith(root.toString())) {
            throw new IOException(String.format("%s not in %s; invalid path",
                    child.toString(), root.toString()));
        }
    }
    
}
