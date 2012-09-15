package com.sk89q.mclauncher;

import com.sk89q.mclauncher.util.Util;
import java.io.File;

/**
 * Represents a jar that contains the main Minecraft game.
 *
 * @author md_5
 */
public class MinecraftJar {

    private final File file;
    private String version;

    public MinecraftJar(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return file.getName();
    }

    public String getVersion() {
        return (version == null) ? (version = Util.getMCVersion(file)) : version;
    }

    @Override
    public String toString() {
        return file.getName() + " (" + getVersion() + ")";
    }
}
