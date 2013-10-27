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

package com.sk89q.skmcl.install;

import com.sun.istack.internal.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Extracts a ZIP file to a given destination directory.
 */
public class ZipExtract implements Runnable {

    @Getter
    private final Resource resource;
    @Getter
    private final File destination;
    @Getter @Setter
    private List<String> exclude;

    /**
     * Create a new extractor instance.
     *
     * @param resource the resource to read from
     * @param destination the destination directory
     */
    public ZipExtract(@NotNull Resource resource, @NotNull File destination) {
        this.resource = resource;
        this.destination = destination;
    }

    @Override
    public void run() {
        InputStream is = null;

        try {
            is = resource.getInputStream();
            ZipInputStream zis = null;
            ZipEntry entry;

            destination.getParentFile().mkdirs();

            try {
                zis = new ZipInputStream(is);

                while ((entry = zis.getNextEntry()) != null) {
                    if (matches(entry)) {
                        File file = new File(getDestination(), entry.getName());
                        writeEntry(zis, file);
                    }
                }
            } finally {
                closeQuietly(zis);
            }

            resource.cleanup();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(is);
        }
    }

    /**
     * Checks if the given entry should be extracted.
     *
     * @param entry the entry
     * @return true if the entry matches the filter
     */
    private boolean matches(ZipEntry entry) {
        if (exclude != null) {
            for (String pattern : exclude) {
                if (entry.getName().startsWith(pattern)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Write an entry to a path.
     *
     * @param zis the input stream
     * @param path the path
     * @throws IOException thrown on I/O error
     */
    private void writeEntry(ZipInputStream zis, File path) throws IOException {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        try {
            path.getParentFile().mkdirs();

            fos = new FileOutputStream(path);
            bos = new BufferedOutputStream(fos);
            IOUtils.copy(zis, bos);
        } finally {
            closeQuietly(bos);
            closeQuietly(fos);
        }
    }

}
