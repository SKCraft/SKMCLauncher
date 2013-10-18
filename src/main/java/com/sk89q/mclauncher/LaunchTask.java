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

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.config.Identity;
import com.sk89q.mclauncher.config.LauncherOptions;
import com.sk89q.mclauncher.config.MinecraftJar;
import com.sk89q.mclauncher.config.SettingsList;
import com.sk89q.mclauncher.event.ProgressListener;
import com.sk89q.mclauncher.launch.LaunchProcessBuilder;
import com.sk89q.mclauncher.session.LegacySession;
import com.sk89q.mclauncher.session.MinecraftSession;
import com.sk89q.mclauncher.session.MinecraftSession.InvalidCredentialsException;
import com.sk89q.mclauncher.session.MinecraftSession.LoginException;
import com.sk89q.mclauncher.session.MinecraftSession.OutdatedLauncherException;
import com.sk89q.mclauncher.session.MinecraftSession.UserNotPremiumException;
import com.sk89q.mclauncher.session.OfflineSession;
import com.sk89q.mclauncher.update.UpdateCache;
import com.sk89q.mclauncher.update.UpdateCheck;
import com.sk89q.mclauncher.update.UpdateException;
import com.sk89q.mclauncher.update.Updater;
import com.sk89q.mclauncher.update.Updater.UpdateType;
import com.sk89q.mclauncher.util.LauncherUtils;
import com.sk89q.mclauncher.util.SwingHelper;
import com.sk89q.mclauncher.util.Task;

/**
 * Used for launching the game.
 */
public class LaunchTask extends Task {
    
    public enum UpdateRequirement {
        NEVER,
        CHECK_FOR_UPDATE,
        NEED_UPDATE
    }
    
    private final JFrame frame;
    private final MinecraftJar activeJar;
    private final Configuration configuration;
    private final LaunchOptions launchOptions;
    private final LauncherOptions options;
    private final Identity identity;
    private final File minecraftDir;

    private MinecraftSession session;
    
    /**
     * Construct the launch task.
     * 
     * @param frame starting frame
     * @param configuration workspace
     * @param launchOptions the launch options
     * @param jar the jar to launch
     */
    public LaunchTask(JFrame frame, Configuration configuration, 
            LaunchOptions launchOptions, MinecraftJar jar) {
        this.frame = frame;
        this.configuration = configuration;
        this.launchOptions = launchOptions;
        this.activeJar = jar;
        
        options = Launcher.getInstance().getOptions();
        identity = launchOptions.getIdentity();
        minecraftDir = configuration.getMinecraftDir();
    }
    
    @Override
    public void execute() throws ExecutionException, InterruptedException {
        createSession();
        LauncherUtils.checkInterrupted();
        try {
            checkForUpdates();
        } catch (UpdateException e) {
            throw new ExecutionException(e.getMessage(), e);
        }
        LauncherUtils.checkInterrupted();
        launch();
    }
    
    /**
     * Create a session and login as needed.
     * 
     * @throws ExecutionException on error while executing
     * @throws InterruptedException on interruption
     */
    private void createSession() throws ExecutionException, InterruptedException {
        if (launchOptions.isPlayingOffline()) {
            session = new OfflineSession();
            
            // Check to see if the player has MC installed
            // This check is easily bypassed, but files can easily be downloaded
            if (!Launcher.getInstance().getOptions().getIdentities().getHasLoggedIn()) {
                throw new ExecutionException("Login once before using offline mode.");
            }
        } else {
            session = new LegacySession(identity.getId());
        }

        LauncherUtils.checkInterrupted();
        
        // We may have to login!
        if (!session.isValid()) {
            login();
        }
    }
    
    /**
     * Try logging in.
     * 
     * @throws ExecutionException on error while executing
     */
    private void login() throws ExecutionException {
        fireTitleChange("Logging in...");
        fireStatusChange("Connecting to login server...");
        fireValueChange(-1);
        
        try {
            session.login(identity.getPassword());
            
            // Store the fact that we've logged in successfully
            options.getIdentities().setHasLoggedIn(true);
            options.save();
        
        // Bad login
        } catch (InvalidCredentialsException e) {
            throw new ExecutionException(
                    "You've entered an invalid username/password combination.");
        
        // This refers to the official launcher
        } catch (OutdatedLauncherException e) {
            throw new ExecutionException(
                    "It looks like this launcher needs to be updated.");
            
        // Switch to demo mode if the user account is not paid
        } catch (UserNotPremiumException e) {
            if (!launchOptions.isDemoMode()) {
                SwingHelper.showError(
                        getComponent(), "Not Premium", 
                        "You aren't logging in to a premium account.\n" +
                        "Minecraft will run in demo mode.");
            }
            
            launchOptions.setDemoMode(true);
        
        // An unknown error
        } catch (LoginException e) {
            throw new ExecutionException("A login error has occurred: " + e.getMessage());

        // A specific type of IO error
        } catch (SSLHandshakeException e) {
            throw new ExecutionException(
                    "Uh oh, couldn't confirm that the the login server " +
                    "connected to was owned by Mojang. You probably need to " +
                    "update your launcher.");
        
        // General I/O error
        } catch (final IOException e) {
            String message;
            if (e instanceof UnknownHostException) {
                message = "host is unresolved: " + e.getMessage();
            } else {
                message = e.getMessage();
            }
            
            if (SwingHelper.confirm(getComponent(), "Login error", 
                    "The Minecraft login server is unreachable (" + message + "). " +
                            "Would you like to play offline?")) {
                launchOptions.setPlayOffline(true);
            }
        }
    }
    
