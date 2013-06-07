package com.sk89q.mclauncher.util;

import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.EventObject;

import javax.swing.event.EventListenerList;

import com.sk89q.mclauncher.DownloadListener;
import com.sk89q.mclauncher.DownloadProgressEvent;

public abstract class AbstractDownloader implements Downloader {

    private int timeout = 300000;
    private OutputStream output;
    private URL url;
    private volatile boolean running = true;
    private String etagCheck = null;
    private MessageDigest digest;
    private EventListenerList listenerList = new EventListenerList();

    /**
     * Construct the downloader instance.
     * 
     * @param url url to download
     * @param output output stream to write to
     */
    public AbstractDownloader(URL url, OutputStream output) {
        this.url = url;
        this.output = output;
    }

    @Override
    public void addDownloadListener(DownloadListener l) {
        listenerList.add(DownloadListener.class, l);
    }

    @Override
    public void removeDownloadListener(DownloadListener l) {
        listenerList.remove(DownloadListener.class, l);
    }

    /**
     * Fire a connection start event.
     */
    protected void fireConnectionStarted() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((DownloadListener) listeners[i + 1]).connectionStarted(new EventObject(this));
        }
    }

    /**
     * Fire a length known event.
     */
    protected void fireLengthKnown() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((DownloadListener) listeners[i + 1]).lengthKnown(new EventObject(this));
        }
    }

    /**
     * Fire a download progress event.
     * 
     * @param downloaded number of bytes downloaded so far
     */
    protected void fireDownloadProgress(long downloaded) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((DownloadListener) listeners[i + 1]).downloadProgress(new DownloadProgressEvent(this, downloaded));
        }
    }

    /**
     * Fire a download completed event.
     */
    protected void fireDownloadCompleted() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((DownloadListener) listeners[i + 1]).downloadCompleted(new EventObject(this));
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public OutputStream getOutput() {
        return output;
    }

    public void setOutput(OutputStream output) {
        this.output = output;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public MessageDigest getDigest() {
        return digest;
    }

    public void setDigest(MessageDigest digest) {
        this.digest = digest;
    }

    public EventListenerList getListenerList() {
        return listenerList;
    }

    public void setListenerList(EventListenerList listenerList) {
        this.listenerList = listenerList;
    }

    public String getEtagCheck() {
        return etagCheck;
    }

    @Override
    public void setEtagCheck(String etag) {
        etagCheck = etag;
    }

    @Override
    public void setMessageDigest(MessageDigest digest) {
        this.digest = digest;
    }

    @Override
    public void cancel() {
        running = false;
    }
    
    /**
     * Used to fire periodical progress events.
     */
    protected class ProgressEventUpdater implements Runnable {
        private volatile boolean running = true;
        
        @Override
        public void run() {
            while (running) {
                fireDownloadProgress(getDownloadedLength());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        
        public void stop() {
            running = false;
        }
    }

}