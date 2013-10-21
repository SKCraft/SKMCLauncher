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
import com.sk89q.skmcl.profile.Profile;
import com.sk89q.skmcl.application.Version;
import com.sk89q.skmcl.util.ProgressEvent;

import javax.swing.*;

/**
 * An installed version of Minecraft.
 */
public class MinecraftInstall implements Instance {

    private final Profile profile;
    private final Release version;

    /**
     * Create a new instance.
     *
     * @param profile the hosting profile
     * @param version the version
     */
    public MinecraftInstall(Profile profile, Release version) {
        this.profile = profile;
        this.version = version;
    }

    /**
     * Get the profile.
     *
     * @return the profile
     */
    public Profile getProfile() {
        return profile;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public SwingWorker<Instance, ProgressEvent> getUpdater() {
        return new MinecraftUpdater(this);
    }

    @Override
    public SwingWorker<Instance, ProgressEvent> getLauncher() {
        return null;
    }

    @Override
    public String toString() {
        return "MinecraftInstall{" +
                "version=" + version +
                '}';
    }
}
