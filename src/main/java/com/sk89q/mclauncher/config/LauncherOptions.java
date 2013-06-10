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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.util.LauncherUtils;
import com.sk89q.mclauncher.util.XmlUtils;

/**
 * Stores options for the launcher.
 */
@XmlRootElement(name = "launcher")
public class LauncherOptions {
    
    private static final Logger logger = Logger.getLogger(
            LauncherOptions.class.getCanonicalName());
    private static final SettingsList defaultSettings;
    
    private ConfigurationList configurations = new ConfigurationList();
    private ServerList servers = new ServerList();
    private IdentityList identities = new IdentityList();
    private SettingsList settings = new SettingsList(defaultSettings);

    private transient File file;
    
    static {
        SettingsList def;
        
        InputStream in = Launcher.class.getResourceAsStream("/resources/defaults.xml");
        if (in != null) {
            try {
                def = XmlUtils.parseJaxb(SettingsList.class, in);
            } catch (JAXBException e) {
                def = null;
                logger.log(Level.WARNING, "Could not read default settings", e);
            } finally {
                LauncherUtils.close(in);
            }
        } else {
            def = null;
        }
        
        defaultSettings = def;
    }
    
    public LauncherOptions() {
    }
    
    /**
     * Create an instance of the options with a file set.
     * 
     * @param file the file
     */
    public LauncherOptions(File file) {
        setFile(file);
    }
    
    @XmlElement
    public ConfigurationList getConfigurations() {
        return configurations;
    }

    public void setConfigurations(ConfigurationList configurations) {
        this.configurations = configurations;
    }

    @XmlElement
    public ServerList getServers() {
        return servers;
    }

    public void setServers(ServerList servers) {
        this.servers = servers;
    }

    @XmlElement
    public IdentityList getIdentities() {
        return identities;
    }

    public void setIdentities(IdentityList identities) {
        this.identities = identities;
    }

    @XmlElement
    public SettingsList getSettings() {
        return settings;
    }
    
    public void setSettings(SettingsList settings) {
        this.settings = settings;
        settings.setParents(defaultSettings);
    }

    @XmlTransient
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
    
    /**
     * Copy options from another instance.
     * 
     * @param newOptions the options
     */
    private void read(LauncherOptions newOptions) {
        setConfigurations(newOptions.configurations);
        setIdentities(newOptions.identities);
        setSettings(newOptions.settings);
    }

    /**
     * Load the configuration.
     * 
     * @return true if successful
     */
    public boolean load() {
        if (file == null) {
            throw new RuntimeException("No file was set on this instance");
        }
        
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            LauncherOptions newOptions = XmlUtils.parseJaxb(LauncherOptions.class, bis);
            read(newOptions);
            return true;
        } catch (FileNotFoundException e) {
            return true;
        } catch (JAXBException e) {
            logger.log(Level.WARNING, "Failed to load configuration", e);
            return false;
        } finally {
            LauncherUtils.close(bis);
            LauncherUtils.close(fis);
        }
    }

    /**
     * Save the configuration.
     * 
     * @return true if successful
     */
    public boolean save() {
        if (file == null) {
            throw new RuntimeException("No file was set on this instance");
        }
        
        try {
            XmlUtils.writeJaxb(this, file, LauncherOptions.class);
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save configuration", e);
            return false;
        } catch (JAXBException e) {
            logger.log(Level.WARNING, "Failed to save configuration", e);
            return false;
        }
    }
    
}
