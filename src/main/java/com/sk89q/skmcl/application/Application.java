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

package com.sk89q.skmcl.application;

import com.sk89q.skmcl.minecraft.Minecraft;
import com.sk89q.skmcl.profile.Profile;
import com.sk89q.skmcl.util.Environment;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import java.io.IOException;
import java.util.List;

/**
 * Represents an application (or game) that can be installed and run.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Minecraft.class, name = "minecraft")
})
public interface Application {

    /**
     * Set the profile that is hosting this application.
     *
     * @param profile the profile
     */
    @JsonIgnore
    void setProfile(Profile profile);

    /**
     * Get the currently selected version.
     *
     * @return the currently selected version
     */
    Version getVersion();

    /**
     * Set the currently selected version.
     *
     * <p>This call should not block. If changes have to be made to the installation,
     * then those changes must be deferred until an update or launch is
     * initiated.</p>
     *
     * @param version the currently selected version
     */
    void setVersion(Version version);

    /**
     * Get the latest stable version.
     *
     * @return the latest stable version
     * @throws IOException thrown on I/O error
     */
    Version getLatestStable() throws IOException;

    /**
     * Get the latest snapshot version.
     *
     * @return the latest snapshot version, or null if none are available
     * @throws IOException thrown on I/O error
     */
    Version getLatestSnapshot() throws IOException;

    /**
     * Get a list of versions that are installed.
     *
     * @return a list of versions installed
     * @throws IOException thrown on I/O error
     */
    @JsonIgnore
    List<? extends Version> getInstalled() throws IOException;

    /**
     * Get a list of versions that can be installed.
     *
     * @return a list of versions that can be installed
     * @throws IOException thrown on I/O error
     */
    @JsonIgnore
    List<? extends Version> getAvailable() throws IOException;

    /**
     * Forget any cached version information.
     */
    void forgetVersions();

    /**
     * Get an instance representing the current installation for the current
     * version was returned by
     *
     * @param environment the environment
     * @param <T> the version type
     * @return an instance
     */
    <T extends Version> Instance getInstance(Environment environment);

}
