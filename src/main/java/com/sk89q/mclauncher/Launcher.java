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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.swing.SwingUtilities;

import com.sk89q.mclauncher.config.Constants;
import com.sk89q.mclauncher.config.Def;
import com.sk89q.mclauncher.config.LauncherOptions;
import com.sk89q.mclauncher.security.X509KeyRing;
import com.sk89q.mclauncher.update.UpdateCache;
import com.sk89q.mclauncher.util.BasicArgsParser;
import com.sk89q.mclauncher.util.BasicArgsParser.ArgsContext;
import com.sk89q.mclauncher.util.ConsoleFrame;
import com.sk89q.mclauncher.util.Util;

/**
 * Launcher entry point.
 * 
 * @author sk89q
 */
public class Launcher {
    
    private static final Logger logger = Logger.getLogger(Launcher.class.getCanonicalName());
    public static final String VERSION;
    private static String noticesText;

    private static volatile ConsoleFrame consoleFrame;
    private LauncherOptions options;
    private X509KeyRing keyRing;
    private static Launcher instance;
    
    /**
     * Some initialization.
     */
    static {
        Package p = Launcher.class.getPackage();

        if (p == null) {
            p = Package.getPackage("com.sk89q.mclauncher");
        }

        if (p == null) {
            VERSION = "(unknown)";
        } else {
            String v = p.getImplementationVersion();

            if (v == null) {
                VERSION = "(unknown)";
            } else {
                VERSION = v;
            }
        }
    }
    
    /**
     * Get this instance.
     * 
     * @return instance of launcher
     */
    public static Launcher getInstance() {
        return instance;
    }
    
    /**
     * Construct the launcher.
     */
    private Launcher() {
        instance = this;
        
        System.setProperty("http.agent", "SKMCLauncher/" + VERSION + " (+http://www.sk89q.com)");
        
        // Read options
        File base = getLauncherDataDir();
        base.mkdirs();
        File optionsFile = new File(base, "config.xml");
        options = new LauncherOptions(optionsFile);
        options.load();
        
        // If the options file does not exist, try to import old data
        if (!optionsFile.exists()) {
            try {
                importLauncherLogin();
            } catch (IOException e) {
            }
        }
        
        keyRing = new X509KeyRing();
        try {
            Constants.register(keyRing);
        } catch (CertificateException e) {
            logger.log(Level.SEVERE, "Failed to install register built-in certificates", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to install register built-in certificates", e);
        }
    }

    /**
     * Get the options.
     * 
     * @return options
     */
    public LauncherOptions getOptions() {
        return options;
    }
    
    /**
     * Get the key ring.
     * 
     * @return key ring
     */
    public X509KeyRing getKeyRing() {
        return keyRing;
    }

    /**
     * Get a cipher.
     * 
     * @param mode cipher mode (see {@link Cipher} constants)
     * @param password password
     * @return cipher
     * @throws InvalidKeySpecException on cipher error
     * @throws NoSuchAlgorithmException on cipher error
     * @throws NoSuchPaddingException on cipher error
     * @throws InvalidKeyException on cipher error
     * @throws InvalidAlgorithmParameterException on cipher error
     */
    public Cipher getCipher(int mode, String password)
            throws InvalidKeySpecException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Random random = new Random(0x29482c2L);
        byte salt[] = new byte[8];
        random.nextBytes(salt);
        PBEParameterSpec paramSpec = new PBEParameterSpec(salt, 5);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = factory.generateSecret(new PBEKeySpec(password.toCharArray()));
        Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
        cipher.init(mode, key, paramSpec);
        return cipher;
    }
    
    /**
     * Get the data directory for the official launcher.
     * 
     * @return directory
     */
    public static File getOfficialDataDir() {
        return getAppDataDir("minecraft");
    }
    
