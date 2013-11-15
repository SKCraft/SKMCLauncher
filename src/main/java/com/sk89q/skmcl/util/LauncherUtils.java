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

package com.sk89q.skmcl.util;

import java.io.*;
import java.util.logging.Logger;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Various utility methods.
 */
public final class LauncherUtils {
    
    private LauncherUtils() {
    }
    
    /**
     * Gets the value of an enum from a name. The name can be null, causing
     * the return value to be null.
     * 
     * @param en enum
     * @param name name of item
     * @return value or null
     */
    public static <T extends Enum<T>> T valueOf(Class<T> en, String name) {
        if (name == null) return null;
        try {
            return Enum.valueOf(en, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Gets the value of an enum from a name. The name can be null, causing
     * the return value to be null.
     * 
     * @param en enum
     * @param name name of item, will be upper cased
     * @return value or null
     */
    public static <T extends Enum<T>> T uppercaseValueOf(Class<T> en, String name) {
        if (name == null) return null;
        return valueOf(en, name.toUpperCase());
    }
    
    /**
     * Get a filename's extension
     * 
     * @param name filename
     * @return extension, or an empty string for no extension
     */
    public static String getExtension(String name) {
        int index = name.lastIndexOf('.');
        if (index == 0) {
            return "";
        } else {
            return name.substring(index + 1, name.length());
        }
    }
    
    /**
     * Get a filename's extension
     * 
     * @param file file
     * @return extension, or an empty string for no extension
     */
    public static String getExtension(File file) {
        return getExtension(file.getName());
    }

    /**
     * Get a stack trace as a string.
     * 
     * @param t exception
     * @return stack trace
     */
    public static String getStackTrace(Throwable t) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        t.printStackTrace(printWriter);
        return result.toString();
    }

    /**
     * Just consume and discard a stream.
     * 
     * @param from stream to read
     */
    public static void consumeBlindly(InputStream from) {
        final InputStream in = from;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                try {
                    while (in.read(buffer) != -1) {
                    }
                } catch (IOException e) {
                } finally {
                    closeQuietly(in);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Given a path, get a File instance for the given directory. If the
     * directory does not exist, keep going up the path to find a directory
     * that does exist. If no directory can be found, return null.
     * 
     * @param path path, or null to return null
     * @return File representing directory, or null
     */
    public static File getClosestDirectory(String path) {
        if (path == null) return null;
        File f = new File(path);
        while (!f.isDirectory()) {
            File parent = f.getParentFile();
            if (parent == null) {
                return null; // Path simply doesn't exist or we can't get a better one
            }
            f = parent;
        }
        return f;
    }

    /**
     * Check if the thread interruption flag is set.
     * 
     * @throws InterruptedException thrown if set
     */
    public static void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    /**
     * Get a relative path to a base directory.
     * 
     * @param base the base directory
     * @param path the path to make relative
     * @return the relativized path
     */
    public static String getRelative(File base, File path) {
        return base.toURI().relativize(path.toURI()).getPath();
    }

    /**
     * Read an {@link InputStream} to a string.
     *
     * @param is the input stream
     * @param encoding the encoding to read with
     * @return the string
     * @throws IOException on I/O error
     */
    public static String toString(InputStream is, String encoding) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, encoding));

        StringBuilder s = new StringBuilder();
        char[] buf = new char[1024];
        int len = 0;
        while ((len = reader.read(buf)) != -1) {
            s.append(buf, 0, len);
        }
        return s.toString();
    }

    /**
     * Get a logger for a given class.
     *
     * @param cls the class
     * @return a logger
     */
    public static Logger getLogger(Class<?> cls) {
        return Logger.getLogger(cls.getCanonicalName());
    }

    /**
     * Check whether the property {class}.{key} is true.
     *
     * @param cls the class
     * @param key the key
     * @return true if set
     */
    public static boolean hasSystemProperty(Class<?> cls, String key) {
        String name = cls.getCanonicalName() + "." + key;
        String value = System.getProperty(name, "false");
        return value.equalsIgnoreCase("true");
    }

}
