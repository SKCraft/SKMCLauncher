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

package com.sk89q.skmcl.minecraft;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Builds a .jar file consisting of the contents of other .jar files.
 *
 * <p>If the list of included .jar files is modified, then {@link #setDirty(boolean)}
 * should be called.</p>
 *
 * <p>Errors generated when merging in a particular JAR will be ignored, but will be
 * printed to the log.</p>
 */
@Log
@ToString
public class JarBuilder {

    private transient File source;
    private transient File target;
    private transient File dir;

    @Getter @Setter
    private boolean dirty;
    @Getter @Setter @NonNull
    private List<String> included = new ArrayList<String>();

    @JsonIgnore
    public File getSource() {
        return source;
    }
    @JsonIgnore
    public File getTarget() {
        return target;
    }

    @JsonIgnore
    public File getDir() {
        return dir;
    }

    /**
     * Set the paths to use.
     *
     * @param source the base .jar
     * @param target the target .jar
     * @param dir the directory where the .jars to be merged in are located
     */
    public void setPaths(@NonNull File source, @NonNull File target, @NonNull File dir) {
        this.source = source;
        this.target = target;
        this.dir = dir;
    }

    /**
     * Add the given archive tot he list of included patches.
     *
     * @param path the path
     * @throws IOException on I/O error
     */
    public void install(File path) throws IOException {
        String name = path.getName();
        File dest = new File(getDir(), name);
        FileUtils.copyFile(path, dest);
        included.add(name);
    }

    /**
     * Rebuild the target JAR file to contain the list of included JAR files.
     *
     * @throws IOException on I/O error
     */
    public void rebuild() throws IOException {
        Set<String> existing = new HashSet<String>();

        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        ZipOutputStream zos = null;

        log.log(Level.INFO,
                "Rebuilding {0}", getTarget().getAbsolutePath());

        try {
            fos = new FileOutputStream(getTarget());
            bos = new BufferedOutputStream(fos);
            zos = new ZipOutputStream(bos);

            // Merge in the patch .jars in reverse order
            ListIterator<String> it = getIncluded().listIterator(getIncluded().size());
            while (it.hasPrevious()) {
                File file = new File(dir, it.previous());
                if (file.exists()) {
                    try {
                        mergeInto(zos, file, existing);
                    } catch (IOException e) {
                        log.log(Level.WARNING,
                                "Failed to merge in " + file.getAbsolutePath(), e);
                    }
                } else {
                    log.log(Level.WARNING,
                            "Can't merge in " + file.getAbsolutePath());
                }
            }

            // Now merge in the base .jar
            mergeInto(zos, getSource(), existing);

            setDirty(false);
        } finally {
            closeQuietly(zos);
            closeQuietly(bos);
            closeQuietly(fos);
        }
    }

    /**
     * Merge the given file into the ZIP output stream, taking care to not duplicate
     * files that are in the given set.
     *
     * @param zos the zip output stream
     * @param file the file
     * @param existing a set of paths that already exist in the target ZIP
     * @throws IOException thrown on I/O error
     */
    private static void mergeInto(ZipOutputStream zos, File file, Set<String> existing)
            throws IOException {
        log.log(Level.INFO,
                "Merging in {0}", file.getAbsolutePath());

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ZipInputStream zis = null;
        ZipEntry entry;

        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            zis = new ZipInputStream(bis);

            while ((entry = zis.getNextEntry()) != null) {
                String path = normalize(entry.getName());

                if (matches(path) && !existing.contains(path)) {
                    existing.add(path);
                    zos.putNextEntry(new ZipEntry(path));
                    IOUtils.copy(zis, zos);
                }
            }
        } finally {
            closeQuietly(zis);
            closeQuietly(bis);
            closeQuietly(fis);
        }
    }

    /**
     * Returns whether the path should be included in the final JAR.
     *
     * @param path the path
     * @return true if the path should be included
     */
    private static boolean matches(String path) {
        return !path.toUpperCase().startsWith("META-INF/");
    }

    /**
     * Normalize the path to be stored in the ZIP.
     *
     * @param path the path
     * @return the normalized path
     */
    private static String normalize(String path) {
        return separatorsToUnix(FilenameUtils.normalize(path));
    }

    /**
     * Get the path to the .jar file that can be run.
     *
     * <p>This method may block of the .jar has to be rebuilt. The returned path
     * may point to the original .jar if no changes were necessary.</p>
     *
     * @return the path to the .jar file
     * @throws IOException on I/O error
     */
    public File getExecutedPath() throws IOException {
        if (getIncluded().size() == 0) {
            return getSource();
        }

        if (isDirty() || !getTarget().exists()) {
            rebuild();
        }

        return getTarget();
    }

}
