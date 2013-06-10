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

package com.sk89q.mclauncher.update;

/**
 * The result of an update check.
 */
public interface UpdateCheck {
    
    /**
     * Return whether an update is required.
     * 
     * <p>This method may block to perform the check.</p>
     * 
     * @return true if an update is required
     * @throws InterruptedException thrown on interruption
     * @throws UpdateException thrown if an error occurs while checking
     */
    boolean needsUpdate() throws InterruptedException, UpdateException;
    
    /**
     * Create an updater.
     * 
     * @return the updater
     * @throws UpdateException thrown if an updater can't be made
     */
    Updater createUpdater() throws UpdateException;

}
