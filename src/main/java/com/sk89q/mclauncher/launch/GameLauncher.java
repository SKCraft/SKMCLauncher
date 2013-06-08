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

package com.sk89q.mclauncher.launch;

import java.applet.Applet;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.util.BasicArgsParser;
import com.sk89q.mclauncher.util.BasicArgsParser.ArgsContext;
import com.sk89q.mclauncher.util.UIUtil;
import com.sk89q.mclauncher.util.Util;

/**
 * Does the heavy work of actually launching the game.
 * 
 * @author sk89q
 */
public class GameLauncher  {
    
    private static final Logger logger = Logger.getLogger(GameLauncher.class.getCanonicalName());

    private File baseDir;
    private File actualDir;
    private String activeJar;
    private ClassLoader classLoader;
    private Map<String, String> parameters = new HashMap<String, String>();
    private List<String> addonPaths = new ArrayList<String>();
    private Dimension windowDim;
    
    private GameLauncher(File baseDir, String activeJar) {
        logger.info("SK's Minecraft Launcher, v" + Launcher.VERSION);
        logJavaInformation();
        
        this.baseDir = baseDir;
        this.actualDir = Launcher.toMinecraftDir(baseDir);
        this.activeJar = activeJar;
    }
    
    private void logJavaInformation() {
        logger.info("-------------------------------------------------");
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        logger.info("Java arguments: " + runtimeBean.getInputArguments());
        logger.info("Library path: " + runtimeBean.getLibraryPath());
        logger.info("-------------------------------------------------");
        logger.info("Java version: " + System.getProperty("java.version"));
        logger.info("Java architecture: " + System.getProperty("sun.arch.data.model"));
        logger.info("JVM: " + runtimeBean.getVmName() + " version " + 
        runtimeBean.getVmVersion() + " (vendor: "+ runtimeBean.getVmVendor() + ")");
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        logger.info(String.format("HEAP initial: %d MB", 
                mem.getHeapMemoryUsage().getInit() / 1024 / 1024));
        logger.info(String.format("HEAP maximum: %d MB", 
                mem.getHeapMemoryUsage().getMax() / 1024 / 1024));
        logger.info(String.format("NON-HEAP initial: %d MB", 
                mem.getNonHeapMemoryUsage().getInit() / 1024 / 1024));
        logger.info(String.format("NON-HEAP maximum: %d MB", 
                mem.getNonHeapMemoryUsage().getMax() / 1024 / 1024));
        logger.info("-------------------------------------------------");
    }
    
    public void setParameter(String key, String val) {
        parameters.put(key, val);
    }
    
    public void addAddonPath(String path) {
        addonPaths.add(path);
    }
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
    public List<String> getAddonPaths() {
        return addonPaths;
    }
    
    private void setupEnvironment() throws LaunchException {
        System.setProperty("org.lwjgl.librarypath", new File(actualDir,
                "bin/natives").getAbsolutePath());
        System.setProperty("net.java.games.input.librarypath", new File(
                actualDir, "bin/natives").getAbsolutePath());
        
        String expected = baseDir.getAbsolutePath();
        
        System.setProperty("user.home", expected);
        if (!System.getProperty("user.home").equals(expected)) {
            throw new LaunchException("user.home was supposed to be set to '" + expected + "', but it was '" +
                    System.getProperty("user.home") + "'");
        }
        
        String appData = System.getenv("APPDATA");
        if (appData == null || !appData.equals(expected)) {
            throw new LaunchException("APPDATA was supposed to be set to '" + expected + "', but it was '" +
                    appData + "'");
        }
        
        // set minecraft.applet.WrapperClass to support newer FML builds
        // FML seems to restart the whole game which causes some problems in custom launchers like this one
        System.setProperty("minecraft.applet.WrapperClass", "com.sk89q.mclauncher.launch.GameAppletContainer");
        
        logger.info("Base directory: " + baseDir.getAbsolutePath());
        logger.info("What Minecraft will use: " + actualDir.getAbsolutePath());
    }
    
