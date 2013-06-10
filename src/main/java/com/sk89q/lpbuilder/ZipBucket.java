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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sk89q.lpbuilder.FileSignatureBuilder.SignatureList;
import com.sk89q.mclauncher.model.Archive;
import com.sk89q.mclauncher.util.LauncherUtils;

class ZipBucket extends Archive {
    
    private final List<File> contents = new ArrayList<File>();

    void queue(File file) {
        contents.add(file);
    }

    public void writeContents(FileSignatureBuilder builder, File baseDir, File target)
            throws IOException {
        SignatureList list = builder.createList();
        
        FileOutputStream fos = null;
        ZipOutputStream zip = null;
        FileInputStream fis = null;
        byte[] buf = new byte[1024 * 8];
        
        try {
            fos = new FileOutputStream(target);
            zip = new ZipOutputStream(fos);

            for (File file : contents) {
                String relativePath = getRelative(baseDir, file);
                zip.putNextEntry(new ZipEntry(relativePath));
                list.add(relativePath, builder.fromFile(file));
                
                fis = new FileInputStream(file);
                int count;
                while ((count = fis.read(buf)) > 0) {
                    zip.write(buf, 0, count);
                }
                LauncherUtils.close(fis);
            }
            
            // Set the version
            setVersion(list.toDigest());
        } finally {
            LauncherUtils.close(zip);
            LauncherUtils.close(fos);
            LauncherUtils.close(fis);
        }
    }

    private static String getRelative(File base, File path) {
        return base.toURI().relativize(path.toURI()).getPath();
    }

}
