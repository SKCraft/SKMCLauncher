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

import com.sk89q.skmcl.util.HttpRequest;
import com.sk89q.skmcl.application.Application;
import com.sk89q.skmcl.application.Instance;
import com.sk89q.skmcl.profile.Profile;
import com.sk89q.skmcl.application.Version;
import com.sk89q.skmcl.minecraft.model.ReleaseList;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.sk89q.skmcl.util.HttpRequest.url;

/**
 * Installs, upates, and launches Minecraft.
 */
public class Minecraft implements Application {

    public static final String VERSIONS_LIST_URL =
            "http://s3.amazonaws.com/Minecraft.Download/versions/versions.json";

    private Version selected;
    private transient Profile profile;

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
    public List<Release> getInstalled() {
        List<Release> versions = new ArrayList<Release>();
        File dir = new File(profile.getContentDir(), "versions");
        if (dir.exists()) {
            for (File d : dir.listFiles(new VersionFoldersFilter())) {
                versions.add(new Release(d.getName()));
            }
        }
        return versions;
    }

    @Override
    public List<Release> getAvailable() throws IOException {
        return HttpRequest
                .get(url(VERSIONS_LIST_URL))
                .execute()
                .returnContent()
                .asJson(ReleaseList.class)
                .getVersions();
    }

    @Override
    public Instance getInstance(Version version) {
        if (version instanceof Release) {
            return new MinecraftInstall(getProfile(), (Release) version);
        } else {
            throw new IllegalArgumentException("Must be a Release");
        }
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
