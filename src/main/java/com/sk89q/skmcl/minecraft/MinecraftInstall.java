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

package com.sk89q.skmcl.minecraft;

import com.sk89q.skmcl.application.Instance;
import com.sk89q.skmcl.launch.JavaProcessBuilder;
import com.sk89q.skmcl.launch.LaunchEnvironment;
import com.sk89q.skmcl.minecraft.model.Library;
import com.sk89q.skmcl.minecraft.model.ReleaseManifest;
import com.sk89q.skmcl.profile.Profile;
import com.sk89q.skmcl.util.Environment;
import com.sk89q.skmcl.util.ProgressEvent;
import lombok.Getter;
import org.codehaus.jackson.map.ObjectMapper;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * An installed version of Minecraft.
 */
public class MinecraftInstall implements Instance {

    @Getter
    private final Profile profile;
    @Getter
    private final Release version;
    @Getter
    private final String versionPath;
    @Getter
    private final Environment environment;

    /**
     * Create a new instance.
     *
     * @param profile the hosting profile
     * @param version the version
     * @param environment the environment
     */
    public MinecraftInstall(Profile profile, Release version, Environment environment) {
        this.profile = profile;
        this.version = version;
        this.environment = environment;
        versionPath = String.format("versions/%1$s/%1$s", version.getId());
    }

    public File getManifestPath() {
        return new File(getProfile().getContentDir(), versionPath + ".json");
    }

    public File getJarPath() {
        return new File(getProfile().getContentDir(), versionPath + ".jar");
    }

    public File getAssetsDir() {
        return new File(getProfile().getSharedDir(), "assets");
    }

    @Override
    public SwingWorker<Instance, ProgressEvent> getUpdater() {
        return new MinecraftUpdater(this);
    }

    @Override
    public Process launch(LaunchEnvironment environment) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JavaProcessBuilder process = new JavaProcessBuilder();
        ReleaseManifest manifest = mapper.readValue(
                getManifestPath(), ReleaseManifest.class);

        for (Library library : manifest.getLibraries()) {
            File path = new File(getProfile().getContentDir(),
                    "libraries/" + library.getPath(environment.getEnvironment()));
            if (path.exists()) {
                process.classPath(path);
            }
        }

        process.classPath(getJarPath());
        process.setMainClass(manifest.getMainClass());

        System.out.println(process.buildCommand());

        ProcessBuilder builder = new ProcessBuilder(process.buildCommand());
        builder.directory(getProfile().getContentDir());
        return builder.start();
    }

    @Override
    public String toString() {
        return "MinecraftInstall{" +
                "version=" + version +
                '}';
    }
}
