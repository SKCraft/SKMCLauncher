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

package com.sk89q.mclauncher.update;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.event.EventListenerList;

import com.sk89q.mclauncher.DownloadListener;
import com.sk89q.mclauncher.DownloadProgressEvent;
import com.sk89q.mclauncher.ProgressListener;
import com.sk89q.mclauncher.SelectComponentsDialog;
import com.sk89q.mclauncher.StatusChangeEvent;
import com.sk89q.mclauncher.TitleChangeEvent;
import com.sk89q.mclauncher.ValueChangeEvent;
import com.sk89q.mclauncher.model.Component;
import com.sk89q.mclauncher.model.FileGroup;
import com.sk89q.mclauncher.model.PackageFile;
import com.sk89q.mclauncher.model.PackageManifest;
import com.sk89q.mclauncher.util.Downloader;
import com.sk89q.mclauncher.util.URLConnectionDownloader;
import com.sk89q.mclauncher.util.Util;
import com.sk89q.mclauncher.util.XMLUtil;

/**
 * Downloads and applies an update.
 * 
 * @author sk89q
 */
public class Updater implements DownloadListener {
    
    private static final Logger logger = Logger.getLogger(Updater.class.getCanonicalName());

    private final JFrame frame;
    private final URL baseUrl;
    private final InputStream packageStream;
    private final File rootDir;
    private final UpdateCache cache;
    private final EventListenerList listenerList = new EventListenerList();
    
    private int downloadTries = 5;
    private long retryDelay = 5000;
    private boolean forced = false;
    private Map<String, String> parameters = new HashMap<String, String>();

    private PackageManifest manifest;
    
    private double subprogressOffset = 0;
    private double subprogressSize = 1;
    private volatile boolean running = true;
    private Downloader downloader;
    private int currentIndex = 0;
    private long totalEstimatedSize = 0;
    private long downloadedEstimatedSize = 0;
    private int numFiles;
    
    /**
     * Construct the updater.
     * 
     * @param frame the parent frame
     * @param baseUrl the base URL
     * @param packageStream
     * @param rootDir
     * @param cache update cache
     */
    public Updater(JFrame frame, URL baseUrl, InputStream packageStream,
            File rootDir, UpdateCache cache) {
        this.frame = frame;
        this.baseUrl = baseUrl;
        this.packageStream = packageStream;
        this.rootDir = rootDir;
        this.cache = cache;
    }
    
    /**
     * Get the number of download tries.
     * 
     * @return download try count
     */
    public int getDownloadTries() {
        return downloadTries;
    }

    /**
     * Set the number of download tries
     * 
     * @param downloadTries count
     */
    public void setDownloadTries(int downloadTries) {
        this.downloadTries = downloadTries;
    }

    /**
     * Returns whether everything is being reinstalled (all files are
     * updated).
     * 
     * @return status
     */
    public boolean isReinstalling() {
        return forced;
    }

    /**
     * Set whether all files should be re-downloaded and re-installed.
     * 
     * @param forced true to reinstall
     */
    public void setReinstall(boolean forced) {
        this.forced = forced;
    }

    /**
     * Register a request parameter.
     * 
     * @param param key
     * @param value value
     */
    public void registerParameter(String param, String value) {
        parameters.put(param, value);
    }
    
