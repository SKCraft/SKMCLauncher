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

import com.sk89q.mclauncher.util.LauncherUtils;
import org.apache.commons.codec.digest.DigestUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads multiple files from HTTP URLs.
 */
public class HttpDownloader extends
        SwingWorker<List<HttpDownloader.RemoteFile>, ProgressEvent> {

    private static final Logger logger = LauncherUtils.getLogger(HttpDownloader.class);

    private final List<RemoteFile> queue = new ArrayList<RemoteFile>();
    private boolean overwriteExisting = false;

    /**
     * Return whether existing files are overwritten (and re-downloaded).
     *
     * @return true to overwrite existing
     */
    public boolean getOverwriteExisting() {
        return overwriteExisting;
    }

    /**
     * Set whether existing files should be overwritten (and re-downloaded).
     *
     * @param overwriteExisting true to overwrite existing
     */
    public void setOverwriteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
    }

    /**
     * Submit a file to be downloaded.
     *
     * @param baseDir the base directory to store downloaded files
     * @param url the URL to download from
     * @return the destination file
     */
    public File submit(File baseDir, URL url) {
        String id = DigestUtils.shaHex(url.toString());
        File file = new File(baseDir, id);
        queue.add(new RemoteFile(file, url));
        return file;
    }

    @Override
    protected List<RemoteFile> doInBackground() throws Exception {
        for (RemoteFile file : queue) {
            download(file);
        }

        return queue;
    }

    private void download(RemoteFile remoteFile) throws IOException, InterruptedException {
        File file = remoteFile.getDestination();

        if (!getOverwriteExisting() && file.exists()) {
            logger.log(Level.INFO, "Skipping {0} because it is already downloaded",
                    remoteFile);
        } else {
            logger.log(Level.INFO, "Downloading {0}...", remoteFile);

            File parentFile = file.getParentFile();
            parentFile.mkdirs();
            File tempFile = new File(parentFile, file.getName() + ".tmpdownload");
            tempFile.delete();

            HttpRequest
                    .get(remoteFile.getUrl())
                    .execute()
                    .expectResponseCode(200)
                    .saveContent(tempFile);

            file.delete();
            if (!tempFile.renameTo(file)) {
                throw new IOException(
                        String.format("Failed to rename %s to %s", tempFile, file));
            }
        }
    }

    /**
     * A file that has been queued with a given URL to download from and a destination
     * path to save the downloaded file to.
     */
    public static class RemoteFile {
        private final File destination;
        private final URL url;

        private RemoteFile(File destination, URL url) {
            this.destination = destination;
            this.url = url;
        }

        /**
         * Get the destination file.
         *
         * @return the destination
         */
        public File getDestination() {
            return destination;
        }

        /**
         * Get the URL to download from.
         *
         * @return the URL
         */
        public URL getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return "RemoteFile{" +
                    "destination=" + destination +
                    ", url=" + url +
                    '}';
        }
    }
}
