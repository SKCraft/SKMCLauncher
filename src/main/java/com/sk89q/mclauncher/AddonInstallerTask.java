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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.sk89q.mclauncher.addons.Addon;
import com.sk89q.mclauncher.addons.AddonsProfile;
import com.sk89q.mclauncher.util.Util;

public class AddonInstallerTask extends Task {

    private static final Logger logger = Logger
            .getLogger(AddonInstallerTask.class.getCanonicalName());
    private AddonsProfile addonsProfile;
    private File file;
    private boolean removeOther = false;
    private volatile boolean running = true;

    public AddonInstallerTask(AddonsProfile addonsProfile, File file) {
        this.addonsProfile = addonsProfile;
        this.file = file;
    }

    @Override
    protected void execute() throws ExecutionException {
        fireTitleChange("Installing addon...");
        fireStatusChange("Installing " + file.getName() + "...");
        fireValueChange(-1);

        String id = file.getName().replaceAll("\\..*$", "");
        String name = file.getName().replaceAll("\\..*$", "");
        String detectedModLoaderName = null;
        boolean multipleModLoaderMods = false;
        InputStream in = null;
        int entriesCount = 0;
        boolean hasRoot = false;
        boolean foundMCClasses = false;
        String mungePath = null;
        boolean multipleMunge = false;

        Pattern mcClassRE = Pattern.compile("^.*[a-z]{1,3}\\.class$");
        Pattern modLoaderNameRE = Pattern.compile("^(?:.*/)?mod_([A-Za-z0-9_]+)\\.class$");
        
        // First, scan to see if we need to munge paths
        fireStatusChange("Scanning addon files...");
        try {
            in = new FileInputStream(file);
            JarInputStream jarFile = new JarInputStream(
                    new BufferedInputStream(in));
            JarEntry entry;

            while ((entry = jarFile.getNextJarEntry()) != null) {
                if (!running) {
                    throw new CancelledExecutionException();
                }

                if (entry.isDirectory())
                    continue;
                
                entriesCount++;
                
                String path = entry.getName().replace("\\", "/"); // Normalize
                
                if (path.startsWith("/"))
                    continue; // Invalid file!
                
                // Detect modLoader name
                Matcher matcher = modLoaderNameRE.matcher(path);
                if (matcher.matches()) {
                    if (detectedModLoaderName != null) {
                        multipleModLoaderMods = true;
                    } else {
                        detectedModLoaderName = matcher.group(1);
                    }
                }
                
                // Detect paths to munge
                if (mcClassRE.matcher(path).matches()) {
                    foundMCClasses = true;
                    if (path.indexOf('/') == -1) {
                        hasRoot = true;
                    } else {
                        int lastSlash = path.lastIndexOf('/');
                        String folderPath = path.substring(0, lastSlash + 1);
                        
                        // Can only have one munge path
                        if (mungePath != null) {
                            if (mungePath.startsWith(folderPath)) {
                                // Our current path encompasses our set
                                // munge path
                                mungePath = folderPath;
                            } else if (folderPath.startsWith(mungePath)) {
                                // Our set munge path is above this current path
                                continue;
                            } else {
                                // Not good
                                multipleMunge = true;
                            }
                        } else {
                            // Our first munge path, yippee
                            mungePath = folderPath;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new ExecutionException("Failed to scan the addon: "
                    + e.getMessage(), e);
        } catch (CancelledExecutionException e) {
            throw e;
        } finally {
            Util.close(in);
        }

        // Do we munge?
        boolean needToMunge = foundMCClasses && !hasRoot && mungePath != null
                && !multipleMunge;
        
        // If we have multiple munge paths, we best do nothing
        // Maybe we'll try munging anyway in the future
        if (multipleMunge) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(
                                getComponent(),
                                "To install addons where the Java .class files " +
                                "are not at the top of the archive, the installer " +
                                "tries to move the files to the root of the archive. " +
                                "However, in this case, it appears that the file " +
                                "you selected has multiple folders with Java files, " +
                                "so the installer will attempt to move nothing. You may " +
                                "have to manually install the addon by adding the files to " +
                                "minecraft.jar if the addon doesn't load properly.",
                                "Installation warning",
                                JOptionPane.WARNING_MESSAGE);
                    }
                });
            } catch (InterruptedException e) {
            } catch (InvocationTargetException e) {
            }
        }
        
