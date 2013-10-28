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

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.concurrent.Callable;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Copies a file to another location.
 */
@ToString
public class FileCopy implements Runnable, Callable<File> {

    @Getter
    private final Resource resource;
    @Getter
    private final File destination;

    public FileCopy(@NonNull Resource resource, @NonNull File destination) {
        this.resource = resource;
        this.destination = destination;
    }

    @Override
    public File call() throws Exception {
        InputStream is = resource.getInputStream();
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        destination.getParentFile().mkdirs();

        try {
            fos = new FileOutputStream(destination);
            bos = new BufferedOutputStream(fos);
            IOUtils.copy(is, bos);
        } finally {
            closeQuietly(is);
            closeQuietly(bos);
            closeQuietly(fos);
        }

        resource.cleanup();

        return destination;
    }

    @Override
    public void run() {
        try {
            call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
