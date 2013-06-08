package com.sk89q.mclauncher.launch;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.LoginSession;
import com.sk89q.mclauncher.addons.Addon;
import com.sk89q.mclauncher.addons.AddonsProfile;
import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.config.Def;
import com.sk89q.mclauncher.config.SettingsList;
import com.sk89q.mclauncher.util.ConsoleFrame;
import com.sk89q.mclauncher.util.Util;

public class LaunchProcessBuilder {
    
    private static final Pattern maxPermGenPattern = 
            Pattern.compile("^-XX:MaxPermSize=.*$", Pattern.CASE_INSENSITIVE);

    private final Configuration configuration;
    private final String username;
    private final LoginSession session;
    
    private String activeJar;
    private boolean demo = false;
    private String autoConnect; // Add this later
    
    // Settings
    private String runtimePath;
    private String wrapperPath;
    private int minMem = 1024;
    private int maxMem = 1024;
    private String[] extraArgs = new String[0];
    private String extraClasspath;
    private boolean showConsole;
    private boolean relaunch;
    private boolean coloredConsole;
    private boolean consoleKillsProcess;
    private boolean fullScreen;
    private boolean lwjglDebug = false;
    private int windowWidth = 300;
    private int windowHeight = 300;
    
    private ConsoleFrame consoleFrame;
    
    public LaunchProcessBuilder(Configuration configuration, 
            String username, LoginSession session) {
        this.configuration = configuration;
        this.username = username;
        this.session = session;
    }

    public void readSettings(SettingsList settings) {
        runtimePath = Util.nullEmpty(settings.get(Def.JAVA_RUNTIME));
        wrapperPath = Util.nullEmpty(settings.get(Def.JAVA_WRAPPER_PROGRAM));
        minMem = settings.getInt(Def.JAVA_MIN_MEM, 128);
        maxMem = settings.getInt(Def.JAVA_MAX_MEM, 1024);
        extraArgs = settings.get(Def.JAVA_ARGS, "").split(" +");
        extraClasspath = Util.nullEmpty(settings.get(Def.JAVA_CLASSPATH));
        showConsole = (this.showConsole || settings.getBool(Def.JAVA_CONSOLE, false));
        relaunch = settings.getBool(Def.LAUNCHER_REOPEN, false);
        coloredConsole = settings.getBool(Def.COLORED_CONSOLE, true);
        consoleKillsProcess = settings.getBool(Def.CONSOLE_KILLS_PROCESS, true);
        fullScreen = settings.getBool(Def.WINDOW_FULLSCREEN, false);
        lwjglDebug = settings.getBool(Def.LWJGL_DEBUG, false);
        windowWidth = settings.getInt(Def.WINDOW_WIDTH, 300);
        windowHeight = settings.getInt(Def.WINDOW_HEIGHT, 300);
    }
    
    public String getActiveJar() {
        return activeJar;
    }

    public void setActiveJar(String activeJar) {
        this.activeJar = activeJar;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }

    public String getAutoConnect() {
        return autoConnect;
    }

    public void setAutoConnect(String autoConnect) {
        this.autoConnect = autoConnect;
    }

    public boolean getShowConsole() {
        return showConsole;
    }

    public void setShowConsole(boolean showConsole) {
        this.showConsole = showConsole;
    }

    private String getLauncherPath() throws IOException {
        try {
            return Launcher.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            throw new IOException("The path to the launcher could not be discovered.", e);
        }
    }
    
    public void launch() throws IOException {
        String validatedRuntimePath = "";
        
        // Figure out what to use for the Java runtime
        if (runtimePath != null) {
            File test = new File(runtimePath);
            // Try the parent directory
            if (!test.exists()) {
                throw new IOException("The configured Java runtime path '" + runtimePath + "' doesn't exist.");
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
        File actualWorkingDirectory = configuration.getBaseDir();
        File jarFile = new File(configuration.getMinecraftDir(), "bin/" + activeJar);
        
        if (!jarFile.exists()) {
            throw new IOException("Launch failed -- can't find '"
                    + jarFile.getAbsolutePath() + "'.");
        }

        // Get addons
        List<Addon> addons;
        try {
            AddonsProfile addonsProfile = configuration.getAddonsProfile(activeJar);
            addonsProfile.read();
            addons = addonsProfile.getEnabledAddons();
        } catch (IOException e) {
            throw new IOException("Failed to get addons list: " + e.getMessage(), e);
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
        if (lwjglDebug) {
            params.add("-Dorg.lwjgl.util.Debug=true");
        }
        
        // Add extra arguments
        boolean userIncreasedPermGen = false;
        for (String arg : extraArgs) {
            arg = arg.trim();
            if (arg.length() > 0) {
                params.add(arg);
                if (maxPermGenPattern.matcher(arg).matches()) {
                    userIncreasedPermGen = true;
                }
            }
        }
        
        // If the user didn't increase the max permanent generation size, then
        // we do it ourselfs -- perm. gen is where classes stay in memory
        if (!userIncreasedPermGen) {
            params.add("-XX:MaxPermSize=256M");
        }
        
        // Add classpath
        params.add("-classpath");
        params.add(getLauncherPath()
                + (extraClasspath != null ? File.pathSeparator + extraClasspath
                        : ""));

        // Class to run
        params.add(GameLauncher.class.getCanonicalName());

        // Child launcher flags
        params.add("-width");
        params.add(String.valueOf(windowWidth));
        params.add("-height");
        params.add(String.valueOf(windowHeight));
        
        // Child launcher arguments
        params.add(actualWorkingDirectory.getAbsolutePath());
        params.add(activeJar);
        
        ProcessBuilder procBuilder = new ProcessBuilder(params);
        
        // Have to do this for Windows here; can't do it in the launcher spawn
        procBuilder.environment().put("APPDATA", actualWorkingDirectory.getAbsolutePath());
        
        // Start the baby!
        final Process proc;
        try {
            proc = procBuilder.start();
        } catch (IOException e) {
            throw new IOException("The game could not be started: " + e.getMessage(), e);
        }
        
        // Create console
        if (showConsole) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    consoleFrame = new ConsoleFrame(
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
        if (fullScreen) {
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
                            if (consoleFrame != null) {
                                consoleFrame.waitFor();
                            }
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

}
