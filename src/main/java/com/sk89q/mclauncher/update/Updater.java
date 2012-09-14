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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.EventListenerList;

import com.sk89q.mclauncher.DownloadListener;
import com.sk89q.mclauncher.DownloadProgressEvent;
import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.ProgressListener;
import com.sk89q.mclauncher.StatusChangeEvent;
import com.sk89q.mclauncher.TitleChangeEvent;
import com.sk89q.mclauncher.ValueChangeEvent;
import com.sk89q.mclauncher.security.X509KeyRing.Ring;
import com.sk89q.mclauncher.update.PackageFile.MessageDigestAlgorithm;
import com.sk89q.mclauncher.util.Downloader;
import com.sk89q.mclauncher.util.SocketDownloader;
import com.sk89q.mclauncher.util.URLConnectionDownloader;
import com.sk89q.mclauncher.util.Util;

/**
 * Downloads and applies an update.
 * 
 * @author sk89q
 */
public class Updater implements DownloadListener {
    
    private static final Logger logger = Logger.getLogger(Updater.class.getCanonicalName());

    private InputStream packageStream;
    private File rootDir;
    private UpdateCache cache;
    private int downloadTries = 5;
    private long retryDelay = 5000;
    private boolean forced = false;
    private Map<String, String> parameters = new HashMap<String, String>();
    
    private EventListenerList listenerList = new EventListenerList();
    private double subprogressOffset = 0;
    private double subprogressSize = 1;
    private volatile boolean running = true;
    private Downloader downloader;
    private List<PackageFile> fileList;
    private int currentIndex = 0;
    private long totalEstimatedSize = 0;
    private long downloadedEstimatedSize = 0;
    
