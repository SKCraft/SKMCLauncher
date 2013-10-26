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

import lombok.ToString;

import java.io.*;

import static com.sk89q.mclauncher.util.LauncherUtils.close;

@ToString
public class FileCopy implements Runnable {

    private final InstallerRuntime runtime;
    private final Resource resource;
    private final File destination;

    private boolean logInstall = true;

    FileCopy(InstallerRuntime runtime, Resource resource, File destination) {
        this.runtime = runtime;
        this.resource = resource;
        this.destination = destination;
    }

    public FileCopy skipLog() {
        logInstall = false;
        return this;
    }

    @Override
    public void run() {
        try {
            InputStream is = resource.getInputStream();
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;

            destination.getParentFile().mkdirs();

            try {
                fos = new FileOutputStream(destination);
                bos = new BufferedOutputStream(fos);

                byte[] buffer = new byte[16384];

                int ret = is.read(buffer);
                while (ret >= 1) {
                    bos.write(buffer, 0, ret);
                    ret = is.read(buffer);
                }
            } finally {
                close(is);
                close(bos);
                close(fos);
            }

            resource.cleanup();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
