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

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.spout.nbt.CompoundMap;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.ListTag;
import org.spout.nbt.Tag;
import org.spout.nbt.stream.NBTInputStream;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.addons.AddonsProfile;
import com.sk89q.mclauncher.util.Util;

/**
 * Represents a configuration for the game.
 */
public class Configuration implements Comparable<Configuration> {
    
    private static final Logger logger = 
            Logger.getLogger(Configuration.class.getCanonicalName());
    public static final Pattern ID_PATTERN = Pattern.compile(
            "^[A-Za-z0-9\\-_\\.]+{1,64}$");
    
    private String id;
    private String customBasePath;
    private String appDir;
    private String name;
    private URL updateUrl;
    private String lastJar;
    private SettingsList settings = new SettingsList();
    private Date lastLaunch;

    private transient JarList jarList;
    private transient boolean builtIn = false;
    private transient boolean checkedIcon;
    private transient BufferedImage cachedIcon;
    
    public Configuration() {
    }
    
    public static Configuration createInstance(String id, String name, URL updateUrl) {
        return createCustom(
                id, name, "%INSTANCEDIR%" + File.separator + id, updateUrl);
    }

    public static Configuration createCustom(
            String id, String name, String dir, URL updateUrl) {
        
        if (!isValidId(id)) {
            throw new IllegalArgumentException("Invalid configuration ID");
        }
        
        Configuration configuration = new Configuration();
        configuration.setId(id);
        configuration.setName(name);
        configuration.setCustomBasePath(dir);
        configuration.setUpdateUrl(updateUrl);
        return configuration;
    }
    
    @Deprecated
    public static Configuration createGlobal(
            String id, String name, String dir, URL updateUrl) {
        
        if (!isValidId(id)) {
            throw new IllegalArgumentException("Invalid configuration ID");
        }
        
        Configuration configuration = new Configuration();
        configuration.setId(id);
        configuration.setName(name);
        configuration.setAppDir(dir);
        configuration.setUpdateUrl(updateUrl);
        return configuration;
    }

    @XmlElement
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement
    public String getAppDir() {
        return appDir;
    }

    public void setAppDir(String appDir) {
        if (appDir != null && appDir.isEmpty()) {
            this.appDir = null;
            return;
        }
        if (appDir != null && !appDir.matches("^[A-Za-z0-9\\-_]+{1,32}$")) {
            throw new IllegalArgumentException("Invalid data directory name");
        }
        this.appDir = appDir;
    }

    @XmlElement(name = "basePath")
    public String getCustomBasePath() {
        return customBasePath;
    }

    public void setCustomBasePath(String customBasePath) {
        if (customBasePath != null && customBasePath.isEmpty()) {
            this.customBasePath = null;
            return;
        }
        this.customBasePath = customBasePath;
    }

    @XmlElement(name = "updateURL")
    public URL getUpdateUrl() {
        return updateUrl;
    }

    public void setUpdateUrl(URL updateUrl) {
        this.updateUrl = updateUrl;
    }
    
    @XmlElement(name = "lastJar")
    public String getLastJarName() {
        return lastJar;
    }

    public void setLastJarName(String lastActiveJar) {
        this.lastJar = lastActiveJar;
    }

    public void setLastJar(MinecraftJar jar) {
        setLastJarName(jar instanceof DefaultJar ? null : jar.getName());
    }

    @XmlElement
    public SettingsList getSettings() {
        return settings;
    }

    public void setSettings(SettingsList settings) {
        this.settings = settings;
    }

    @XmlElement(name = "lastLaunch")
    public Date getLastLaunch() {
        return lastLaunch;
    }

    public void setLastLaunch(Date lastLaunch) {
        this.lastLaunch = lastLaunch;
    }

    public void updateLastLaunch() {
        setLastLaunch(new Date());
    }

    @XmlTransient
    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    @XmlTransient
    public boolean isUsingDefaultPath() {
        return customBasePath == null && appDir == null;
    }

