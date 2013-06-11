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

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.config.Identity;
import com.sk89q.mclauncher.config.IdentityList;
import com.sk89q.mclauncher.config.LauncherOptions;
import com.sk89q.mclauncher.config.MinecraftJar;
import com.sk89q.mclauncher.util.SwingHelper;
import com.sk89q.mclauncher.util.Task;
import com.sk89q.mclauncher.util.TaskWorker;

/**
 * Main launcher GUI frame.
 * 
 * @author sk89q
 */
public class LauncherFrame extends JFrame {

    private static final long serialVersionUID = 4122023031876609883L;

    private final LauncherOptions options;
    private final LaunchOptions launchOptions;

    private LauncherView view;
    private TaskWorker worker = new TaskWorker();

    /**
     * Construct the launcher.
     */
    public LauncherFrame() {
        setTitle("SK's Minecraft Launcher");
        setSize(620, 500);
        SwingHelper.setLookAndFeel();
        SwingHelper.setIconImage(this, "/resources/icon.png");

        options = Launcher.getInstance().getOptions();
        launchOptions = new LaunchOptions(this);

        view = new ClassicView(this, launchOptions);
        add(view, BorderLayout.CENTER);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                view.cleanUp();
                dispose();
            }
        });
    }
    
    /**
     * Get the selected configuration.
     * 
     * @return configuration
     */
    public Configuration getSelected() {
        return view.getSelected();
    }

    /**
     * Get the options affecting launch.
     * 
     * @return the launch options
     */
    public LaunchOptions getLaunchSettings() {
        return launchOptions;
    }
    
    /**
     * Launch the game.
     */
    public void launch() {
        if (worker.isAlive()) {
            return;
        }

        Configuration configuration = getSelected();
        IdentityList identities = options.getIdentities();
        
        if (!launchOptions.verifyAndNotify()) {
            return;
        }

        Identity identity = launchOptions.getIdentity();
        boolean remember = launchOptions.shouldRememberPassword();
        MinecraftJar jar = launchOptions.getActiveJar();

        // Save the identity
        if (!launchOptions.isPlayingOffline()) {
            if (remember) {
                identities.remember(identity.getId(), identity.getPassword());
            } else {
                identities.forgetPassword(identity.getId());
            }
        } else {
            identity = new Identity("Player", null);
        }
        
        configuration.updateLastLaunch();
        configuration.setLastJar(jar);
        
        options.getConfigurations().sortByDate();
        view.selectAfterSort();
        
        options.save();

        LaunchTask task = new LaunchTask(
                this, configuration, launchOptions, jar);
        worker = Task.startWorker(this, task);
    }
    
}