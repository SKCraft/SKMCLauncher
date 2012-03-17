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

package com.sk89q.mclauncher.launch;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import com.sk89q.mclauncher.Launcher;

public class LauncherRelauncher implements Runnable {
    
    private String originalAppData;

    public LauncherRelauncher(String originalAppData) {
        this.originalAppData = originalAppData;
    }

    @Override
    public void run() {
        String launcherPath;
        try {
            launcherPath = Launcher.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("The path to the launcher could not be discovered.", e);
        }
        
        ArrayList<String> params = new ArrayList<String>();
        params.add("javaw");
        params.add("-classpath");
        params.add(launcherPath);
        params.add(Launcher.class.getCanonicalName());
        ProcessBuilder procBuilder = new ProcessBuilder(params);
        if (originalAppData != null) {
            procBuilder.environment().put("APPDATA", originalAppData);
        }
        try {
            procBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException("The launcher could not be started: " + e.getMessage(), e);
        }
    }

}