    /**
     * Replaces parameters within a URL.
     * 
     * @param url url to parameterize
     * @return new URL
     */
    private URL parameterizeURL(URL url) {
        String urlStr = url.toString();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            try {
                urlStr = urlStr.replace("%" + entry.getKey() + "%",
                        URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Get the URL of a file.
     * 
     * @param group the group
     * @param file the file
     * @return the URL
     */
    private URL getURL(FileGroup group, PackageFile file) {
        return group.getURL(baseUrl, file);
    }
    
    /**
     * Returns whether two digests (in hex) match.
     * 
     * @param s1 digest 1
     * @param s2 digest 2
     * @return true for match
     */
    private boolean matchesDigest(String s1, String s2) {
        return s1.replaceAll("^0+", "").equalsIgnoreCase(s2.replaceAll("^0+", ""));
    }
    
    /**
     * Checks to make sure that the process is still running. Otherwise,
     * throw a {@link CancelledUpdateException}.
     * 
     * @throws CancelledUpdateException on cancel
     */
    private void checkRunning() throws CancelledUpdateException {
        if (!running) {
            throw new CancelledUpdateException();
        }
    }
    
    /**
     * Parse the package file.
     * 
     * @throws UpdateException on package parse error
     */
    private void parsePackageFile() throws UpdateException {
        try {
            manifest = XMLUtil.parseJaxb(PackageManifest.class, packageStream);
            manifest.setDestDir(rootDir);
            totalEstimatedSize = manifest.getTotalSize();
            numFiles = manifest.getDownloadCount();
            
            if (!manifest.isSupportedVersion()) {
                throw new UpdateException(
                        "The update package is written in an unsupported version. (Update launcher?)");
            }
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Failed to read package file", e);
            throw new UpdateException(
                    "Could not read package.xml file. " +
                    "The update cannot continue.\n\nThe error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Download the files.
     * 
     * @throws UpdateException on download error
     */
    private void downloadFiles() throws UpdateException {
        currentIndex = 0;
        
        for (FileGroup group : manifest.getFileGroups()) {
            for (PackageFile file : group.getFiles()) {
                checkRunning();
                
                if (!file.matchesEnvironment()) {
                    logger.info("Update: " + getURL(group, file) + " does NOT match environment");
                    continue;
                }
                
                if (!file.matchesFilter(manifest.getComponents())) {
                    logger.info("Update: " + getURL(group, file) + " does NOT match filter");
                    continue;
                }

                logger.info("Update: " + getURL(group, file) + " OK");
                
                downloadFile(group, file);
            }
        }
    }
    
    /**
     * Download the given file.
     * 
     * @param group the group
     * @param file the file
     * @throws UpdateException on download error
     */
    private void downloadFile(FileGroup group, PackageFile file) throws UpdateException {
        fireDownloadStatusChange("Connecting...");
        
        OutputStream out;
        MessageDigest digest = null; // Not null if we are verifying the file hash
        URL url = parameterizeURL(getURL(group, file));
        String cacheId = getCacheId(file);
        
        // We will later have to remove old entries from the cache in order
        // to prevent a number of problems
        cache.touch(cacheId);
        
        // Keep the last version
        String lastVersion = cache.getFileVersion(cacheId);
        
        // Load the MessageDigest
        // 'digest' may be null after this if the algorithm is not supported
        if (!forced) {
            try {
                digest = group.createMessageDigest();
            } catch (NoSuchAlgorithmException e) {
                digest = null; // Guess we're not going to verify files
            }
        }

        // Create the folder
        file.getTempFile().getParentFile().mkdirs();

        // Try to download
        int retryNum = 0;
        for (int trial = downloadTries; trial >= -1; trial--) {
            checkRunning();
            
            // Create temporary file to place data
            try {
                out = new BufferedOutputStream(new FileOutputStream(file.getTempFile()));
            } catch (IOException e) {
                throw new UpdateException("Could not write to " +
                        file.getTempFile().getAbsolutePath() + ".", e);
            }

            // Attempt downloading
            try {
                downloader = new URLConnectionDownloader(url, out);
                downloader.addDownloadListener(this);
                
                boolean shouldDownload = true;
                
                // We have our own per-file versioning mechanism where we compare the
                // given file version (an arbitrary string) with the string we
                // last stored for this file
                if (!forced && file.getVersion() != null) {
                    if (lastVersion != null && lastVersion.equals(file.getVersion())) {
                        shouldDownload = false;
                    }
                } else {
                    // But Mojang has their own digest-based + E-Tag method that
                    // we also support
                    if (digest != null) {
                        downloader.setMessageDigest(digest);
                        downloader.setEtagCheck(cache.getFileVersion(cacheId));
                    }
                }
                
                if (shouldDownload && downloader.download()) {
                    checkRunning();
                    
                    String storedVersion = null;
                    
                    // Check MD5 hash
                    if (digest != null) {
                        String signature = new BigInteger(1, digest.digest()).toString(16);
                        if (!matchesDigest(downloader.getEtag(), signature)) {
                            throw new UpdateException(
                                    String.format("Signature for %s did not match; expected %s, got %s",
                                            getURL(group, file), downloader.getEtag(), signature));
                        }
                        
                        storedVersion = signature;
                    }
                    
                    // Use our own per-file versioning if we have that
                    if (file.getVersion() != null) {
                        storedVersion = file.getVersion();
                    }
                    
                    // OK, if we're not overwriting, then store the last version
                    if (file.getOverwrite() != null) {
                        storedVersion = lastVersion;
                    }
                    
                    // This may clear the version if we don't have a version to store
                    // this time
                    cache.getFileVersion(cacheId, storedVersion);
                } else { // File already downloaded
                    file.setIgnored(true);
                    
                    fireDownloadStatusChange("Already up-to-date.");
                    fireAdjustedValueChange((downloadedEstimatedSize + file.getSize())
                            / (double) totalEstimatedSize);
                }
                
                break;
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to fetch " + url, e);
                
                if (trial == -1) {
                    throw new UpdateException(
                            "Could not download " + getURL(group, file) + ": " + e.getMessage(), e);
                }
            } finally {
                downloader = null;
                Util.close(out);
            }
            
            retryNum++;
            
            Util.sleep(retryDelay);
            fireDownloadStatusChange("Download failed; retrying (" + retryNum + ")...");
        }
        
        currentIndex++;
        downloadedEstimatedSize += file.getSize();
    }
    
    /**
     * Deploy newly-downloaded updates.
     * 
     * @throws UpdateException 
     */
    private void deploy(UninstallLog log) throws UpdateException {
        currentIndex = 0;

        for (FileGroup group : manifest.getFileGroups()) {
            for (PackageFile file : group.getFiles()) {
                checkRunning();
                
                if (!file.matchesEnvironment()) {
                    continue;
                }
                
                if (!file.matchesFilter(manifest.getComponents())) {
                    continue;
                }
                
                if (file.isIgnored()) {
                    continue;
                }
                
                fireAdjustedValueChange(currentIndex / numFiles);
                fireStatusChange(String.format("Installing %s (%d/%d)...", file.getFile().getName(),
                        currentIndex + 1, numFiles));
                
                try {
                    file.deploy(log);
                } catch (SecurityException e) {
                    logger.log(Level.WARNING, "Failed to deploy " + file, e);
                    throw new UpdateException("The digital signature(s) of " +
                            file.getFile().getAbsolutePath() + " could not be verified: " + e.getMessage(), e);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to deploy " + file, e);
                    throw new UpdateException("Could not install to " +
                            file.getFile().getAbsolutePath() + ": " + e.getMessage(), e);
                } catch (Throwable e) {
                    logger.log(Level.WARNING, "Failed to deploy " + file, e);
                    throw new UpdateException("Could not install " +
                            file.getFile().getAbsolutePath() + ": " + e.getMessage(), e);
                }
                
                currentIndex++;
            }
        }
    }
    
    /**
     * Delete old files from the previous installation.
     * 
     * @param oldLog old log
     * @param newLog new log
     * @throws UpdateException update exception
     */
    private void deleteOldFiles(UninstallLog oldLog, UninstallLog newLog) throws UpdateException {
        for (FileGroup group : manifest.getFileGroups()) {
            for (PackageFile file : group.getFiles()) {
                checkRunning();
                
                if (file.isIgnored()) {
                    newLog.copyGroupFrom(oldLog, file.getFile());
                }
            }
        }
        
        for (Entry<String, Set<String>> entry : oldLog.getEntrySet()) {
            for (String path : entry.getValue()) {
                checkRunning();
                
                if (!newLog.has(path)) {
                    new File(rootDir, path).delete();
                }
            }
        }
    }
    
    /**
     * Ask to select components, if necessary.
     */
    public void askComponents() {
        boolean needsDialog = false;
        
        for (Component component : manifest.getComponents()) {
            if (!component.isRequired()) {
                needsDialog = true;
                cache.recallSelection(component);
            }
        }
        
        if (needsDialog) {
            fireStatusChange("Asking for install options...");
            SelectComponentsDialog dialog = new SelectComponentsDialog(frame, manifest);
            dialog.setVisible(true);
        }
        
        for (Component component : manifest.getComponents()) {
            if (!component.isRequired()) {
                cache.storeSelection(component);
            }
        }
    }
    
    /**
     * Perform the update.
     * 
     * @throws UpdateException
     */
    public void performUpdate() throws UpdateException {
        File logFile = new File(rootDir, "uninstall.dat");
        
        fireStatusChange("Parsing package .xml...");
        parsePackageFile();
        
        askComponents();
        
        try {
            fireStatusChange("Downloading files...");
            setSubprogress(0, 0.8);
            downloadFiles();
            
            UninstallLog oldLog = new UninstallLog();
            UninstallLog newLog = new UninstallLog();
            newLog.setBaseDir(rootDir);
            try {
                oldLog.read(logFile);
            } catch (IOException e) {
            }

            fireStatusChange("Installing...");
            setSubprogress(0.8, 0.2);
            deploy(newLog);

            fireStatusChange("Removing old files...");
            deleteOldFiles(oldLog, newLog);
            
            // Save install log
            try {
                newLog.write(logFile);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to write " + logFile, e);
                throw new UpdateException("The uninstall log file could not be written to. " +
                		"The update has been aborted.", e);
            }
        } finally {
            // Cleanup
            fireStatusChange("Cleaning up temporary files...");
            for (FileGroup group : manifest.getFileGroups()) {
                for (PackageFile file : group.getFiles()) {
                    File tempFile = file.getTempFile();
                    if (tempFile != null) {
                        tempFile.delete();
                    }
                }
            }
        }
    }
    
    /**
     * Get the current file.
     * 
     * @return the current file
     */
    private PackageFile getCurrentFile() {
        int index = 0;
        for (FileGroup group : manifest.getFileGroups()) {
            for (PackageFile file : group.getFiles()) {
                if (index == currentIndex) {
                    return file;
                }
                index++;
            }
        }
        return null;
    }
    
    /**
     * Fires a status message for the currently downloading file.
     * 
     * @param message message to show
     */
    private void fireDownloadStatusChange(String message) {
        fireStatusChange(String.format("(%d/%d) %s: %s", currentIndex + 1,
                numFiles, getCurrentFile().getFile().getName(), message));
    }

    /**
     * Called whenever a HTTP download connection is created.
     */
    @Override
    public void connectionStarted(EventObject event) {
        fireDownloadStatusChange("Connected.");
    }

    /**
     * Called with the length is known in an HTTP download.
     */
    @Override
    public void lengthKnown(EventObject event) {
    }

    /**
     * Called when download progress is made.
     */
    @Override
    public void downloadProgress(DownloadProgressEvent event) {
        long total = ((Downloader) event.getSource()).getTotalLength();
        PackageFile download = getCurrentFile();
        
        // If length is known
        if (total > 0) {
            fireDownloadStatusChange(String.format("Downloaded %,d/%,d KB...",
                    event.getDownloadedLength() / 1024, total / 1024));
            fireAdjustedValueChange((downloadedEstimatedSize / (double) totalEstimatedSize) +
                    (download.getSize() / (double) totalEstimatedSize) *
                    (event.getDownloadedLength() / (double) total));
        } else {
            fireDownloadStatusChange(String.format("Downloaded %,d KB...",
                    (event.getDownloadedLength() / 1024)));
        }
    }

    /**
     * Called when a download completes.
     */
    @Override
    public void downloadCompleted(EventObject event) {
        fireDownloadStatusChange("Download completed.");
        fireAdjustedValueChange((downloadedEstimatedSize / (double) totalEstimatedSize) +
                (getCurrentFile().getSize() / (double) totalEstimatedSize));
    }
    
    /**
     * Set a sub-progress range with is used by {@link #fireAdjustedValueChange(double)}.
     * 
     * @param offset offset, between 0 and 1
     * @param size size, between 0 and 1
     */
    protected void setSubprogress(double offset, double size) {
        this.subprogressOffset = offset;
        this.subprogressSize = size;
    }
    
    /**
     * Fire a title change.
     * 
     * @param message title
     */
    protected void fireTitleChange(String message) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((ProgressListener) listeners[i + 1]).titleChanged(
                    new TitleChangeEvent(this, message));
        }
    }
    
    /**
     * Fire a status change.
     * 
     * @param message new status
     */
    protected void fireStatusChange(String message) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((ProgressListener) listeners[i + 1]).statusChanged(
                    new StatusChangeEvent(this, message));
        }
    }
    
    /**
     * Fire a value change.
     * 
     * @param value value between 0 and 1
     */
    protected void fireValueChange(double value) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((ProgressListener) listeners[i + 1]).valueChanged(
                    new ValueChangeEvent(this, value));
        }
    }
    
