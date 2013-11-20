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

import com.sk89q.skmcl.application.Application;
import com.sk89q.skmcl.application.OnlineRequiredException;
import com.sk89q.skmcl.application.ResolutionException;
import com.sk89q.skmcl.application.Version;
import com.sk89q.skmcl.minecraft.model.ReleaseList;
import com.sk89q.skmcl.profile.Profile;
import com.sk89q.skmcl.util.Environment;
import com.sk89q.skmcl.util.HttpRequest;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.sk89q.skmcl.util.HttpRequest.url;

/**
 * Installs, updates, and launches Minecraft.
 */
public class Minecraft implements Application {

    public static final String VERSIONS_LIST_URL =
            "https://s3.amazonaws.com/Minecraft.Download/versions/versions.json";

    private Version version;
    private transient Profile profile;
    private transient ReleaseList releaseList;

    /**
     * Get a copy of the profile in use.
     *
     * @return the profile
     */
    protected Profile getProfile() {
        return profile;
    }

    @Override
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public void setVersion(Version version) {
        this.version = version;
    }

    @Override
    public Version getLatestStable() throws IOException, InterruptedException {
        Version release = getReleaseList().find(
                getReleaseList().getLatest().getRelease());
        if (release != null) {
            return release;
        } else {
            throw new IOException("Failed to get latest stable release");
        }
    }

    @Override
    public Version getLatestSnapshot() throws IOException, InterruptedException {
        return getReleaseList().find(getReleaseList().getLatest().getSnapshot());
    }

    @Override
    public List<Version> getInstalled() {
        List<Version> versions = new ArrayList<Version>();
        File dir = new File(profile.getContentDir(), "versions");
        if (dir.exists()) {
            for (File d : dir.listFiles(new VersionFoldersFilter())) {
                versions.add(new Version(d.getName()));
            }
        }
        return versions;
    }

    @Override
    public List<Version> getAvailable() throws IOException, InterruptedException {
        return getReleaseList().getVersions();
    }

    @Override
    public boolean hasSnapshots() {
        return true;
    }

    @Override
    public void forgetVersions() {
        releaseList = null;
    }

    @Override
    public MinecraftInstall getInstance(Environment environment, boolean offline)
            throws ResolutionException, OnlineRequiredException {
        Version current = getVersion();
        if (current == null) {
            throw new NullPointerException("No version is set for this application");
        }
        return new MinecraftInstall(getProfile(),
                current.resolve(this, offline), environment);
    }

    /**
     * Get a copy of the release list, fetching it from the web if it's not cached.
     *
     * @return the release list
     * @throws IOException on I/O error
     */
    private ReleaseList getReleaseList() throws IOException, InterruptedException {
        if (releaseList == null) {
            ReleaseList list = HttpRequest
                    .get(url(VERSIONS_LIST_URL))
                    .execute()
                    .returnContent()
                    .asJson(ReleaseList.class);
            this.releaseList = list;
            return list;
        }

        return releaseList;
    }

    /**
     * Permits only directory {@link File}s to go through.
     */
    private static class VersionFoldersFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    }

}
