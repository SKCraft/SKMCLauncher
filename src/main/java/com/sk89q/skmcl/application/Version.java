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

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * Represents a version.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "class",
        defaultImpl = Version.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LatestStable.class, name = "stable"),
        @JsonSubTypes.Type(value = LatestSnapshot.class, name = "snapshot"),
        @JsonSubTypes.Type(value = Version.class, name = "version")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Version {

    @Getter @Setter @NonNull
    private String id;

    public Version() {
    }

    public Version(@NonNull String id) {
        this.id = id;
    }

    /**
     * Get a friendly but identifiable name for the version (if possible).
     *
     * @return a name
     */
    @JsonIgnore
    public String getName() {
        return id;
    }

    @Override
    public String toString() {
        return getName();
    }

    boolean thisEquals(Version other) {
        return getId().equals(other.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version version = (Version) o;
        return thisEquals(version) && version.thisEquals(this);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
