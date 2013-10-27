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

import java.util.concurrent.Callable;

/**
 * A session is a user login.
 *
 * <p>Sessions must be run (see {@link Callable}) before they can be used.</p>
 */
public interface Session extends Callable<Boolean> {

    /**
     * Returns whether the session can be used in its current state.
     *
     * <p>For authenticated sessions, this would return true if the login is successful.
     * For offline sessions, they may always return true.</p>
     *
     * @return true is valid
     */
    boolean isValid();

    /**
     * Get the username. If a login was successful, this will be the correct
     * form of the username.
     *
     * @return username
     */
    String getUsername();

    /**
     * Get a session ID.
     *
     * @return session ID, or null
     */
    String getSessionId();

    /**
     * Get an access token that will identify the user in place of the password.
     *
     *
     * @return an access token, or null
     */
    String getAccessToken();

}