    /**
     * Fire an adjusted value change, which is adjusted with
     * {@link #setSubprogress(double, double)}.
     * 
     * @param value value between 0 and 1
     */
    protected void fireAdjustedValueChange(double value) {
        fireValueChange(value * subprogressSize + subprogressOffset);
    }
    
    /**
     * Gets the relative path between a base and a path.
     * 
     * @param base base path containing path
     * @param path path
     * @return relative path
     */
    private String getRelative(File base, File path) {
        return base.toURI().relativize(path.toURI()).getPath();
    }
    
    /**
     * Get the ID used to be stored as the key for a file on an {@link UpdateCache}.
     * 
     * @param file the file
     * @return an ID
     */
    private String getCacheId(PackageFile file) {
        return getRelative(rootDir, file.getFile());
    }
    
    /**
     * Registers a progress listener.
     * 
     * @param l listener
     */
    public void addProgressListener(ProgressListener l) {
        listenerList.add(ProgressListener.class, l);
    }

    /**
     * Unregister a progress listener.
     * 
     * @param l listener
     */
    public void removeProgressListener(ProgressListener l) {
        listenerList.remove(ProgressListener.class, l);
    }
    
    /**
     * Cancel the update.
     */
    public void cancel() {
        running = false;
        Downloader downloader = this.downloader;
        if (downloader != null) {
            downloader.cancel();
        }
    }

}
