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

package com.sk89q.skmcl;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LauncherStub {

    private static final Logger logger = Logger.getLogger(
            LauncherStub.class.getCanonicalName());

    private static File getFileChooseDefaultDir() {
        JFileChooser chooser = new JFileChooser();
        FileSystemView fsv = chooser.getFileSystemView();
        return fsv.getDefaultDirectory();
    }

    private static File getUserLauncherDir() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return new File(getFileChooseDefaultDir(), "SKMCLauncher");
        } else {
            return new File(System.getProperty("user.home"), ".skmclauncher");
        }
    }

    private static boolean isPortableMode() {
        return new File("portable.txt").exists();
    }

    public static void main(String[] args) {
        boolean portable = isPortableMode();
        File dataDir = portable ? new File(".") : getUserLauncherDir();

        try {
            Class<?> cls = Class.forName("com.sk89q.skmcl.Launcher");
            Method method = cls.getMethod("launchFromStub",
                    boolean.class, File.class, String[].class);
            method.invoke(null, portable, dataDir, args);
        } catch (final Throwable t) {
            logger.log(Level.SEVERE, "Failed to launch", t);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null,
                            "An error has occurred:\n" +
                                    t.getClass().getCanonicalName() + ": " +
                                    t.getMessage() +
                                    "\n\nThe launcher will now close.",
                            "Launch Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }

    }

}
