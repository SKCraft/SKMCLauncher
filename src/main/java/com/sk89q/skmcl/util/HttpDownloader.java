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
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.codec.digest.DigestUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads multiple files from HTTP URLs.
 *
 * <ul>
 *     <li>On failure of a download, a defined delay will occur and retries will be
 *     attempted up until the retry limit.</li>
 *     <li>Multiple downloads can occur asynchronously, and all downloads will be
 *     attempted even if all failed.</li>
 *     <li>After all files are downloaded, an exception will be raised for the first
 *     file that failed to download.</li>
 *     <li>As a {@link Callable}, an instance will return a list of {@link Future} for
 *     each file that was downloaded (or attempted).</li>
 * </ul>
 */
public class HttpDownloader extends SwingWorker<List<Future<HttpDownloader.RemoteFile>>, ProgressEvent> {

    private static final Logger logger = LauncherUtils.getLogger(HttpDownloader.class);
    public static final int DEFAULT_THREAD_COUNT = 5;

    private final ExecutorService executor;
    private final List<Future<RemoteFile>> executed = new ArrayList<Future<RemoteFile>>();
    @Getter @Setter
    private boolean overwrite = false;
    @Getter @Setter
    private int retryDelay = 2000;
    @Getter @Setter
    private int tryCount = 3;

    /**
     * Create a new downloader using the given executor.
     *
     * @param executor the executor
     */
    public HttpDownloader(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Create a new downloader using a fixed thread executor with the given number
     * of simultaneous download threads.
     *
     * @param numThreads the number of threads
     */
    public HttpDownloader(int numThreads) {
        this(Executors.newFixedThreadPool(numThreads));
    }

    /**
     * Create a new downloader with a default number of threads, as indicated by
     * {@link #DEFAULT_THREAD_COUNT}.
     */
    public HttpDownloader() {
        this(DEFAULT_THREAD_COUNT);
    }

    /**
     * Submit a file to be downloaded.
     *
     * @param baseDir the base directory to store downloaded files
     * @param url the URL to download from
     * @param versionId a unique ID to identify this URL and version, or null to use URL
     * @return the destination file
     */
    public File submit(File baseDir, URL url, String versionId) {
        String id = DigestUtils.shaHex(versionId != null ? versionId : url.toString());
        String dir = id.substring(0, 1);
        File file = new File(baseDir, dir + "/" + id);
        synchronized (executed) {
            executed.add(executor.submit(new RemoteFile(file, url)));
        }
        return file;
    }

    @Override
    protected List<Future<RemoteFile>> doInBackground() throws Exception {
        executor.shutdown();
        while (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS));

        // Run through all the jobs to see whether any failed
        synchronized (executed) {
            for (Future<?> future : executed) {
                future.get();
            }
        }

        return executed;
    }

    /**
     * A file that has been queued with a given URL to download from and a destination
     * path to save the downloaded file to.
     */
    @ToString
    public class RemoteFile implements Callable<RemoteFile> {
        @Getter
        private final File destination;
        @Getter
        private final URL url;

        private RemoteFile(File destination, URL url) {
            this.destination = destination;
            this.url = url;
        }

        @Override
        public RemoteFile call() throws IOException, InterruptedException {
            File file = getDestination();

            if (!overwrite && file.exists()) {
                logger.log(Level.INFO, "Skipping {0} because it is already downloaded", this);
            } else {
                logger.log(Level.INFO, "Downloading {0}...", this);

                File parentFile = file.getParentFile();
                parentFile.mkdirs();
                File tempFile = new File(parentFile, file.getName() + ".tmpdownload");
                int trial = 0;

                while (true) {
                    tempFile.delete();

                    try {
                        HttpRequest
                                .get(getUrl())
                                .execute()
                                .expectResponseCode(200)
                                .saveContent(tempFile);

                        break;
                    } catch (IOException e) {
                        if (trial >= tryCount) {
                            logger.log(Level.WARNING, "Failed to download " + getUrl(), e);
                            throw e;
                        } else {
                            logger.log(Level.WARNING, "Waiting to retry downloading " + getUrl(), e);
                            Thread.sleep(retryDelay);
                        }
                    }
                }

                file.delete();
                if (!tempFile.renameTo(file)) {
                    throw new IOException(
                            String.format("Failed to rename %s to %s", tempFile, file));
                }
            }

            return this;
        }
    }
}
