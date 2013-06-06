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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import com.sk89q.mclauncher.model.UpdateManifest;
import com.sk89q.mclauncher.util.XMLUtil;

/**
 * Downloads an update manifest.
 */
public class UpdateManifestFetcher {
    
    private static final Logger logger = 
            Logger.getLogger(UpdateManifestFetcher.class.getCanonicalName());

    private URL updateUrl;
    private UpdateManifest manifest;
    
    /**
     * Construct the check.
     * 
     * @param updateUrl URL to check for updates
     */
    public UpdateManifestFetcher(URL updateUrl) {
        this.updateUrl = updateUrl;
    }
    
    /**
     * Attempt to check the update server.
     * 
     * @throws IOException on I/O error
     */
    public void downloadManifest() throws IOException {
        HttpURLConnection conn = null;
                
        try {
            conn = (HttpURLConnection) updateUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(false);
            conn.setReadTimeout(5000);

            conn.connect();
            
            if (conn.getResponseCode() != 200) {
                throw new IOException("Did not get expected 200 code");
            }
            
            manifest = XMLUtil.parseJaxb(UpdateManifest.class, 
                    new BufferedInputStream(conn.getInputStream()));
        } catch (JAXBException e) {
            logger.log(Level.WARNING, "Failed to read update manifest", e);
            throw new IOException("Failed to parse update manifest", e);
        } finally {
            if (conn != null) conn.disconnect();
            conn = null;
        }
    }

    /**
     * Get the update check URL.
     * 
     * @return url
     */
    public URL getUpdateUrl() {
        return updateUrl;
    }

    /**
     * Get the manifest.
     * 
     * @return the manifest
     */
    public UpdateManifest getManifest() {
        return manifest;
    }
    
}
