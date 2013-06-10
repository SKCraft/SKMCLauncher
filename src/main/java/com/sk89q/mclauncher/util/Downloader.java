package com.sk89q.mclauncher.util;

import java.io.IOException;
import java.security.MessageDigest;

import com.sk89q.mclauncher.event.DownloadListener;

public interface Downloader {

    /**
     * Register listener for download events.
     * 
     * @param l listener
     */
    public abstract void addDownloadListener(DownloadListener l);

    /**
     * Unregister listener for download events.
     * 
     * @param l listener
     */
    public abstract void removeDownloadListener(DownloadListener l);

    /**
     * Get the total length of the file (once connected). This can be -1 if
     * the length is not known.
     * 
     * @return length in bytes, or -1 if not known
     */
    public abstract long getTotalLength();

    /**
     * Get the total downloaded length of the file.
     * 
     * @return length in bytes
     */
    public abstract long getDownloadedLength();

    /**
     * Get the etag reported by the server.
     * 
     * @return etag or null if not known
     */
    public abstract String getEtag();

    /**
     * Use the given etag to check for file modification. If the server
     * returnes a 304 Not Modified, then the download will not take place.
     * 
     * @param etag etag to check, or null to disable
     */
    public abstract void setEtagCheck(String etag);

    /**
     * Set a message digest to calculate a digest with.
     * 
     * @param digest digest, or null to disable
     */
    public abstract void setMessageDigest(MessageDigest digest);

    /**
     * Initiate and complete the download. Events will be fired appropriately
     * during this period.
     * 
     * @return true if the file was downloaded, false if it was cached
     * @throws IOException IO error
     * @throws InterruptedException thrown on interruption
     */
    public abstract boolean download() throws IOException, InterruptedException;

    /**
     * Cancel the download.
     */
    public abstract void cancel();

}