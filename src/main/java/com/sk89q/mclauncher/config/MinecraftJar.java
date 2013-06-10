package com.sk89q.mclauncher.config;

import java.io.File;

import com.sk89q.mclauncher.util.LauncherUtils;

/**
 * Represents a jar that contains the main Minecraft game.
 */
public class MinecraftJar {

    private final File file;
    private String version;

    public MinecraftJar(File f) {
        this.file = f;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return file.getName();
    }

    public String getVersion() {
        return (version == null) ? (version = LauncherUtils.getMCVersion(file)) : version;
    }

    @Override
    public String toString() {
        return file.getName() + " (" + getVersion() + ")";
    }
}
