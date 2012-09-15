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

package com.sk89q.mclauncher.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * Various utility methods.
 * 
 * @author sk89q
 */
public class Util {
    
    private Util() {
    }
    
    /**
     * Closes a {@link Closeable} object, eating {@link IOException} errors.
     * Will do nothing if the passed object is null.
     * 
     * @param o {@link Closeable} object
     */
    public static void close(Closeable o) {
        if (o == null) return;
        
        try {
            o.close();
        } catch (IOException e) {
        }
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
     * Returns the original item or a default if the original item is null.
     * 
     * @param obj object
     * @param def default
     * @return result
     */
    public static <T> T defaultValue(T obj, T def) {
        return obj != null ? obj : def;
    }
    
    /**
     * Returns the original string or null if the string is empty or null.
     * 
     * @param s original
     * @return string
     */
    public static String nullEmpty(String s) {
        if (s == null) return null;
        if (s.trim().length() == 0) return null;
        return s;
    }
    
    /**
     * Sleep without throwing an exception.
     * 
     * @param millis time in milliseconds
     * @return true if not interrupted
     */
    public static boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
    
    /**
     * Copy a file from one location to the next.
     * 
     * @param from from
     * @param to target
     * @throws IOException on error
     */
    public static void copyFile(File from, File to) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        
        try {
            in = new BufferedInputStream(new FileInputStream(from));
            out = new BufferedOutputStream(new FileOutputStream(to));
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
               out.write(buf, 0, len);
            }
        } finally {
            close(in);
            close(out);
        }
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
                    Util.close(in);
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
     * Given a file, attempts to extract the Minecraft version from it.
     *
     * @param file to parse
     * @return the version
     */
    public static String getMCVersion(File file) {
        String prefix = "Minecraft Minecraft ";
        Pattern magic = Pattern.compile("(" + prefix + "(\\w|\\.)+(?=\01\00))");
        String version = "Unknown";
        try {
            JarFile jar = new JarFile(file);
            ZipEntry entry = jar.getEntry("net/minecraft/client/Minecraft.class");
            if (entry != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)));
                String line;
                StringBuilder all = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    all.append(line);
                }
                br.close();
                Matcher matcher = magic.matcher(all);
                if (matcher.find()) {
                    version = matcher.group().substring(prefix.length());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return version;
    }

}
