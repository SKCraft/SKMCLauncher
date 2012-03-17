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

package com.sk89q.mclauncher.config;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.addons.AddonsProfile;
import com.sk89q.mclauncher.util.SettingsList;

/**
 * Represents a configuration for the game.
 * 
 * @author sk89q
 */
public class Configuration {
    private String id;
    private File customBasePath;
    private String appDir;
    private String name;
    private URL updateUrl;
    private String lastActiveJar;
    private SettingsList settings = new SettingsList();
    private boolean builtIn = false;
    
    /**
     * Construct a configuration.
     * 
     * @param id id
     * @param name name
     * @param appDir data directory name
     * @param updateUrl URL to update from, or null to use default
     */
    public Configuration(String id, String name, String appDir, URL updateUrl) {
        if (!id.matches("^[A-Za-z0-9\\-]+{1,64}$")) {
            throw new IllegalArgumentException("Invalid configuration name");
        }
        this.id = id;
        setName(name);
        setAppDir(appDir);
        setUpdateUrl(updateUrl);
    }
    
    /**
     * Construct a configuration.
     * 
     * @param id id
     * @param name name
     * @param customBasePath base path to use
     * @param updateUrl URL to update from, or null to use default
     */
    public Configuration(String id, String name, File customBasePath, URL updateUrl) {
        if (!id.matches("^[A-Za-z0-9\\-]+{1,64}$")) {
            throw new IllegalArgumentException("Invalid configuration name");
        }
        this.id = id;
        setName(name);
        setCustomBasePath(customBasePath);
        setUpdateUrl(updateUrl);
    }

    /**
     * Get the ID.
     * 
     * @return ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get the name.
     * 
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name.
     * 
     * @param name name
     */
    public void setName(String name) {
        if (!name.matches("^.{1,32}$")) {
            throw new IllegalArgumentException("Invalid name");
        }
        this.name = name;
    }
    
    /**
     * Returns whether the default base path is used.
     * 
     * @return true if the default path is used
     */
    public boolean isUsingDefaultPath() {
        return customBasePath == null && appDir == null;
    }

    /**
     * Get the custom base path. A custom base path, if set, is the base
     * directory that is used, otherwise the application directory field
     * is used instead.
     * 
     * @return path or null
     */
    public File getCustomBasePath() {
        return customBasePath;
    }

    /**
     * Set the custom base path. A custom base path, if set, is the base
     * directory that is used, otherwise the application directory field
     * is used instead.
     * 
     * @param customBasePath path or null
     */
    public void setCustomBasePath(File customBasePath) {
        this.customBasePath = customBasePath;
    }

    /**
     * Get the data directory name.
     * 
     * @see #setCustomBasePath(File) can override
     * @return name or null for default
     */
    public String getAppDir() {
        return appDir;
    }

    /**
     * Set the data directory name.
     * 
     * @see #setCustomBasePath(File) can override
     * @param appDir name
     */
    public void setAppDir(String appDir) {
        if (appDir != null && !appDir.matches("^[A-Za-z0-9\\-_]+{1,32}$")) {
            throw new IllegalArgumentException("Invalid data directory name");
        }
        this.appDir = appDir;
    }

    /**
     * Get the update URL.
     * 
     * @return url or null for default
     */
    public URL getUpdateUrl() {
        return updateUrl;
    }

    /**
     * Set the update URL.
     * 
     * @param updateUrl url or null for default
     */
    public void setUpdateUrl(URL updateUrl) {
        this.updateUrl = updateUrl;
    }
    
    /**
     * Get whether this configuration is built-in.
     * 
     * @return built-in status
     */
    public boolean isBuiltIn() {
        return builtIn;
    }

    /**
     * Set whether this configuration is built-in.
     * 
     * @param builtIn built-in status
     */
    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    /**
     * Get the last active JAR.
     * 
     * @return JAR name or null
     */
    public String getLastActiveJar() {
        return lastActiveJar;
    }

    /**
     * Set the last active jar.
     * 
     * @param lastActiveJar JAR name or null
     */
    public void setLastActiveJar(String lastActiveJar) {
        this.lastActiveJar = lastActiveJar;
    }

    /**
     * Get the settings.
     * 
     * @return settings
     */
    public SettingsList getSettings() {
        return settings;
    }

    /**
     * Set settings.
     * 
     * @param settings settings
     */
    public void setSettings(SettingsList settings) {
        this.settings = settings;
    }

    /**
     * Shortcut method to get the base working directory.
     * 
     * @return directory
     */
    public File getBaseDir() {
        File path;
        if (getCustomBasePath() != null) {
            return getCustomBasePath();
        } else if (getAppDir() == null) {
            path = Launcher.getAppDataDir();
        } else {
            path = Launcher.getAppDataDir(getAppDir());
        }
        
        path.mkdirs();
        return path;
    }

    /**
     * Shortcut method to get the directory that Minecraft will actually use.
     * 
     * @return directory
     */
    public File getMinecraftDir() {
        File path;
        if (getCustomBasePath() != null) {
            return Launcher.toMinecraftDir(getCustomBasePath());
        } else if (getAppDir() == null) {
            path = Launcher.getOfficialDataDir();
        } else {
            path = Launcher.toMinecraftDir(getBaseDir());
        }
        
        path.mkdirs();
        return path;
    }

    /**
     * Get a list of JARs.
     * 
     * @return list of jars
     */
    public List<String> getJars() {
        List<String> jars = new ArrayList<String>();
        jars.add("minecraft.jar");
        File[] files = new File(getMinecraftDir(), "bin").listFiles();
        if (files == null) {
            return jars;
        }
        for (File f : files) {
            String name = f.getName();
            
            if (name.matches("[A-Za-z0-9\\-_ ]+\\.jar$") && !name.equalsIgnoreCase("jinput.jar")
                    && !name.equalsIgnoreCase("lwjgl.jar")
                    && !name.equalsIgnoreCase("lwjgl_util.jar")
                    && !name.equalsIgnoreCase("minecraft.jar")) {
                jars.add(name);
            }
        }
        return jars;
    }

    /**
     * Get the addons profile.
     * 
     * @param activeJar jar
     * @return addons profile
     */
    public AddonsProfile getAddonsProfile(String activeJar) {
        return new AddonsProfile(new File(getMinecraftDir(), "addons/" + activeJar));
    }
    
}
