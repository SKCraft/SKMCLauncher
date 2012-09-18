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

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLHandshakeException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.sk89q.mclauncher.LoginSession.LoginException;
import com.sk89q.mclauncher.LoginSession.OutdatedLauncherException;
import com.sk89q.mclauncher.addons.Addon;
import com.sk89q.mclauncher.addons.AddonsProfile;
import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.config.Def;
import com.sk89q.mclauncher.config.LauncherOptions;
import com.sk89q.mclauncher.launch.GameLauncher;
import com.sk89q.mclauncher.update.CancelledUpdateException;
import com.sk89q.mclauncher.update.UpdateCache;
import com.sk89q.mclauncher.update.UpdateCheck;
import com.sk89q.mclauncher.update.UpdateException;
import com.sk89q.mclauncher.update.Updater;
import com.sk89q.mclauncher.util.ConsoleFrame;
import com.sk89q.mclauncher.util.SettingsList;
import com.sk89q.mclauncher.util.UIUtil;
import com.sk89q.mclauncher.util.Util;

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
    private String activeJar;
    private LoginSession session;
    private Configuration configuration;
    private File rootDir;
    private boolean playOffline = false;
    private boolean skipUpdateCheck = false;
    private boolean forceUpdate = false;
    private boolean wantUpdate = false;
    private boolean notInstalled = false;
    private volatile Updater updater;
    private boolean demo = false;

    private boolean showConsole = false;
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
            String username, String password, String jar) {
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
     * Set to show the Java console.
     * 
     * @param showConsole true to show console
     */
    public void setShowConsole(boolean showConsole) {
        this.showConsole = showConsole;
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
     * Execute the launch task.
     */
    @Override
    public void execute() throws ExecutionException {
        rootDir = configuration.getMinecraftDir();
        rootDir.mkdirs();
        
        session = new LoginSession(username);
        
        if (!playOffline) {
            login();
        }
        
        notInstalled = (activeJar == null && !(new File(rootDir, "bin/minecraft.jar").exists()));
        
        if (activeJar == null && !skipUpdateCheck) {
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
        
        LauncherOptions options = Launcher.getInstance().getOptions();
        SettingsList settings = new SettingsList(
                options.getSettings(), configuration.getSettings());
        
        // Find launcher path
        String launcherPath;
        try {
            launcherPath = Launcher.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            throw new ExecutionException("The path to the launcher could not be discovered.", e);
        }
        
        // Read some settings
        String username = playOffline ? "Player" : this.username;
        String runtimePath = Util.nullEmpty(settings.get(Def.JAVA_RUNTIME));
        String wrapperPath = Util.nullEmpty(settings.get(Def.JAVA_WRAPPER_PROGRAM));
        int minMem = settings.getInt(Def.JAVA_MIN_MEM, 128);
        int maxMem = settings.getInt(Def.JAVA_MAX_MEM, 1024);
        String[] extraArgs = settings.get(Def.JAVA_ARGS, "").split(" +");
        String extraClasspath = Util.nullEmpty(settings.get(Def.JAVA_CLASSPATH));
        final boolean showConsole = (this.showConsole || settings.getBool(Def.JAVA_CONSOLE, false));
        final boolean relaunch = settings.getBool(Def.LAUNCHER_REOPEN, false);
        final boolean coloredConsole = settings.getBool(Def.COLORED_CONSOLE, true);
        final boolean consoleKillsProcess = settings.getBool(Def.CONSOLE_KILLS_PROCESS, true);
        String validatedRuntimePath = "";
        
        // Figure out what to use for the Java runtime
        if (runtimePath != null) {
            File test = new File(runtimePath);
            // Try the parent directory
            if (!test.exists()) {
                throw new ExecutionException("The configured Java runtime path '" + runtimePath + "' doesn't exist.");
            } else if (test.isFile()) {
                test = test.getParentFile();
            }
            File test2 = new File(test, "bin");
            if (test2.isDirectory()) {
                test = test2;
            }
            validatedRuntimePath = test.getAbsolutePath() + File.separator;
        }
        
        // Set some things straight
        String actualJar = activeJar != null ? activeJar : "minecraft.jar";
        File actualWorkingDirectory = configuration.getBaseDir();
        
        if (!new File(configuration.getMinecraftDir(), "bin/" + actualJar).exists()) {
            throw new ExecutionException("The game is not installed.");
        }
        
        // Get addons
        List<Addon> addons;
        try {
            AddonsProfile addonsProfile = configuration.getAddonsProfile(actualJar);
            addonsProfile.read();
            addons = addonsProfile.getEnabledAddons();
        } catch (IOException e) {
            throw new ExecutionException("Failed to get addons list: " + e.getMessage(), e);
        }
        
        ArrayList<String> params = new ArrayList<String>();
        
        // Start with a wrapper
        if (wrapperPath != null) {
            params.add(wrapperPath);
        }
        
        // Choose the java version that we want
        params.add(validatedRuntimePath + "java");
        
        // Add memory options
        if (minMem > 0) {
            params.add("-Xms" + minMem + "M");
        }
        if (maxMem > 0) {
            params.add("-Xmx" + maxMem + "M");
        }
        
        // Add some Java flags
        params.add("-Dsun.java2d.noddraw=true");
        params.add("-Dsun.java2d.d3d=false");
        params.add("-Dsun.java2d.opengl=false");
        params.add("-Dsun.java2d.pmoffscreen=false");
        if (settings.getBool(Def.LWJGL_DEBUG, false)) {
            params.add("-Dorg.lwjgl.util.Debug=true");
        }
        
        // Add extra arguments
        for (String arg : extraArgs) {
            arg = arg.trim();
            if (arg.length() > 0) {
                params.add(arg);
            }
        }
        
        // Add classpath
        params.add("-classpath");
        params.add(launcherPath + (extraClasspath != null ? File.pathSeparator + extraClasspath : ""));
        
        // Class to run
        params.add(GameLauncher.class.getCanonicalName());

        // Child launcher flags
        params.add("-width");
        params.add(String.valueOf(settings.getInt(Def.WINDOW_WIDTH, 300)));
        params.add("-height");
        params.add(String.valueOf(settings.getInt(Def.WINDOW_HEIGHT, 300)));
        
        // Child launcher arguments
        params.add(actualWorkingDirectory.getAbsolutePath());
        params.add(actualJar);
        
        ProcessBuilder procBuilder = new ProcessBuilder(params);
        
        // Have to do this for Windows here; can't do it in the launcher spawn
        procBuilder.environment().put("APPDATA", actualWorkingDirectory.getAbsolutePath());
        
        // Start the baby!
        final Process proc;
        try {
            proc = procBuilder.start();
        } catch (IOException e) {
            throw new ExecutionException("The game could not be started: " + e.getMessage(), e);
        }
        
        // Create console
        if (showConsole) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ConsoleFrame consoleFrame = new ConsoleFrame(
                            10000, coloredConsole, proc, consoleKillsProcess);
                    consoleFrame.setVisible(true);
                    consoleFrame.consume(proc.getInputStream());
                    consoleFrame.consume(proc.getErrorStream(), Color.RED);
                }
            });
        }
        
        PrintStream out = new PrintStream(new BufferedOutputStream(proc.getOutputStream()));
        
        // Add parameters
        out.println("@username=" + username);
        out.println("@mppass=" + username);
        out.println("@sessionid=" + (session.isValid() ? session.getSessionId() : ""));
        if (demo) {
            out.println("@demo=true");
        }
        if (settings.getBool(Def.WINDOW_FULLSCREEN, false)) {
            out.println("@fullscreen=true");
        }
        if (autoConnect != null) {
            String[] parts = autoConnect.split(":", 2);
            if (parts.length == 1) {
                out.println("@server=" + parts[0]);
                out.println("@port=25565");
            } else {
                out.println("@server=" + parts[0]);
                out.println("@port=" + parts[1]);
            }
        }
        
        // Add enabled addons
        for (Addon addon : addons) {
            out.println("!" + addon.getFile().getAbsolutePath());
        }
        
        out.close(); // Here it starts
        
        if (showConsole || relaunch) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    frame.dispose();
                }
            });
            
            if (relaunch) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (!showConsole) {
                                Util.consumeBlindly(proc.getInputStream());
                                Util.consumeBlindly(proc.getErrorStream());
                            }
                            proc.waitFor();
                        } catch (InterruptedException e) {
                        }
                        Launcher.startLauncherFrame();
                    }
                }).start();
            }
        } else {
            System.exit(0);
        }
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
            
            username = session.getUsername();
        } catch (SSLHandshakeException e) {
            throw new ExecutionException("Verification of the identity of the authentication server failed. You may need to update the launcher, or someone has attmpted to steal your credentials.");
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
            UpdateCheck check = new UpdateCheck(updateUrl);
            try {
                check.checkUpdateServer();
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

            updateRequired = (cache.getLastUpdateId() == null ||
                    !cache.getLastUpdateId().equals(check.getLatestVersion()));
            packageDefUrl = check.getPackageDefUrl();
            latestVersion = check.getLatestVersion();
        }
        
        // Ask the user if s/he wants to update
        if (!forceUpdate && updateRequired && !notInstalled) {
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
        if (notInstalled || forceUpdate || (updateRequired && wantUpdate)) {
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
                    
                    update(rootDir, cache, conn.getInputStream(), forceUpdate, username, ticket);
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
                update(rootDir, cache, Launcher.class.getResourceAsStream("/resources/update.xml"),
                        forceUpdate, username, ticket);
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
     * @param rootDir path to the working directory of minecraft
     * @param packageStream input stream of the package .xml file
     * @param forced true to force re-download
     * @throws ExecutionException thrown on any error
     */
    private void update(File rootDir, UpdateCache cache, InputStream packageStream,
            boolean forced, String username, String ticket) throws ExecutionException {
        fireTitleChange("Updating Minecraft...");
        
        updater = new Updater(packageStream, rootDir, cache);
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
        
        // Remind the user to disable mods
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(getComponent(),
                            "Your game has been updated. If you encounter problems, " +
                            "try disabling any mods (if any) that you have installed.",
                            "Update completed", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
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
