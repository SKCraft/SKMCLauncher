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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.model.PackageManifest;
import com.sk89q.mclauncher.model.UpdateManifest;
import com.sk89q.mclauncher.util.LauncherUtils;

/**
 * Checks for an update using an {@link UpdateManifest}.
 */
public class ManifestUpdateCheck implements UpdateCheck {
    
    private final Configuration configuration;
    private final UpdateCache cache;
    private final UpdateManifestFetcher fetcher;
    
    public ManifestUpdateCheck(
            Configuration configuration, UpdateCache cache, UpdateManifestFetcher fetcher) {
        this.configuration = configuration;
        this.cache = cache;
        this.fetcher = fetcher;
    }
    
    @Override
    public boolean needsUpdate() throws InterruptedException, UpdateException {
        try {
            fetcher.downloadManifest();
        } catch (UnknownHostException e) {
            throw new UpdateException("Host is unresolved: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new UpdateException(e.getMessage(), e);
        }
        
        return cache.getLastUpdateId() == null ||
                !cache.getLastUpdateId().equals(fetcher.getManifest().getLatestVersion());
    }

    @Override
    public Updater createUpdater() throws UpdateException {
        try {
            fetcher.downloadManifest();
        } catch (IOException e) {
            throw new UpdateException("Could not fetch the update manifest (" +
                    e.getMessage() + "). The update cannot be performed.");
        }
        
        URL packageUrl = null;
        UpdateManifest updateManifest = fetcher.getManifest();
        
        // Get the URL to the package
        try {
            packageUrl = updateManifest.toPackageURL(fetcher.getUpdateURL());
        } catch (MalformedURLException e) {
            throw new UpdateException("Invalid URL: " + updateManifest.getPackageURL());
        }
        
        // Get the package manifest
        HttpURLConnection conn = null;
        InputStream is = null;
        PackageManifest packageManifest;
        
        try {
            conn = (HttpURLConnection) packageUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(false);
            conn.setReadTimeout(5000);

            conn.connect();
            
            if (conn.getResponseCode() != 200) {
                throw new IOException("Did not get expected 200 code");
            }
            
            is = conn.getInputStream();
            packageManifest = PackageManifestUpdater.parsePackage(is);
        } catch (IOException e) {
            throw new UpdateException("Could not fetch the package manifest (" +
                    e.getMessage() + "). The update cannot be performed.");
        } finally {
            LauncherUtils.close(is);
            if (conn != null) conn.disconnect();
            conn = null;
        }
        
        return new PackageManifestUpdater(
                packageUrl,
                packageManifest, 
                configuration.getMinecraftDir(), 
                cache, 
                updateManifest.getLatestVersion());
    }
    
}