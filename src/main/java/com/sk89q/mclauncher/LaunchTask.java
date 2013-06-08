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
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLHandshakeException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.sk89q.mclauncher.LoginSession.LoginException;
import com.sk89q.mclauncher.LoginSession.OutdatedLauncherException;
import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.config.DefaultJar;
import com.sk89q.mclauncher.config.LauncherOptions;
import com.sk89q.mclauncher.config.MinecraftJar;
import com.sk89q.mclauncher.config.SettingsList;
import com.sk89q.mclauncher.launch.LaunchProcessBuilder;
import com.sk89q.mclauncher.model.UpdateManifest;
import com.sk89q.mclauncher.update.CancelledUpdateException;
import com.sk89q.mclauncher.update.UpdateCache;
import com.sk89q.mclauncher.update.UpdateException;
import com.sk89q.mclauncher.update.UpdateManifestFetcher;
import com.sk89q.mclauncher.update.Updater;
import com.sk89q.mclauncher.util.UIUtil;

/**
 * Used for launching the game.
 * 
 * @author sk89q
 */
public class LaunchTask extends Task {
    
    private static final Logger logger = Logger.getLogger(LaunchTask.class.getCanonicalName());
    
    private volatile boolean running = true;
    
    private JFrame frame;
    private String username;
    private String password;
    private MinecraftJar activeJar;
    private LoginSession session;
    private Configuration configuration;
    private File rootDir;
    private boolean playOffline = false;
    private boolean skipUpdateCheck = false;
    private boolean forceUpdate = false;
    private boolean forceIncrementalUpdate = false;
    private boolean wantUpdate = false;
    private boolean notInstalled = false;
    private volatile Updater updater;
    private boolean demo = false;
    private boolean allowOfflineName = false;

    private boolean forceConsole = false;
    private String autoConnect;
    
    /**
     * Construct the launch task.
     * 
     * @param frame starting frame
     * @param configuration workspace
     * @param username username
     * @param password password
     * @param jar jar name
     */
    public LaunchTask(JFrame frame, Configuration configuration,
            String username, String password, MinecraftJar jar) {
        this.frame = frame;
        this.configuration = configuration;
        this.username = username;
        this.password = password;
        this.activeJar = jar;
    }
    
    /**
     * Set play online state.
     * 
     * @param playOffline true to play offline
     */
    public void setPlayOffline(boolean playOffline) {
        this.playOffline = playOffline;
    }

    /**
     * Set update force state.
     * 
     * @param forceUpdate true to force update
     */
    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

    /**
     * Set update force state.
     * 
     * @param forceIncrementalUpdate true to force update
     */
    public void setForceIncrementalUpdate(boolean forceIncrementalUpdate) {
        this.forceIncrementalUpdate = forceIncrementalUpdate;
        
    }
    
    /**
     * Set to show the Java console.
     * 
     * @param forceConsole true to show console
     */
    public void setForceConsole(boolean forceConsole) {
        this.forceConsole = forceConsole;
    }
    
    /**
     * Run Minecraft in demo mode.
     * 
     * @param demo true for demo mode, false for normal mode if a premium account.
     */
    public void setDemo(boolean demo) {
        this.demo = demo;
    }

    /**
     * Set the auto connect server address.
     * 
     * @param autoConnect address (addr:port, addr) or null
     */
    public void setAutoConnect(String autoConnect) {
        this.autoConnect = autoConnect;
    }
    
    /**
     * Set the ability to use the player's username while playing offline.
     * 
     * @param allow address (addr:port, addr) or null
     */
    public void setAllowOfflineName(boolean allow) {
        this.allowOfflineName = allow;
    }

    /**
     * Execute the launch task.
     */
    @Override
    public void execute() throws ExecutionException {
        rootDir = configuration.getMinecraftDir();
        rootDir.mkdirs();
        
        session = new LoginSession(username);
        
        if (!playOffline) {
            login();
        } else {
            if (!Launcher.getInstance().getOptions().getIdentities().getHasLoggedIn()) {
                throw new ExecutionException("Login once before using offline mode.");
            }
        }
        
        notInstalled = (activeJar instanceof DefaultJar &&
                !(new File(rootDir, "bin/minecraft.jar").exists()));
        
        if (activeJar instanceof DefaultJar && !skipUpdateCheck) {
            checkForUpdates();
        }
        
        launch();
    }
    