    /**
     * Given a base directory path, get the actual path that will be used
     * by Minecraft, since we can't set the root data directory Minecraft
     * uses (in a reliable fashion, at least)
     * 
     * @param base base path
     * @return final path
     */
    public static File toMinecraftDir(File base) {
        switch (getPlatform()) {
            case LINUX:
            case SOLARIS:
                return new File(base, ".minecraft");
            case WINDOWS:
                return new File(base, ".minecraft");
            case MAC_OS_X:
                return new File(base,
                        "Library/Application Support/minecraft");
            default:
                return new File(base, "minecraft");
        }
    }

    /**
     * Get the path the system's application data directory as with
     * Minecraft standards.
     * 
     * @return file
     */
    public static File getAppDataDir() {
        String homeDir = System.getProperty("user.home", ".");
        File workingDir;

        switch (getPlatform()) {
        case LINUX:
        case SOLARIS:
            workingDir = new File(homeDir);
            break;
        case WINDOWS:
            String applicationData = System.getenv("APPDATA");
            if (applicationData != null)
                workingDir = new File(applicationData);
            else
                workingDir = new File(homeDir);
            break;
        case MAC_OS_X:
            workingDir = new File(homeDir);
            break;
        default:
            workingDir = new File(homeDir);
        }
        
        return workingDir;
    }

    /**
     * Get the path of a folder in the system's application data directory, with
     * the folder being of the format that Minecraft expects.
     * 
     * @param appDir
     *            application directory name
     * @return file
     */
    public static File getAppDataDir(String appDir) {
        String homeDir = System.getProperty("user.home", ".");
        File workingDir;

        switch (getPlatform()) {
        case LINUX:
        case SOLARIS:
            workingDir = new File(homeDir, "." + appDir + "/");
            break;
        case WINDOWS:
            String applicationData = System.getenv("APPDATA");
            if (applicationData != null)
                workingDir = new File(applicationData, "." + appDir + "/");
            else
                workingDir = new File(homeDir, "." + appDir + "/");
            break;
        case MAC_OS_X:
            workingDir = new File(homeDir, "Library/Application Support/"
                    + appDir);
            break;
        default:
            workingDir = new File(homeDir, appDir + "/");
        }
        
        return workingDir;
    }
    
    /**
     * Get the data directory for the launcher.
     * 
     * @return directory
     */
    public static File getLauncherDataDir() {
        String homeDir = System.getProperty("user.home", ".");
        File workingDir = new File(".", "config.xml");
        if (workingDir.exists()) {
            return new File(".");
        }
        
        switch (getPlatform()) {
            case LINUX:
            case SOLARIS:
                workingDir = new File(homeDir, ".skmclauncher");
                break;
            case WINDOWS:
                String applicationData = System.getenv("APPDATA");
                if (applicationData != null) {
                    workingDir = new File(applicationData, "SKMCLauncher");
                } else {
                    workingDir = new File(homeDir, "SKMCLauncher");
                }
                break;
            case MAC_OS_X:
                workingDir = new File(homeDir,
                        "Library/Application Support/SKMCLauncher");
                break;
            default:
                workingDir = new File(homeDir, "SKMCLauncher");
        }
        if (!new File(workingDir, "config.xml").exists()) {
            workingDir = getOfficialDataDir();
        }
        if (!workingDir.exists() && !workingDir.mkdirs()) {
            throw new RuntimeException("Unable to create " + workingDir);
        }
        
        return workingDir;
    }
    