    /**
     * Shortcut method to get the base working directory.
     * 
     * @return directory
     */
    public File getBaseDir() {
        File path;
        if (getCustomBasePath() != null) {
            path = Launcher.replacePathTokens(getCustomBasePath());
        } else if (getAppDir() == null) {
            path = Launcher.getAppDataDir();
        } else {
            path = Launcher.getAppDataDir(getAppDir());
        }
        
        path.mkdirs();
        return path;
    }

    /**
     * Shortcut method to get the directory show in an options dialog.
     * 
     * @return directory
     */
    public String getDirForOptions() {
        if (getCustomBasePath() != null) {
            return getCustomBasePath();
        } else if (getAppDir() == null) {
            return Launcher.getAppDataDir().getAbsolutePath();
        } else {
            return Launcher.getAppDataDir(getAppDir()).getAbsolutePath();
        }
    }

    /**
     * Shortcut method to get the directory that Minecraft will actually use.
     * 
     * @return directory
     */
    public File getMinecraftDir() {
        File path;
        if (getCustomBasePath() != null) {
            path = Launcher.toMinecraftDir(Launcher.replacePathTokens(getCustomBasePath()));
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
    @XmlTransient
    public JarList getJars() {
        if (jarList == null) {
            jarList = new JarList(new File(getMinecraftDir(), "bin"));
            jarList.setSelectedItem(getLastJarName());
        }
        return jarList;
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

    /**
     * Get the icon for the profile. May return null.
     * 
     * @return icon or null
     */
    public BufferedImage getIcon() {
        if (cachedIcon == null && !checkedIcon) {
            checkedIcon = true;
            loadIcon();
        }
        return cachedIcon;
    }
    
    /**
     * Try to load an icon from the configuration directory.
     * 
     * @param path path
     * @return this object
     */
    private Configuration loadIcon() {
        File iconPath = new File(getMinecraftDir(), "skmclauncher_icon.png");
        if (!iconPath.exists()) {
            return null;
        }
        
        FileInputStream in = null;
        try {
            in = new FileInputStream(iconPath);
            BufferedInputStream bis = new BufferedInputStream(in);
            BufferedImage icon = ImageIO.read(bis);
            
            // Make sure that the dimensions are acceptable
            // @TODO: Resize / support higher DPI images
            if (icon.getWidth() == 32 && icon.getHeight() == 32) {
                cachedIcon = icon;
            }
        } catch (IOException e) {
            logger.warning("Failed to load icon at " + iconPath);
        } finally {
            Util.close(in);
        }
        
        return this;
    }
    
    /**
     * Try to load an icon from the JAR.
     * 
     * @param path path
     * @return this object
     */
    public Configuration loadIcon(String path) {
        InputStream in = Launcher.class.getResourceAsStream(path);
        
        if (in != null) {
            try {
                cachedIcon = ImageIO.read(in);
            } catch (IOException e) {
                logger.warning("Failed to load icon at " + path);
            }
        }
        
        return this;
    }

    public List<ServerEntry> detectUserServers() {
        File file = new File(getMinecraftDir(), "servers.dat");
        List<ServerEntry> retn = new ArrayList<ServerEntry>();
        
        try {

            if (file.exists()) {
                NBTInputStream nbt = null;
                try {
                    nbt = new NBTInputStream(new FileInputStream(file), false);
                    Tag tag = nbt.readTag();
                    ListTag<?> servers = (ListTag<?>) 
                            ((CompoundMap) tag.getValue()).get("servers");


                    for (Object val : servers.getValue()) {
                        CompoundMap server = ((CompoundTag) val).getValue();
                        String name = (String) server.get("name").getValue();
                        String ip = (String) server.get("ip").getValue();

                        retn.add(new ServerEntry(name, ip));
                    }
                } finally {
                    Util.close(nbt);
                }
            }
        } catch (Throwable t) {
        }
        
        return retn;
    }
    
    public static boolean isValidId(String id) {
        return ID_PATTERN.matcher(id).matches();
    }

    @Override
    public int compareTo(Configuration o) {
        if (getLastLaunch() == null && o.getLastLaunch() == null) {
            return 0;
        } else if (getLastLaunch() == null) {
            return 1;
        } else if (o.getLastLaunch() == null) {
            return -1;
        } else {
            return -getLastLaunch().compareTo(o.getLastLaunch());
        }
    }
    
}
