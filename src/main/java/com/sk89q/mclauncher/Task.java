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

package com.sk89q.mclauncher;

import java.awt.Component;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import com.sk89q.mclauncher.util.SwingHelper;

/**
 * Used to run a task in a different thread apart from a GUI. The class has
 * some useful methods for these kind of activities and can show error
 * dialogs thrown from exceptions.
 * 
 * @author sk89q
 */
public abstract class Task implements Runnable {

    private static final Logger logger = Logger.getLogger(Task.class
            .getCanonicalName());
    
    private Component component;
    private EventListenerList listenerList = new EventListenerList();
    private double subprogressOffset = 0;
    private double subprogressSize = 1;
    
    /**
     * Get the initial dialog of the dialog that will be shown to
     * indicate that progress is occurring.
     * 
     * @return title
     */
    public String getInitialTitle() {
        return "Working...";
    }
    
    /**
     * Gets the component that is the progress dialog. This can be used to
     * show dialogs from.
     * 
     * @return component
     */
    public Component getComponent() {
        return component;
    }
    
    /**
     * Used to set the component.
     * 
     * @param component component
     */
    private void setComponent(Component component) {
        this.component = component;
    }
    
    /**
     * Get the list of progress listeners.
     * 
     * @return list
     */
    public ProgressListener[] getProgressListenerList() {
        return listenerList.getListeners(ProgressListener.class);
    }
    
    /**
     * Add a progress listener.
     * 
     * @param l listener
     */
    public void addProgressListener(ProgressListener l) {
        listenerList.add(ProgressListener.class, l);
    }

    /**
     * Remove a progress listener.
     * 
     * @param l listener
     */
    public void removeProgressListener(ProgressListener l) {
        listenerList.remove(ProgressListener.class, l);
    }
    
    /**
     * Execute the task.
     */
    @Override
    public final void run() {
        try {
            execute();
        } catch (CancelledExecutionException e) {
        } catch (final ExecutionException e) {
            // This can be thrown and they will show an error dialog -- it
            // won't be counted as an unhandled error
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        SwingHelper.showError(component,
                                "Error occurred",
                                e.getMessage());
                    }
                });
            } catch (InterruptedException e1) {
            } catch (InvocationTargetException e1) {
            }
        } catch (Throwable t) {
            Launcher.showConsole();
            logger.log(Level.SEVERE, "Unexpected error occurred (please report this error in full)", t);
            SwingHelper.showError(component,
                    "Error occurred", 
                    "An unexpected error has occurred. Please report the error shown in the newly-shown console.");
        }
        
        fireComplete();
    }

    /**
     * Really Execute the task.
     * 
     * @throws CancelledExecutionException
     *             thrown if the activity has been cancelled
     * @throws ExecutionException
     *             an error has occurred and a message needs to be displayed
     */
    protected abstract void execute() throws ExecutionException;
    
    /**
     * Called to cancel the task.
     * 
     * @return true if the cancel was successful, false to deny the cancellation
     *         attempt, or null to show a "cancellation in progress" display
     */
    public abstract Boolean cancel();
    
    /**
     * Set the range of the progress bar (from 0 to 1) to use for all
     * {@link #fireAdjustedValueChange(double)} calls.
     * 
     * @param offset offset, from 0 to 1
     * @param size size of the part, from 0 to 1
     */
    protected void setSubprogress(double offset, double size) {
        this.subprogressOffset = offset;
        this.subprogressSize = size;
    }
    
    /**
     * Fire a title change event. This changes the dialog title.
     * 
     * @param message the title
     */
    protected void fireTitleChange(String message) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((ProgressListener) listeners[i + 1]).titleChanged(
                    new TitleChangeEvent(this, message));
        }
    }
    
    /**
     * Fire a status message change event.
     * 
     * @param message message
     */
    protected void fireStatusChange(String message) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((ProgressListener) listeners[i + 1]).statusChanged(
                    new StatusChangeEvent(this, message));
        }
    }
    
    /**
     * Fires a value change event.
     * 
     * @param value value between 0 or 1, or -1 for an infinite progress bar
     */
    protected void fireValueChange(double value) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((ProgressListener) listeners[i + 1]).valueChanged(
                    new ValueChangeEvent(this, value));
        }
    }
    
    /**
     * Fires a value that is fitted between the range set by
     * {@link #setSubprogress(double, double)}.
     * 
     * @param value value from 0 to 1
     */
    protected void fireAdjustedValueChange(double value) {
        fireValueChange(value * subprogressSize + subprogressOffset);
    }
    
    /**
     * Fire that the task has been completed.
     */
    private void fireComplete() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((ProgressListener) listeners[i + 1]).completed(
                    new EventObject(this));
        }
    }
    
    /**
     * Utility method to start a task worker.
     * 
     * @param frame source frame
     * @param task task to perform
     * @return an instance of the {@link TaskWorker}
     */
    public static TaskWorker startWorker(Window frame, Task task) {
        ProgressDialog dialog = new ProgressDialog(frame, task, task.getInitialTitle());
        task.setComponent(dialog);
        task.addProgressListener(dialog);
        TaskWorker thread = new TaskWorker(task);
        thread.start();
        dialog.setVisible(true);
        return thread;
    }
    
    /**
     * Thrown on an error.
     */
    public static class ExecutionException extends Exception {
        private static final long serialVersionUID = 7477571317146886480L;
        
        public ExecutionException() {
        }
        
        public ExecutionException(String message) {
            super(message);
        }
        
        public ExecutionException(String message, Throwable t) {
            super(message, t);
        }
    }

    /**
     * Thrown to indicate a cancel that is propagating.
     */
    public static class CancelledExecutionException extends ExecutionException {
        private static final long serialVersionUID = 6214758720774734786L;
    }
    
}