    /**
     * Import old launcher settings.
     * 
     * @throws IOException on I/O error
     */
    private void importLauncherLogin() throws IOException {
        File file = new File(getOfficialDataDir(), "lastlogin");
        if (!file.exists()) return;
        
        DataInputStream in = null;
        try {
            Cipher cipher = getCipher(Cipher.DECRYPT_MODE, "passwordfile");
            in = new DataInputStream(new CipherInputStream(
                    new FileInputStream(file), cipher));
            String username = in.readUTF();
            String password = in.readUTF();
            if (username.trim().length() == 0) {
                return;
            }
            if (password.trim().length() == 0) {
                password = null;
            }
            options.saveIdentity(username, password);
            options.setLastUsername(username);
        } catch (InvalidKeyException e) {
            throw new IOException(e);
        } catch (InvalidKeySpecException e) {
            throw new IOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (NoSuchPaddingException e) {
            throw new IOException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new IOException(e);
        } finally {
            Util.close(in);
        }
    }
    
    /**
     * Import old launcher game version information.
     * 
     * @param cache update cache to update
     * @throws IOException on I/O error
     */
    public void importLauncherUpdateVersion(UpdateCache cache) throws IOException {
        File file = new File(getOfficialDataDir(), "bin/version");
        if (!file.exists()) return;
        
        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(file));
            String version = in.readUTF();
            cache.setLastUpdateId(version);
        } finally {
            Util.close(in);
        }
    }
    
    /**
     * Show the console window. If the console window doesn't yet exist, it
     * will be created. If it already exists, a new one will not be created.
     * Messages logged to the logging system while the console is active
     * will appear in the console.
     */
    public static void showConsole() {
        if (consoleFrame != null) return;

        final boolean colorEnabled = Launcher.getInstance().getOptions()
                .getSettings().getBool(Def.COLORED_CONSOLE, true);
        
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    ConsoleFrame frame = consoleFrame;
                    if (frame == null || frame.isActive()) {
                        frame = new ConsoleFrame(10000, colorEnabled);
                        consoleFrame = frame;
                        frame.setTitle("Launcher Debugging Console");
                        frame.registerLoggerHandler();
                        frame.setVisible(true);
                    }
                }
            });
        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
        }
    }

    /**
     * Detect platform.
     * 
     * @return platform
     */
    public static Platform getPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win"))
            return Platform.WINDOWS;
        if (osName.contains("mac"))
            return Platform.MAC_OS_X;
        if (osName.contains("solaris") || osName.contains("sunos"))
            return Platform.SOLARIS;
        if (osName.contains("linux"))
            return Platform.LINUX;
        if (osName.contains("unix"))
            return Platform.LINUX;
        
        return Platform.UNKNOWN;
    }
    
    /**
     * Get the copyright notices.
     * 
     * @return notice
     */
    public static String getNotices() {
        if (noticesText != null) {
            return noticesText;
        }
        
        BufferedReader in = null;
        try {
            InputStream f = Launcher.class.getResourceAsStream("/resources/NOTICE.txt");
            if (f == null) {
                logger.log(Level.WARNING, "Failed to read NOTICE.txt");
                Util.close(in);
                return noticesText = "<Failed to read NOTICE.txt>";
            }
            in = new BufferedReader(new InputStreamReader(f));
            StringBuilder contents = new StringBuilder();
            char[] buffer = new char[4096];
            int read = 0;
            do {
                contents.append(buffer, 0, read);
                read = in.read(buffer);
            } while (read >= 0);
            return noticesText = contents.toString();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read NOTICE.txt", e);
            Util.close(in);
            return noticesText = "<Failed to read NOTICE.txt>";
        }
    }

    /**
     * Entry point.
     * 
     * @param args arguments
     */
    public static void main(String[] args) {
        BasicArgsParser parser = new BasicArgsParser();
        parser.addValueArg("address");
        parser.addValueArg("username");
        parser.addValueArg("password");
        parser.addFlagArg("launch");
        
        ArgsContext context = parser.parse(args);
        final String autoConnect = context.get("address");
        final String username = context.get("username");
        final String password = context.get("password");
        final boolean autoLaunch = context.has("launch");

        new Launcher();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LauncherFrame frame = new LauncherFrame();
                frame.setVisible(true);
                
                if (username != null) {
                    frame.setLogin(username, password);
                }
                
                if (autoConnect != null && autoConnect.matches("^[A-Za-z0-9\\.]{1,128}(?::[0-9]+)?$")) {
                    frame.setAutoConnect(autoConnect);
                }
                
                if (autoLaunch) {
                    frame.launch();
                }
            }
        });
    }
    
    /**
     * Start the launcher frame.
     */
    static void startLauncherFrame() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LauncherFrame frame = new LauncherFrame();
                frame.setVisible(true);
            }
        });
    }
}
