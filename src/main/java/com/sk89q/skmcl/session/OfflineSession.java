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
import lombok.ToString;

/**
 * An offline session.
 */
@ToString
public class OfflineSession implements Session {

    @Getter @Setter @NonNull
    private String username = "Player";

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getSessionId() {
        return "offline";
    }

    @Override
    public String getAccessToken() {
        return null;
    }

    @Override
    public Boolean call() throws Exception {
        return true;
    }

}