    /**
     * Check for updates.
     * 
     * @throws ExecutionException on error while executing
     * @throws InterruptedException thrown on interruption
     * @throws UpdateException on an update error
     */
    public void checkForUpdates() throws 
            ExecutionException, InterruptedException, UpdateException {
        
        // Don't update in offline mode
        if (launchOptions.isPlayingOffline()) {
            return;
        }
        
        // Only update if the JAR is set to <default>
        if (!activeJar.allowsUpdate()) {
            return;
        }

        fireStatusChange("Checking for updates...");
        
        File cacheFile = new File(minecraftDir, "update_cache.xml");
        UpdateCache cache = new UpdateCache(cacheFile);
        UpdateCheck updateCheck = configuration.createUpdateCheck(session, cache);
        UpdateType updateType;

        // Special case update scenarios
        if (!activeJar.isInstalled(minecraftDir)) {
            updateType = UpdateType.FULL;
        } else if (launchOptions.isForcingUpdate()) {
            updateType = UpdateType.FULL;
        } else if (launchOptions.isForcingIncrementalUpdate()) {
            updateType = UpdateType.INCREMENTAL;
        
        // Otherwise check for an update
        } else {
            try {
                if (!updateCheck.needsUpdate()) {
                    return; // No update needed
                }
            } catch (InterruptedException e) {
                throw new InterruptedException();
            } catch (UpdateException e) {
                if (!SwingHelper.confirm(getComponent(), 
                        "Update check failed", 
                        "The update server is unreachable (" + e.getMessage() + "). " +
                        "Would you like to continue playing without checking for updates?")) {
                    throw new InterruptedException();
                }
            }
            
            // Ask the user if s/he wants to update
            if (!SwingHelper.confirm(getComponent(), 
                    "Update available",
                    "An update is available. Would you like to update?")) {
                return;
            }  
            
            updateType = UpdateType.INCREMENTAL;
        }

        LauncherUtils.checkInterrupted();

        fireStatusChange("Creating updater...");
        
        // Create the updater
        Updater updater = updateCheck.createUpdater();
        updater.setOwner(frame);
        
        // Add listeners
        for (ProgressListener listener : getProgressListenerList()) {
            updater.addProgressListener(listener);
        }

        LauncherUtils.checkInterrupted();

        fireTitleChange("Updating '" + configuration.getName() + "'...");
        
        try {
            updater.update(updateType);
        } catch (UpdateException e) {
            throw new ExecutionException(e.getMessage(), e);
        }
    }
    
    /**
     * Try launching.
     * 
     * @throws ExecutionException on error while executing
     */
    private void launch() throws ExecutionException {
        fireTitleChange("Launching...");
        fireStatusChange("Launching Minecraft...");
        fireValueChange(-1);
        
        // Build a SettingsList used for launch settings, that is combined from the
        // main launcher options as well as this configuration's options
        SettingsList settings = new SettingsList(
                Launcher.getInstance().getOptions().getSettings(),
                configuration.getSettings());

        LaunchProcessBuilder builder = 
                new LaunchProcessBuilder(configuration, session);
        builder.readSettings(settings);
        builder.setShowConsole(
                builder.getShowConsole() || launchOptions.getShowConsole());
        builder.setActiveJar(activeJar.getName());
        builder.setDemo(launchOptions.isDemoMode());
        builder.setAutoConnect(launchOptions.getAutoConnect());
        
        try {
            builder.launch();
        } catch (IOException e) {
            throw new ExecutionException(e.getMessage(), e.getCause());
        }
        
        // A System.exit() call may fire before we get here

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
            }
        });
    }

    /**
     * Request a cancel.
     */
    @Override
    public boolean tryCancel() {
        if (JOptionPane.showConfirmDialog(getComponent(), 
                "Are you sure you want to cancel?",
                "Cancel", JOptionPane.YES_NO_OPTION) != 0) {
            return false;
        }
        
        return true;
    }

}