        // Oh look, we have a name to use!
        if (detectedModLoaderName != null && !multipleModLoaderMods) {
            id = detectedModLoaderName;
            name = detectedModLoaderName;
        }
        
        // Is this addon already installed?
        if (addonsProfile.hasInstalled(id)) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        removeOther = JOptionPane.showConfirmDialog(
                                getComponent(),
                                "This addon appears to already be installed. " +
                                "Do you want to keep the other versions installed?",
                                "Already installed",
                                JOptionPane.YES_NO_OPTION) != 0;
                    }
                });
            } catch (InterruptedException e) {
            } catch (InvocationTargetException e) {
            }
        }
        
        // Remove other versions
        if (removeOther) {
            for (Addon addon : addonsProfile.getAddonsOfId(id)) {
                addonsProfile.remove(addon);
            }
            
            try {
                addonsProfile.write();
            } catch (IOException e) {
            }
        }

        File target = new File(addonsProfile.getDirectory(), file.getName());
        OutputStream out = null;
        
        int nameIndex = 0;
        while (target.exists()) {
            if (addonsProfile.isUsing(target)) {
                // Lame filename munging
                target = new File(target.getParentFile(), nameIndex + "_" + target.getName());
            } else {
                target.delete();
                break;
            }
        }

        // Then we actually install
        fireStatusChange("Installing files...");
        try {
            in = new FileInputStream(file);
            JarInputStream jarFile = new JarInputStream(
                    new BufferedInputStream(in));
            
            out = new FileOutputStream(target);
            JarOutputStream outFile = new JarOutputStream(
                    new BufferedOutputStream(out));
            
            byte[] buffer = new byte[8192];
            JarEntry entry;
            int i = 0;

            while ((entry = jarFile.getNextJarEntry()) != null) {
                if (!running) {
                    throw new CancelledExecutionException();
                }

                if (entry.isDirectory())
                    continue;

                fireStatusChange("Installing " + entry.getName() + "...");
                fireValueChange(i / (double) entriesCount);
                
                String path = entry.getName().replace("\\", "/"); // Normalize
                
                // No signatures!
                if (path.matches("^.*\\.[DSA|SF]$"))
                    continue;
                
                if (path.startsWith("/"))
                    continue; // Invalid file!
                
                // We need to munge the path
                if (needToMunge && path.startsWith(mungePath)) {
                    logger.finest("Munging the path " + path);
                    
                    if (path.length() <= mungePath.length()) {
                        // This is a problem that should not occur
                    } else {
                        path = path.substring(mungePath.length());
                    }
                }
                
                JarEntry outEntry = new JarEntry(path);
                outFile.putNextEntry(outEntry);

                int len = 0;
                while ((len = jarFile.read(buffer, 0, buffer.length)) != -1) {
                    outFile.write(buffer, 0, len);
                }
                
                outFile.closeEntry();
                
                i++;
            }
            
            outFile.close();
        } catch (IOException e) {
            throw new ExecutionException("Failed to install the addon: "
                    + e.getMessage(), e);
        } catch (CancelledExecutionException e) {
            Util.close(out);
            target.delete();
            throw e;
        } finally {
            Util.close(in);
            Util.close(out);
        }

        Addon addon = new Addon(id, name, null, target, null);
        addonsProfile.add(addon);

        try {
            addonsProfile.write();
        } catch (IOException e) {
            throw new ExecutionException(
                    "Failed to write the addon list to disk: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public Boolean cancel() {
        running = false;
        return null;
    }

}
