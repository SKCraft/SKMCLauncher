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

package com.sk89q.mclauncher.session;

import java.io.IOException;

/**
 * Represents an offline session.
 */
public class OfflineSession implements MinecraftSession {
    
    private final String username;

    public OfflineSession(String username) {
        this.username = username;
    }

    public OfflineSession() {
        username = "Player";
    }

    @Override
    public void login(String password) throws IOException,
            OutdatedLauncherException, LoginException {
        // Nothing to do!
    }

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
        return null;
    }

    @Override
    public String getAccessToken() {
        return null;
    }

}
