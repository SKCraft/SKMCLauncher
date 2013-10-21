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

package com.sk89q.skmcl.minecraft.model;

import com.sk89q.skmcl.minecraft.Release;

import java.util.List;

public class ReleaseList {

    private LatestReleases latest;
    private List<Release> versions;

    public LatestReleases getLatest() {
        return latest;
    }

    public void setLatest(LatestReleases latest) {
        this.latest = latest;
    }

    public List<Release> getVersions() {
        return versions;
    }

    public void setVersions(List<Release> versions) {
        this.versions = versions;
    }

    public static class LatestReleases {

        private String snapshot;
        private String release;

        public String getSnapshot() {
            return snapshot;
        }

        public void setSnapshot(String snapshot) {
            this.snapshot = snapshot;
        }

        public String getRelease() {
            return release;
        }

        public void setRelease(String release) {
            this.release = release;
        }
    }

}
