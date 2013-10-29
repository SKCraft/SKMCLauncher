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

package com.sk89q.mclauncher.util;

import com.sk89q.skmcl.swing.SwingHelper;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Utility method to make {@link ActionListeners}.
 */
public final class ActionListeners {

    private ActionListeners() {
    }
    
    /**
     * Dispose of a window upon action.
     * 
     * @param window the window
     * @return the listener
     */
    public static ActionListener dispose(final Window window) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.dispose();
            }
        };
    }
    
    /**
     * Open a URL upon action.
     * 
     * @param component the component
     * @param url the URL
     * @return the listener
     */
    public static ActionListener openURL(final Component component, final String url) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingHelper.openURL(url, component);
            }
        };
    }
    
    /**
     * Open a directory browser upon action.
     * 
     * @param component the component
     * @param dir the path
     * @param create true to create the folders if needed
     * @return the listener
     */
    public static ActionListener browseDir(
            final Component component, final File dir, final boolean create) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (create) {
                    dir.mkdirs();
                }
                SwingHelper.browseDir(dir, component);
            }
        };
    }

}
