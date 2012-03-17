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

package com.sk89q.mclauncher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import com.sk89q.mclauncher.addons.AddonsProfile;

public class AddonsProfileLoaderTask extends Task {
    
    private AddonsProfile addonsProfile;
    private AddonManagerDialog dialog;
    
    public AddonsProfileLoaderTask(AddonsProfile addonsProfile, AddonManagerDialog dialog) {
        this.addonsProfile = addonsProfile;
        this.dialog = dialog;
    }

    @Override
    protected void execute() throws ExecutionException {
        fireTitleChange("Reading addons profile...");
        fireStatusChange("Reading addons profile...");
        fireValueChange(-1);
        
        try {
            addonsProfile.read();
        } catch (IOException e) {
            throw new ExecutionException("Failed to read the addons profile from disk.", e);
        }
        
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    dialog.setAddonsProfile(addonsProfile);
                }
            });
        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
        }
    }

    @Override
    public Boolean cancel() {
        return null;
    }

}
