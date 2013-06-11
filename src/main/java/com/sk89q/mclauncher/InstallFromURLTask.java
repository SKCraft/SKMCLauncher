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

package com.sk89q.mclauncher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.config.LauncherOptions;
import com.sk89q.mclauncher.model.UpdateManifest;
import com.sk89q.mclauncher.update.UpdateManifestFetcher;
import com.sk89q.mclauncher.util.LauncherUtils;
import com.sk89q.mclauncher.util.Task;

public class InstallFromURLTask extends Task {
    
    private final InstallFromURLDialog dialog;
    private final LauncherOptions options;
    private final String url;
    
    public InstallFromURLTask(InstallFromURLDialog dialog,
            LauncherOptions options, String url) {
        this.dialog = dialog;
        this.options = options;
        this.url = url;
    }

    @Override
    protected void execute() throws ExecutionException {
        fireStatusChange("Downloading update manifest...");
        
        try {
            URL urlObject = new URL(url);
            
            UpdateManifestFetcher fetcher = new UpdateManifestFetcher(urlObject);
            fetcher.downloadManifest();
            final UpdateManifest manifest = fetcher.getManifest();
            if (!manifest.isValidForInstall()) {
                throw new ExecutionException(
                        "The update manifest at the given URL has an invalid ID/name.");
            }
            
            String id = urlObject.getHost() + "_" + manifest.getId(); // Ignoring port

            Configuration existing = options.getConfigurations().get(id);
            if (existing != null) {
                throw new ExecutionException(
                        "It looks like this pack is already installed as '"
                                + existing.getName() + "'.");
            }

            LauncherUtils.checkInterrupted();
            
            Configuration configuration = 
                    Configuration.createInstance(
                            id, manifest.getName(), urlObject);
            configuration.setLastLaunch(new Date());
            
            // Update the configuration's news URL
            try {
                configuration.setNewsUrl(manifest.toNewsURL(urlObject));
                Launcher.getInstance().getOptions().save();
            } catch (MalformedURLException e) {
                throw new ExecutionException(
                        "The manfiest at the URL has an invalid news URL.");
            }
            
            options.getConfigurations().register(configuration);
            options.getConfigurations().sortByDate();

            if (!options.save()) {
                throw new ExecutionException(
                        "Your options could not be saved to disk.");
            }

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(dialog, "The configuration '"
                            + manifest.getName() + "' has been installed!",
                            "Installed", JOptionPane.INFORMATION_MESSAGE);
                }
            });

            dialog.dispose();
        } catch (IOException e) {
            throw new ExecutionException(
                    "Failed to install from the given URL: " + e.getMessage());
        } catch (InvocationTargetException e) {
            throw new ExecutionException("Unknown error", e);
        } catch (InterruptedException e) {
        }
    }

}
