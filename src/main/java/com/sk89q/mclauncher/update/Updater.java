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

import java.awt.Window;

import com.sk89q.mclauncher.event.ProgressListener;

/**
 * Updates a configuration.
 */
public interface Updater {
    
    /**
     * Represents the update type.
     */
    public static enum UpdateType {
        /**
         * Implies that the update should only download changed files.
         */
        INCREMENTAL,

        /**
         * Implies that the update should update everything (again).
         */
        FULL
    }
    
    /**
     * Get the window to own dialogs.
     * 
     * @return the window
     */
    Window getOwner();
    
    /**
     * Get the window to own dialogs.
     * 
     * @param owner the window
     */
    void setOwner(Window owner);

    /**
     * Add a progress listener.
     * 
     * @param listener the listener
     */
    void addProgressListener(ProgressListener listener);

    /**
     * Remove a progress listener.
     * 
     * @param listener the listener
     */
    void removeProgressListener(ProgressListener listener);
    
    /**
     * Perform the update.
     * 
     * @param type the type of update
     * @throws UpdateException on update exception
     * @throws InterruptedException on interruption
     */
    void update(UpdateType type) throws UpdateException, InterruptedException;

}
