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

package com.sk89q.skmcl.install;

import com.sk89q.skmcl.util.Environment;
import com.sk89q.mclauncher.util.LauncherUtils;
import com.sk89q.skmcl.util.HttpDownloader;
import com.sk89q.skmcl.util.ProgressEvent;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages an installation procedure.
 */
public class InstallerRuntime extends SwingWorker<InstallerRuntime, ProgressEvent> {

    private static final Logger logger = LauncherUtils.getLogger(InstallerRuntime.class);

    private final Environment environment;
    private final InstallLog previousLog;
    private final InstallLog newLog = new InstallLog();
    private final HttpDownloader httpDownloader = new HttpDownloader();
    private final List<Runnable> tasks = new ArrayList<Runnable>();

    private File tempDir;

    /**
     * Create a new installer runtime.
     *
     * @param environment the environment to install for
     */
    public InstallerRuntime(Environment environment) {
        this(environment, new InstallLog());
    }

    /**
     * Create a new installer runtime.
     *
     * @param environment the environment to install for
     * @param previousLog the existing install log
     */
    public InstallerRuntime(Environment environment, InstallLog previousLog) {
        this.environment = environment;
        this.previousLog = previousLog;
    }

    /**
     * Get the environment.
     *
     * @return the environment
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Get the log for installation changes.
     *
     * @return the log
     */
    public InstallLog getLog() {
        return previousLog;
    }

    /**
     * Get the directory to store temporary files.
     *
     * @return the directory
     */
    public File getTemporaryDir() {
        return tempDir;
    }

    /**
     * Set the directory to store temporary files.
     *
     * @param tempDir the directory
     */
    public void setTemporaryDir(File tempDir) {
        this.tempDir = tempDir;
    }

    /**
     * Copy the file from the given resource to the path.
     *
     * @param source the resource
     * @param file the destination
     * @return the copy object
     */
    public FileCopy copyTo(Resource source, File file) {
        FileCopy copy = new FileCopy(this, source, file);
        source.setInstaller(this);
        tasks.add(copy);
        return copy;
    }

    /**
     * Fetch the given URL and return the file where the contents will be put.
     *
     * @param url the url
     * @return the file where the contents will be put
     */
    File fetch(URL url) {
        if (url.getProtocol().toLowerCase().matches("^https?")) {
            return httpDownloader.submit(getTemporaryDir(), url);
        } else {
            throw new IllegalArgumentException("Not sure how to download " + url);
        }
    }

    @Override
    protected InstallerRuntime doInBackground() throws Exception {
        httpDownloader.run();
        httpDownloader.get();

        for (Runnable task : tasks) {
            logger.log(Level.INFO, "Executing {0}...", task.toString());
            task.run();
        }

        return this;
    }

}
