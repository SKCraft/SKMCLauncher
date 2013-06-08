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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sk89q.mclauncher.model.Archive;
import com.sk89q.mclauncher.util.Util;

class ZipBucket extends Archive {
    
    private final List<QueuedFile> contents = new ArrayList<QueuedFile>();

    void queue(File file) {
        QueuedFile q = new QueuedFile();
        q.file = file;
        contents.add(q);
    }

    public void writeContents(File baseDir, File target) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }

        // Make more deterministic
        for (QueuedFile q : contents) {
            q.relPath = getRelative(baseDir, q.file);
        }
        Collections.sort(contents);
        
        FileOutputStream fos = null;
        ZipOutputStream zip = null;
        FileInputStream fis = null;
        byte[] buf = new byte[1024 * 8];
        
        try {
            fos = new FileOutputStream(target);
            zip = new ZipOutputStream(fos);

            for (QueuedFile q : contents) {
                zip.putNextEntry(new ZipEntry(q.relPath));
                
                // Add the path and digest of the file to this ZIP's digest
                digest.update(q.relPath.getBytes());
                digest.update((byte) 0);
                digest.update(UpdateBuilder.getVersionDigest(q.file));
                
                fis = new FileInputStream(q.file);
                int count;
                while ((count = fis.read(buf)) > 0) {
                    zip.write(buf, 0, count);
                }
                Util.close(fis);
            }
            
            // Set the version
            setVersion(Util.getHexString(digest.digest()));
        } finally {
            Util.close(zip);
            Util.close(fos);
            Util.close(fis);
        }
    }

    private static String getRelative(File base, File path) {
        return base.toURI().relativize(path.toURI()).getPath();
    }
    
    private static class QueuedFile implements Comparable<QueuedFile> {
        public String relPath;
        public File file;

        @Override
        public int compareTo(QueuedFile o) {
            return relPath.compareTo(o.relPath);
        }
    }

}
