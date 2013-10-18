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
 * Represents a Minecraft session.
 */
public interface MinecraftSession {

    /**
     * Attempt to perform a login.
     * 
     * @param password plain text password
     * @throws IOException throw on an IO error
     * @throws LoginException on a login exception
     * @throws OutdatedLauncherException thrown if the launcher is 'out of date'
     * @throws UserNotPremiumException thrown if the user is not premium
     */
    void login(String password) throws IOException,
            LoginException, OutdatedLauncherException, UserNotPremiumException;

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
     * Thrown on a login error.
     */
    public static class LoginException extends Exception {
        private static final long serialVersionUID = 3704469434921739106L;
        
        public LoginException(String message) {
            super(message);
        }
        
        public LoginException(String message, Throwable t) {
            super(message, t);
        }
    }

    /**
     * Thrown when the username/password is incorrect.
     */
    public static class InvalidCredentialsException extends LoginException {
        private static final long serialVersionUID = -3790329568439802662L;

        public InvalidCredentialsException() {
            super("Invalid username/email and password combination.");
        }
    }

    /**
     * Thrown when the user is not premium.
     */
    public static class UserNotPremiumException extends Exception {
        private static final long serialVersionUID = -7109390633647649010L;
    }

    /**
     * Thrown when the login server throws an "outdated launcher" exception.
     */
    public static class OutdatedLauncherException extends Exception {
        private static final long serialVersionUID = -7091094616970647279L;
    }

}