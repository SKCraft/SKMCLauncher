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

package com.sk89q.skmcl.profile;

import com.sk89q.skmcl.application.Application;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import java.io.File;
import java.util.Date;

/**
 * Represents a profile that contains an installed game.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleProfile.class, name = "simple")
})
public interface Profile extends Comparable<Profile> {

    /**
     * Get the name of the profile.
     *
     * @return the name of the profile
     */
    String getName();

    /**
     * Set the name of the profile.
     *
     * @param name the name of the profile
     */
    void setName(String name);

    /**
     * Get the application installed into this profile.
     *
     * @return the application
     */
    Application getApplication();

    /**
     * Set the application installed into this profile.
     *
     * @param application the application
     */
    void setApplication(Application application);

    /**
     * Get the last date that the profile was launched.
     *
     * @return the last launch date
     */
    Date getLastLaunchDate();

    /**
     * Set the last date that the profile was launched.
     *
     * @param date the date that the profile was last launched
     */
    void setLastLaunchDate(Date date);

    /**
     * Get the base directory for this profile.
     *
     * @return the base directory
     */
    @JsonIgnore
    File getBaseDir();

    /**
     * Set the base directory for this profile.
     *
     * @param baseDir the base directory
     */
    void setBaseDir(File baseDir);

    /**
     * Get the directory that is shared between profiles.
     *
     * <p>This directory can be used to store shared assets.</p>
     *
     * @return the shared directory
     */
    @JsonIgnore
    File getSharedDir();

    /**
     * Set the directory that is shared between profiles.
     *
     * <p>This directory can be used to store shared assets.</p>
     *
     * @param sharedDir the shared directory
     */
    void setSharedDir(File sharedDir);

    /**
     * Get the directory where application files should be stored.
     *
     * @return the content directory
     */
    @JsonIgnore
    File getContentDir();

    /**
     * Get the directory where temporary files should be stored.
     *
     * @return the temporary directory
     */
    @JsonIgnore
    File getTemporaryDir();

}
