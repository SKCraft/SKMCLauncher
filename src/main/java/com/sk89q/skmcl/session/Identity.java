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

package com.sk89q.skmcl.session;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Date;

public class Identity implements Comparable<Identity> {

    @Getter @Setter
    private String id;
    @Getter @Setter
    private String accessToken;
    @Getter @Setter
    private Date lastUsed;
    @Getter @Setter
    private String cachedUsername;

    public Identity() {
    }

    public Identity(String id) {
        setId(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Identity identity = (Identity) o;

        if (!id.equalsIgnoreCase(identity.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.toLowerCase().hashCode();
    }

    @Override
    public int compareTo(@NonNull Identity o) {
        Date otherDate = o.getLastUsed();

        if (otherDate == null && lastUsed == null) {
            return 0;
        } else if (otherDate == null) {
            return -1;
        } else if (lastUsed == null) {
            return 1;
        } else {
            return -lastUsed.compareTo(otherDate);
        }
    }

    @Override
    public String toString() {
        return getId();
    }
}