    /**
     * Try launching.
     * 
     * @throws ExecutionException
     *             on error while executing
     */
    public void launch() throws ExecutionException {
        fireTitleChange("Launching...");
        fireStatusChange("Launching Minecraft...");
        fireValueChange(-1);
        
        SettingsList settings = new SettingsList(
                Launcher.getInstance().getOptions().getSettings(),
                configuration.getSettings());
        String username = !allowOfflineName && playOffline ? "Player" : this.username;

        LaunchProcessBuilder builder = new LaunchProcessBuilder(
                configuration, username, session);
        builder.readSettings(settings);
        if (forceConsole) {
            builder.setShowConsole(forceConsole);
        }
        builder.setActiveJar(activeJar.getName());
        builder.setDemo(demo);
        builder.setAutoConnect(autoConnect);
        
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
     * Try logging in.
     * 
     * @throws ExecutionException on error while executing
     */
    public void login() throws ExecutionException {
        fireTitleChange("Logging in...");
        fireStatusChange("Connecting to " + session.getLoginURL().getHost() + "...");
        fireValueChange(-1);
        
        try {
            if (!session.login(password)) {
                throw new ExecutionException("You've entered an invalid username/password combination.");
            }
            
            LauncherOptions options = Launcher.getInstance().getOptions();
            options.getIdentities().setHasLoggedIn(true);
            options.save();
            
            username = session.getUsername();
        } catch (SSLHandshakeException e) {
            throw new ExecutionException("Uh oh, couldn't confirm that the the login server connected to was owned by Mojang. You probably need to update your launcher.");
        } catch (OutdatedLauncherException e) {
            throw new ExecutionException("Your launcher has to be updated.");
        } catch (LoginException e) {
            if (e.getMessage().equals("User not premium")) {
                if (!demo) {
                    UIUtil.showError(frame, "Not Premium", "You aren't logging in to a premium account.\nMinecraft will run in demo mode.");
                }
                demo = true;
            } else {
                throw new ExecutionException("A login error has occurred: " + e.getMessage());
            }
        } catch (final IOException e) {
            e.printStackTrace();
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        String message;
                        if (e instanceof UnknownHostException) {
                            message = "host is unresolved: " + e.getMessage();
                        } else {
                            message = e.getMessage();
                        }
                        
                        if (JOptionPane.showConfirmDialog(getComponent(), 
                                "The Minecraft login server is unreachable (" + message + "). " +
                                		"Would you like to play offline?",
                                "Login error", JOptionPane.YES_NO_OPTION) == 0) {
                            playOffline = true;
                        }
                    }
                });
                
