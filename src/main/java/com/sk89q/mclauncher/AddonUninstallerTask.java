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
import java.util.List;

import com.sk89q.mclauncher.addons.Addon;
import com.sk89q.mclauncher.addons.AddonsProfile;

public class AddonUninstallerTask extends Task {
    
    private AddonsProfile addonsProfile;
    private List<Addon> addons;
    private boolean running = true;
    
    public AddonUninstallerTask(AddonsProfile addonsProfile, List<Addon> addons) {
        this.addonsProfile = addonsProfile;
        this.addons = addons;
    }

    @Override
    protected void execute() throws ExecutionException {
        fireTitleChange("Uninstalling addons...");
        
        int i = 0;
        for (Addon addon : addons) {
            fireStatusChange("Uninstalling " + addon.getName() + "...");
            fireValueChange(i / (double) addons.size());
            if (!running) {
                throw new CancelledExecutionException();
            }
            
            addonsProfile.remove(addon);
            
            i++;
        }
        
        try {
            addonsProfile.write();
        } catch (IOException e) {
            throw new ExecutionException("Failed to write the addon list to disk: " +
                    e.getMessage(), e);
        }
    }

    @Override
    public Boolean cancel() {
        running = false;
        return null;
    }

}
