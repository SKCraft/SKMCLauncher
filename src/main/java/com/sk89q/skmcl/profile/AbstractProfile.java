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

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Date;

public abstract class AbstractProfile implements Profile, Comparable<Profile> {

    @Getter @Setter
    private Date lastLaunchDate;

    public int compareTo(@NonNull Profile o) {
        Date otherDate = o.getLastLaunchDate();

        if (otherDate == null && lastLaunchDate == null) {
            return 0;
        } else if (otherDate == null) {
            return -1;
        } else if (lastLaunchDate == null) {
            return 1;
        } else {
            return -lastLaunchDate.compareTo(otherDate);
        }
    }
}
