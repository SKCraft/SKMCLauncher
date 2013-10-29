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

package com.sk89q.skmcl.install;

import com.sk89q.skmcl.util.LauncherUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.logging.Logger;

public class VersionCache {

    private static final Logger logger = LauncherUtils.getLogger(VersionCache.class);

    @Getter @Setter
    private Map<String, String> entries = new HashMap<String, String>();
    private transient Set<String> touched = new HashSet<String>();

    public void put(String key, String value) {
        getEntries().put(key, value);
        touched.add(key);
    }

    public String get(String key) {
        return getEntries().get(key);
    }

    public void touch(String key) {
        touched.add(key);
    }

    public void removeOldEntries() {
        Iterator<Map.Entry<String, String>> it = entries.entrySet().iterator();

        while (it.hasNext()) {
            if (!touched.contains(it.next().getKey())) {
                it.remove();
            }
        }
    }

}