                if (!playOffline) {
                    throw new CancelledExecutionException();
                }
            } catch (InterruptedException e1) {
            } catch (InvocationTargetException e1) {
            }
        } finally {
            password = null;
        }
    }
    
    /**
     * Check for updates.
     * 
     * @throws ExecutionException on error while executing
     */
    public void checkForUpdates() throws ExecutionException {
        // Check account
        if (!demo && !session.isValid() && !this.playOffline) {
            throw new ExecutionException("Please login first to download Minecraft.");
        }
        
        File cacheFile = new File(rootDir, "update_cache.xml");
        UpdateCache cache = new UpdateCache(cacheFile);
        
        boolean updateRequired = false;
        String ticket = "";
        String latestVersion = "";
        
        // We have a download ticket!
        if (session.isValid()) {
            ticket = session.getDownloadTicket();
        }
        
        URL updateUrl = configuration.getUpdateUrl();
        URL packageDefUrl = null;
        
        // Try to import the last version from the official launcher
        if (updateUrl == null && !cacheFile.exists()) {
            try {
                Launcher.getInstance().importLauncherUpdateVersion(cache);
                cache.write();
            } catch (IOException e) {
                logger.log(Level.WARNING,
                        "Failed to import version information from official launcher", e);
            }
        }
        
        // Is there a new version to update to?
        // For vanilla Minecraft, we have that information from the login, but
        // for custom versions, we need to check an update URL
        if (updateUrl == null) {
            // Default Minecraft workspace, so we already version info
            updateRequired = (session.isValid() &&
                    !session.getLatestVersion().equals(cache.getLastUpdateId()));
            
            latestVersion = session.getLatestVersion();
        } else {
            fireStatusChange("Checking for updates...");
            
            // Custom update URL, so we have to check this URL
            UpdateManifestFetcher check = new UpdateManifestFetcher(updateUrl);
            try {
                check.downloadManifest();
            } catch (final IOException e) {
                // Uh oh, update check went wrong!
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            String message;
                            if (e instanceof UnknownHostException) {
                                message = "host is unresolved: " + e.getMessage();
                            } else {
                                message = e.getMessage();
                            }
                            
                            if (JOptionPane.showConfirmDialog(getComponent(), 
                                    "The update server is unreachable (" + message + "). " +
                                            "Would you like to continue playing without check for updates?",
                                    "Login error", JOptionPane.YES_NO_OPTION) == 0) {
                                skipUpdateCheck = true;
                            }
                        }
                    });
                } catch (InterruptedException e1) {
                } catch (InvocationTargetException e1) {
                }
                
                // Handle the end result
                if (!skipUpdateCheck) {
                    throw new CancelledExecutionException();
                } else {
                    return;
                }
            }

            UpdateManifest manifest = check.getManifest();
            updateRequired = (cache.getLastUpdateId() == null ||
                    !cache.getLastUpdateId().equals(manifest.getLatestVersion()));
            try {
                packageDefUrl = manifest.toPackageURL(check.getUpdateURL());
            } catch (MalformedURLException e) {
                throw new ExecutionException("Invalid URL: " + manifest.getPackageURL());
            }
            latestVersion = manifest.getLatestVersion();
        }
        
        // Ask the user if s/he wants to update
        if (!forceUpdate && !forceIncrementalUpdate && updateRequired && !notInstalled) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        if (JOptionPane.showConfirmDialog(getComponent(), 
                                "An update is available. Would you like to update?",
                                "Update available", JOptionPane.YES_NO_OPTION) == 0) {
                            wantUpdate = true;
                        }
                    }
                });
            } catch (InterruptedException e) {
            } catch (InvocationTargetException e) {
            }
        }
        
        // Proceed with the update
        if (notInstalled || forceUpdate || forceIncrementalUpdate || (updateRequired && wantUpdate)) {
            // We have a custom package definition URL that we have to fetch!
            if (packageDefUrl != null) {
                fireStatusChange("Downloading package definition for update...");
                
                HttpURLConnection conn = null;
                
                try {
                    conn = (HttpURLConnection) packageDefUrl.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setUseCaches(false);
                    conn.setDoInput(true);
                    conn.setDoOutput(false);
                    conn.setReadTimeout(5000);

                    conn.connect();
                    
                    if (conn.getResponseCode() != 200) {
                        throw new IOException("Did not get expected 200 code");
                    }
                    
                    update(packageDefUrl, rootDir, cache, conn.getInputStream(), 
                            forceUpdate, forceIncrementalUpdate, username, ticket);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new ExecutionException("Could not fetch the update package definition file (" +
                            e.getMessage() + "). The update cannot be performed.");
                } finally {
                    if (conn != null) conn.disconnect();
                    conn = null;
                }
            } else {
                // For vanilla, we bundle the package
                update(packageDefUrl, rootDir, cache,
                        Launcher.class.getResourceAsStream("/resources/update.xml"),
                        forceUpdate, forceIncrementalUpdate, username, ticket);
            }
            
            // Check for cancel
            if (!running) {
                throw new CancelledExecutionException();
            }
            
            cache.setLastUpdateId(latestVersion);

            try {
                cache.write();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Download the updates listed in the given package .xml file and
     * apply them.
     * 
     * @param baseUrl the base URL of the update package
     * @param rootDir path to the working directory of minecraft
     * @param packageStream input stream of the package .xml file
     * @param forced true to force re-download
     * @param forcedIncremental true to force an incremental update
     * @throws ExecutionException thrown on any error
     */
    private void update(URL baseUrl, File rootDir, UpdateCache cache, InputStream packageStream,
            boolean forced, boolean forcedIncremental,
            String username, String ticket) throws ExecutionException {
        fireTitleChange("Updating Minecraft...");
        
        updater = new Updater(frame, baseUrl, packageStream, rootDir, cache);
        updater.setReinstall(forced);
        updater.registerParameter("user", username);
        updater.registerParameter("ticket", "deprecated"); // Now deprecated
        for (ProgressListener listener : getProgressListenerList()) {
            updater.addProgressListener(listener);
        }
        try {
            updater.performUpdate();
        } catch (CancelledUpdateException e) {
            throw new CancelledExecutionException();
        } catch (UpdateException e) {
            throw new ExecutionException(e.getMessage(), e);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Update error occurred", t);
            throw new ExecutionException("An unknown error occurred.", t);
        }
    }

    /**
     * Request a cancel.
     */
    @Override
    public Boolean cancel() {
        if (JOptionPane.showConfirmDialog(getComponent(), "Are you sure you want to cancel?",
            "Cancel", JOptionPane.YES_NO_OPTION) != 0) {
            return false;
        }
        
        if (updater != null) {
            updater.cancel();
        }
        
        return true;
    }

}
