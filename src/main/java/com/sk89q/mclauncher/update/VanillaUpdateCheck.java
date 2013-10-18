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

package com.sk89q.mclauncher.update;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.model.PackageManifest;
import com.sk89q.mclauncher.session.LegacySession;
import com.sk89q.mclauncher.session.MinecraftSession;
import com.sk89q.mclauncher.util.LauncherUtils;

/**
 * Vanilla update mechanism.
 * 
 * <p>This uses the embedded update.xml file to obtain the update.</p>
 */
public class VanillaUpdateCheck implements UpdateCheck {
    
    private static final URL BASE_URL = LauncherUtils.createURL("http://minecraft.net");
    private static final Logger logger = Logger.getLogger(
            VanillaUpdateCheck.class.getCanonicalName());
    
    private final Configuration configuration;
    private final UpdateCache cache;
    private final LegacySession session;
    
    /**
     * Create a new configuration.
     * 
     * @param configuration the configuration
     * @param session the session
     * @param cache the update cache
     */
    public VanillaUpdateCheck(Configuration configuration, 
            LegacySession session, UpdateCache cache) {
        this.configuration = configuration;
        this.session = session;
        this.cache = cache;
    }
    
    @Override
    public boolean needsUpdate() {
        // Try to import the last version from the official launcher
        if (cache.getLastUpdateId() == null) {
            try {
                importLauncherUpdateVersion(cache);
                cache.write();
            } catch (IOException e) {
                logger.log(Level.WARNING,
                        "Failed to import version information from official launcher", e);
            }
        }
        
        return !session.getLatestVersion().equals(cache.getLastUpdateId());
    }
    
    @Override
    public Updater createUpdater() throws UpdateException {
        InputStream is = null;
        PackageManifest manifest;
        
        try {
            // We have a base update.xml file stored in the .jar
            is = Launcher.class.getResourceAsStream(
                    "/resources/update.xml");
    
            if (is == null) {
                throw new UpdateException(
                        "Failed to get embedded update.xml required to update vanilla " +
                        "Minecraft. (Re-download launcher?)");
            }
            
            manifest = PackageManifestUpdater.parsePackage(is);
        } finally {
            LauncherUtils.close(is);
        }
        
        return new PackageManifestUpdater(
                BASE_URL,
                manifest, 
                configuration.getMinecraftDir(), 
                cache, 
                session.getLatestVersion());
    }
    
    /**
     * Import old launcher game version information.
     * 
     * @param cache update cache to update
     * @throws IOException on I/O error
     */
    public void importLauncherUpdateVersion(UpdateCache cache) throws IOException {
        File file = new File(Launcher.getOfficialDataDir(), "bin/version");
        if (!file.exists()) return;
        
        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(file));
            String version = in.readUTF();
            cache.setLastUpdateId(version);
        } finally {
            LauncherUtils.close(in);
        }
    }

    /**
     * Create an update check from a session.
     * 
     * @param configuration the configuration
     * @param session the session
     * @param cache the update cache
     * @return an update check
     * @throws UpdateException thrown if used with an offline login
     */
    public static UpdateCheck fromSession(Configuration configuration,
            MinecraftSession session, UpdateCache cache) throws UpdateException {
        
        if (!(session instanceof LegacySession)) {
            throw new UpdateException("An update is not possible when in offline mode.");
        }
        
        return new VanillaUpdateCheck(configuration,  (LegacySession) session, cache);
    }

}
