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

import com.sk89q.skmcl.application.Version;
import com.sk89q.skmcl.install.HttpResource;
import com.sk89q.skmcl.install.InstallerRuntime;
import com.sk89q.skmcl.minecraft.model.AWSBucket;
import com.sk89q.skmcl.minecraft.model.Library;
import com.sk89q.skmcl.minecraft.model.ReleaseManifest;
import com.sk89q.skmcl.util.Environment;
import com.sk89q.skmcl.util.HttpRequest;
import com.sk89q.skmcl.util.LauncherUtils;
import com.sk89q.skmcl.worker.Segment;
import com.sk89q.skmcl.worker.Task;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sk89q.skmcl.util.HttpRequest.Form.form;
import static com.sk89q.skmcl.util.HttpRequest.url;
import static com.sk89q.skmcl.util.LauncherUtils.checkInterrupted;
import static com.sk89q.skmcl.util.LauncherUtils.hasSystemProperty;
import static com.sk89q.skmcl.util.SharedLocale._;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Updates an installation of Minecraft.
 */
class MinecraftUpdater extends Task<MinecraftInstall> {

    private static final String VERSION_MANIFEST_URL =
            "http://s3.amazonaws.com/Minecraft.Download/versions/%s/%s.json";
    private static final String ASSETS_URL =
            "https://s3.amazonaws.com/Minecraft.Resources/";

    private static final Logger logger = LauncherUtils.getLogger(MinecraftUpdater.class);
    private final MinecraftInstall instance;
    private final Environment environment;
    private final InstallerRuntime installer;

    /**
     * Create a new instance.
     *
     * @param instance the installation instance
     */
    public MinecraftUpdater(MinecraftInstall instance) {
        this.instance = instance;
        this.environment = instance.getEnvironment();

        File temporaryDir = instance.getProfile().getTemporaryDir();

        installer = new InstallerRuntime(environment);
        installer.setTemporaryDir(temporaryDir);
    }

    /**
     * Get the URL of the JSON file that tells information about the desired version.
     *
     * @return the URL
     */
    private URL getManifestUrl() {
        Version version = instance.getVersion();

        return url(String.format(
                VERSION_MANIFEST_URL,
                version.getId(), version.getId()));
    }

    /**
     * Get the URL where assets can be found.
     *
     * @param marker the bucket marker indicating the entry to start at
     * @return the URL
     */
    private URL getAssetsUrl(String marker) {
        if (marker.length() == 0) {
            return getAssetsUrl();
        } else {
            return url(ASSETS_URL + "?" + form().add("marker", marker).toString());
        }
    }

    /**
     * Get the base URL where assets can be found.
     *
     * @return the URL
     */
    private URL getAssetsUrl() {
        return url(ASSETS_URL);
    }

    @Override
    public MinecraftInstall call() throws Exception {
        Segment step1 = segment(0.02),
                step2 = segment(0.02),
                step3 = segment(0.96);

        installGame(step1);
        checkInterrupted();
        if (!hasSystemProperty(MinecraftUpdater.class, "skipAssets")) {
            installAssets(step2);
            checkInterrupted();
        }

        logger.log(Level.INFO, "Install tasks enumerated; now installing...");

        installer.addObserver(step3);
        installer.call();

        return instance;
    }

    /**
     * Install the game.
     *
     * @param segment segment for progress tacking
     * @throws IOException thrown on I/O error
     * @throws InterruptedException thrown on interruption
     */
    protected void installGame(Segment segment) throws IOException, InterruptedException {
        logger.log(Level.INFO, "Checking for game updates...");
        segment.push(0, _("minecraftUpdate.checkingGameUpdates"));

        File contentDir = instance.getProfile().getContentDir();
        File jarPath = instance.getJarPath();
        File manifestPath = instance.getManifestPath();

        // Obtain the release manifest, save it, and parse it
        ReleaseManifest manifest = HttpRequest
                .get(getManifestUrl())
                .execute()
                .expectResponseCode(200)
                .returnContent()
                .saveContent(manifestPath)
                .asJson(ReleaseManifest.class);

        // If the JAR does not exist, install it
        if (!jarPath.exists()) {
            installer.copyTo(new HttpResource(manifest.getJarUrl()), jarPath);
        }

        // Install all the missing libraries
        for (Library library : manifest.getLibraries()) {
            if (library.matches(environment)) {
                URL url = library.getUrl(environment);
                File file = new File(contentDir, "libraries/" + library.getPath(environment));

                if (!file.exists()) {
                    installer.copyTo(new HttpResource(url), file);
                }

                checkInterrupted();
            }
        }
    }

    /**
     * Add shared Minecraft assets to the installer.
     *
     * @param segment segment for progress tacking
     * @throws IOException on I/O error
     * @throws InterruptedException on interruption
     */
    protected void installAssets(Segment segment) throws IOException, InterruptedException {
        logger.log(Level.INFO, "Checking for asset downloads...");

        File assetsDir = instance.getAssetsDir();
        String marker = "";

        while (marker != null) {
            URL bucketUrl = getAssetsUrl(marker);
            segment.push(0, _("minecraftUpdate.checkingAssets", bucketUrl.toString()));
            logger.log(Level.INFO, "Enumerating assets from {0}...", bucketUrl);
            checkInterrupted();

            // Obtain the assets manifest
            AWSBucket bucket = HttpRequest
                    .get(bucketUrl)
                    .execute()
                    .returnContent()
                    .asXml(AWSBucket.class);

            // Install all the missing assets
            for (AWSBucket.Item item : bucket.getContents()) {
                String key = item.getKey();
                String hash = item.getEtag();

                if (item.isDirectory()) {
                    continue; // skip directories
                }

                URL url = item.getUrl(getAssetsUrl());
                File file = new File(assetsDir, key);

                if (!file.exists() || !getFileETag(file).equals(hash)) {
                    logger.log(Level.INFO, "Need to get {0}", key);
                    String id = hash + file.toString();
                    installer.copyTo(new HttpResource(url).withId(id), file);
                }

                marker = item.getKey();
            }

            // If the last bucket list is not truncated, then we're done
            if (!bucket.isTruncated()) {
                marker = null;
            }
        }
    }

    /**
     * Generate the Etag hash string that is returned by the assets location.
     *
     * @param file the file
     * @return the etag hash string
     */
    protected String getFileETag(File file) {
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            return "\"" + DigestUtils.md5Hex(is) + "\"";
        } catch (IOException e) {
            return "";
        } finally {
            closeQuietly(is);
        }
    }

}
