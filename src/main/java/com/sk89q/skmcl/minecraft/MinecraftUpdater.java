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
import com.sk89q.skmcl.application.Version;
import com.sk89q.skmcl.install.HttpResource;
import com.sk89q.skmcl.install.InstallerRuntime;
import com.sk89q.skmcl.minecraft.model.Library;
import com.sk89q.skmcl.minecraft.model.ReleaseManifest;
import com.sk89q.skmcl.util.Environment;
import com.sk89q.skmcl.util.HttpRequest;
import com.sk89q.skmcl.util.ProgressEvent;

import javax.swing.*;
import java.io.File;
import java.net.URL;

import static com.sk89q.skmcl.util.HttpRequest.url;

/**
 * Updates an installation of Minecraft.
 */
class MinecraftUpdater extends SwingWorker<Instance, ProgressEvent> {

    private final MinecraftInstall instance;

    /**
     * Create a new instance.
     *
     * @param instance the installation instance
     */
    public MinecraftUpdater(MinecraftInstall instance) {
        this.instance = instance;
    }

    /**
     * Get the URL of the JSON file containing information about the libraries for
     * the given Minecraft version.
     *
     * @return the URL
     */
    private URL getUpdateUrl() {
        Version version = instance.getVersion();

        return url(String.format(
                "http://s3.amazonaws.com/Minecraft.Download/versions/%s/%s.json",
                version.getId(), version.getId()));
    }

    @Override
    protected Instance doInBackground() throws Exception {
        Version version = instance.getVersion();
        Environment environment = Environment.getInstance();

        File temporaryDir = instance.getProfile().getTemporaryDir();
        File contentDir = instance.getProfile().getContentDir();

        // The files that go into versions/$version/$version
        String versionPath = String.format("versions/%1$s/%1$s", version.getId());
        File jarPath = new File(contentDir, versionPath + ".jar");
        File manifestPath = new File(contentDir, versionPath + ".json");

        InstallerRuntime installer = new InstallerRuntime(environment);
        installer.setTemporaryDir(temporaryDir);

        // Obtain the release manifest, save it, and parse it
        ReleaseManifest manifest = HttpRequest
                .get(getUpdateUrl())
                .execute()
                .expectResponseCode(200)
                .returnContent()
                .saveContent(manifestPath)
                .asJson(ReleaseManifest.class);

        // If the JAR does not exist, install it
        if (!jarPath.exists()) {
            installer.copyTo(new HttpResource(manifest.getJarUrl()), jarPath).skipLog();
        }

        // Install all the missing libraries
        for (Library library : manifest.getLibraries()) {
            if (library.matches(environment)) {
                URL url = library.getUrl(environment);
                File file = new File(contentDir, "libraries/" + library.getPath(environment));

                if (!file.exists()) {
                    installer.copyTo(new HttpResource(url), file).skipLog();
                }
            }
        }

        installer.run();
        installer.get();

        return instance;
    }
}