    /**
     * Construct the updater.
     * 
     * @param packageStream
     * @param rootDir
     * @param cache update cache
     */
    public Updater(InputStream packageStream, File rootDir, UpdateCache cache) {
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
     * Tries to load the MessageDigest instance for the given algorithm.
     * 
     * @param type type algorithm
     * @return the message digest
     * @throws NoSuchAlgorithmException no algorithm is registered
     */
    private MessageDigest loadMessageDigest(MessageDigestAlgorithm type)
            throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(type.getJavaDigestName());
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
            PackageDefinition def = PackageDefinition.parse(rootDir, packageStream);
            fileList = def.getFileList();
            totalEstimatedSize = def.getEstimatedTotalSize();
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Failed to read package file", e);
            throw new UpdateException("Could not read package.xml file. The update cannot continue.\n\nThe error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Download the files.
     * 
     * @throws UpdateException on download error
     */
    private void downloadFiles() throws UpdateException {
        currentIndex = 0;
        
        for (PackageFile file : fileList) {
            checkRunning();
            fireDownloadStatusChange("Connecting...");

            OutputStream out;
            boolean isVerifying = false;
            boolean firstTry = true;
            MessageDigest m = null;
            URL url = parameterizeURL(file.getURL());
            String cacheId = getRelative(rootDir, file.getFile());
            
            // Load the MessageDigest
            if (!forced && file.getVerifyType() != null) {
                isVerifying = true;
                try {
                    m = loadMessageDigest(file.getVerifyType());
                } catch (NoSuchAlgorithmException e) {
                    isVerifying = false;
                    m = null;
                    // Guess we're not going to verify files
                }
            }

            // Create the folder
            file.getTempFile().getParentFile().mkdirs();

            int retryNum = 0;
            for (int trial = downloadTries; trial >= -1; trial--) {
                checkRunning();
                
                try {
                    out = new BufferedOutputStream(new FileOutputStream(file.getTempFile()));
                } catch (IOException e) {
                    throw new UpdateException("Could not write to " +
                            file.getTempFile().getAbsolutePath() + ".", e);
                }

                // Attempt downloading
                try {
                    if (url.getProtocol().equalsIgnoreCase("http") && firstTry) {
                        logger.info("Using SocketDownloader for URL " + url.toString());
                        downloader = new SocketDownloader(url, out);
                    } else {
                        logger.info("Using URLConnectionDownloader for URL " + url.toString());
                        downloader = new URLConnectionDownloader(url, out);
                    }
                    
                    firstTry = false;
                    
                    if (isVerifying) {
                        downloader.setMessageDigest(m);
                        downloader.setEtagCheck(cache.getCachedHash(cacheId));
                    }
                    downloader.addDownloadListener(this);
                    
                    if (downloader.download()) {
                        checkRunning();
                        
                        // Check MD5 hash
                        if (isVerifying) {
                            String signature = new BigInteger(1, m.digest()).toString(16);
                            if (!matchesDigest(downloader.getEtag(), signature)) {
                                throw new UpdateException(
                                        String.format("Signature for %s did not match; expected %s, got %s",
                                                file.getURL(), downloader.getEtag(), signature));
                            }
                            
                            cache.putCachedHash(cacheId, signature);
                        }
                    } else { // File already downloaded
                        file.setIgnored(true);
                        
                        fireDownloadStatusChange("Already up-to-date.");
                        fireAdjustedValueChange((downloadedEstimatedSize + file.getTotalEstimatedSize())
                                / (double) totalEstimatedSize);
                    }
                    
                    break;
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to fetch " + url, e);
                    
                    if (trial == -1) {
                        throw new UpdateException("Could not download " + file.getURL() + ": " +
                                e.getMessage(), e);
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
            downloadedEstimatedSize += file.getTotalEstimatedSize();
        }
    }
    
    /**
     * Verify newly-downloaded updates.
     * 
     * @throws UpdateException 
     */
    private void verify() throws UpdateException {
        currentIndex = 0;
        
        SignatureVerifier signatureVerifier = new SignatureVerifier(
                Launcher.getInstance().getKeyRing().getKeyStore(Ring.UPDATE));
        
        for (PackageFile file : fileList) {
            checkRunning();
            
            if (file.isIgnored()) {
                continue;
            }
            
            fireAdjustedValueChange(currentIndex / fileList.size());
            fireStatusChange(String.format("Verifying %s (%d/%d)...", file.getFile().getName(),
                    currentIndex + 1, fileList.size()));
            
            try {
                file.verify(signatureVerifier);
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
    
    /**
     * Deploy newly-downloaded updates.
     * 
     * @throws UpdateException 
     */
    private void deploy(UninstallLog log) throws UpdateException {
        currentIndex = 0;
        
        for (PackageFile file : fileList) {
            checkRunning();
            
            if (file.isIgnored()) {
                continue;
            }
            
            fireAdjustedValueChange(currentIndex / fileList.size());
            fireStatusChange(String.format("Installing %s (%d/%d)...", file.getFile().getName(),
                    currentIndex + 1, fileList.size()));
            
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
    
    /**
     * Delete old files from the previous installation.
     * 
     * @param oldLog old log
     * @param newLog new log
     * @throws UpdateException update exception
     */
    private void deleteOldFiles(UninstallLog oldLog, UninstallLog newLog) throws UpdateException {
        for (PackageFile file : fileList) {
            checkRunning();
            
            if (file.isIgnored()) {
                newLog.copyGroupFrom(oldLog, file.getFile());
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
     * Perform the update.
     * 
     * @throws UpdateException
     */
    public void performUpdate() throws UpdateException {
        File logFile = new File(rootDir, "uninstall.dat");
        
        fireStatusChange("Parsing package .xml...");
        parsePackageFile();
        
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

            fireStatusChange("Verifying signatures...");
            setSubprogress(0.8, 0.1);
            verify();

            fireStatusChange("Installing...");
            setSubprogress(0.9, 0.1);
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
            for (PackageFile file : fileList) {
                File tempFile = file.getTempFile();
                if (tempFile != null) {
                    tempFile.delete();
                }
            }
        }
    }
    
    /**
     * Fires a status message for the currently downloading file.
     * 
     * @param message message to show
     */
    private void fireDownloadStatusChange(String message) {
        fireStatusChange(String.format("(%d/%d) %s: %s", currentIndex + 1,
                fileList.size(), fileList.get(currentIndex).getFile().getName(), message));
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
        PackageFile download = fileList.get(currentIndex);
        
        // If length is known
        if (total > 0) {
            fireDownloadStatusChange(String.format("Downloaded %,d/%,d KB...",
                    event.getDownloadedLength() / 1024, total / 1024));
            fireAdjustedValueChange((downloadedEstimatedSize / (double) totalEstimatedSize) +
                    (download.getTotalEstimatedSize() / (double) totalEstimatedSize) *
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
        PackageFile download = fileList.get(currentIndex);
        
        fireDownloadStatusChange("Download completed.");
        fireAdjustedValueChange((downloadedEstimatedSize / (double) totalEstimatedSize) +
                (download.getTotalEstimatedSize() / (double) totalEstimatedSize));
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
