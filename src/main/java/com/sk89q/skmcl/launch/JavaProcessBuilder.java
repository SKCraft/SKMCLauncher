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

package com.sk89q.skmcl.launch;

import com.sk89q.mclauncher.util.JavaRuntimeFinder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ToString
public class JavaProcessBuilder {

    @Getter @Setter
    private File jrePath = JavaRuntimeFinder.findBestJavaPath();
    @Getter @Setter
    private String minMemory;
    @Getter @Setter
    private String maxMemory;
    @Getter @Setter
    private String permGen;
    @Getter
    private final List<File> classPath = new ArrayList<File>();
    @Getter
    private final List<String> flags = new ArrayList<String>();
    @Getter
    private final List<String> args = new ArrayList<String>();
    @Getter @Setter
    private String mainClass;

    public void tryJrePath(File path) throws IOException {
        // Try the parent directory
        if (!path.exists()) {
            throw new IOException(
                    "The configured Java runtime path '" + path + "' doesn't exist.");
        } else if (path.isFile()) {
            path = path.getParentFile();
        }

        File binDir = new File(path, "bin");
        if (binDir.isDirectory()) {
            path = binDir;
        }

        setJrePath(path);
    }

    public JavaProcessBuilder classPath(File file) {
        getClassPath().add(file);
        return this;
    }

    public JavaProcessBuilder classPath(String path) {
        getClassPath().add(new File(path));
        return this;
    }

    public String buildClassPath() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;

        for (File file : classPath) {
            if (first) {
                first = false;
            } else {
                builder.append(File.pathSeparator);
            }

            builder.append(file.getAbsolutePath());
        }

        return builder.toString();
    }

    public List<String> buildCommand() {
        List<String> command = new ArrayList<String>();

        command.add(getJrePath() + File.separator + "java");

        for (String flag : flags) {
            command.add(flag);
        }

        command.add("-cp");
        command.add(buildClassPath());

        command.add(mainClass);

        for (String arg : args) {
            command.add(arg);
        }

        return command;
    }

}