    private void setupClassLoader() {
        List<File> files = new ArrayList<File>();

        for (int i = addonPaths.size() - 1; i >= 0; i--) {
            String path = addonPaths.get(i);
            File f = new File(path);
            if (f.exists()) {
                logger.info("Addon: " + f.getAbsolutePath());
            } else {
                logger.warning("Addon doesn't exist: " + f.getAbsolutePath());
            }
            files.add(f);
        }
        
        files.add(new File(actualDir, "bin/lwjgl.jar"));
        files.add(new File(actualDir, "bin/jinput.jar"));
        files.add(new File(actualDir, "bin/lwjgl_util.jar"));
        files.add(new File(actualDir, "bin/" + activeJar));

        logger.info("List of classpath entries generated!");
        
        URL[] urls = new URL[files.size()];
        int i = 0;
        for (File file : files) {
            try {
                urls[i] = file.toURI().toURL();
            } catch (MalformedURLException e) {
            }
            logger.info("Classpath: " + urls[i]);
            i++;
        }
        
        classLoader = new RogueClassLoader(urls);
    }
    
    public Dimension getWindowDim() {
        return windowDim;
    }

    public void setWindowDim(Dimension windowDim) {
        this.windowDim = windowDim;
    }

    private void launch() throws LaunchException {
        final GameLauncher self = this;
        
        setupEnvironment();
        setupClassLoader();
        
        logger.info("Now launching...");
        
        try {
            LoaderCompat loaderCompat = new LoaderCompat(self);
            loaderCompat.installHooks();
            
            Class<?> cls = classLoader.loadClass("net.minecraft.client.MinecraftApplet");
            Applet game = (Applet) cls.newInstance();
            
            GameFrame frame = new GameFrame(windowDim);
            frame.setVisible(true);
            GameAppletContainer container = new GameAppletContainer(parameters, game, loaderCompat);
            frame.start(container);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Failed to launch", e);
            UIUtil.showError(null, "Launch error", "An error occurred while launching: " +
                    e.getMessage() + "\n\n" + Util.getStackTrace(e));
        }
    }
    
    private static void redirectLogger() {
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        
        rootLogger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                Level level = record.getLevel();
                Throwable t = record.getThrown();
                
                PrintStream out;
                if (t != null || level == Level.SEVERE || level == Level.WARNING) {
                    out = System.err;
                } else {
                    out = System.out;
                }
                
                out.println(record.getMessage());
                if (t != null) {
                    t.printStackTrace(out);
                }
                
                out.flush();
            }
            
            @Override
            public void flush() {
            }
            
            @Override
            public void close() throws SecurityException {
            }
        });
    }
    
    private static void setLookAndFeel() throws InterruptedException, InvocationTargetException {
        // Set look and fill
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager
                            .getSystemLookAndFeelClassName());
                } catch (Exception e) {
                }
            }
        });
    }
    
    /**
     * Entry point.
     * 
     * @param args arguments
     */
    public static void main(String[] args) {
        BasicArgsParser parser = new BasicArgsParser();
        parser.addValueArg("width");
        parser.addValueArg("height");
        //parser.addValueArg("origappdata");
        //parser.addFlagArg("relaunch");
        
        System.setErr(System.out);
        
        ArgsContext context = parser.parse(args);
        
        try {
            if (context.length() < 2) {
                throw new LaunchException("Missing parameters");
            }

            redirectLogger();
            setLookAndFeel();
            
            File dir = new File(context.get(0));
            String jar = context.get(1);
            int windowWidth = context.getInt("width", 854);
            int windowHeight = context.getInt("height", 480);
            
            GameLauncher launcher = new GameLauncher(dir, jar);
            launcher.setWindowDim(new Dimension(windowWidth, windowHeight));
            launcher.setParameter("stand-alone", "true");

            // Read arguments
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = in.readLine()) != null && line.length() != 0) {
                line = line.trim();

                if (line.startsWith("@")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length > 1 && parts[0].length() > 1) {
                        launcher.setParameter(parts[0].substring(1), parts[1]);
                    }
                } else if (line.startsWith("!") && line.length() > 1) {
                    launcher.addAddonPath(line.substring(1).trim());
                }
            }
            
            // Add relaunch hook
            /*if (context.has("relaunch")) {
                String originalAppData = context.get("origappdata");
                Runtime.getRuntime().addShutdownHook(new Thread(new LauncherRelauncher(originalAppData)));
            }*/
            
            launcher.launch();
        } catch (final LaunchException t) {
            logger.severe(t.getMessage());
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    UIUtil.showError(null, "Minecraft launch error", "An error occurred while launching: " +
                            t.getMessage());
                }
            });
        } catch (final Throwable t) {
            logger.log(Level.SEVERE, "Failed to start Minecraft", t);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    UIUtil.showError(null, "Minecraft launch error", "An error occurred while launching: " +
                            t.getMessage() + "\n\n" + Util.getStackTrace(t));
                }
            });
        }
    }
}
