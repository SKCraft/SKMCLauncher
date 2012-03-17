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

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

public abstract class Task implements Runnable {
    
    private Component component;
    private EventListenerList listenerList = new EventListenerList();
    private double subprogressOffset = 0;
    private double subprogressSize = 1;
    
    public String getInitialTitle() {
        return "Working...";
    }
    
    public Component getComponent() {
        return component;
    }
    
    public void setComponent(Component component) {
        this.component = component;
    }
    
    public ProgressListener[] getProgressListenerList() {
        return listenerList.getListeners(ProgressListener.class);
    }
    
    public void addProgressListener(ProgressListener l) {
        listenerList.add(ProgressListener.class, l);
    }

    public void removeProgressListener(ProgressListener l) {
        listenerList.remove(ProgressListener.class, l);
    }
    
    @Override
    public void run() {
        try {
            execute();
        } catch (CancelledExecutionException e) {
        } catch (final ExecutionException e) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(component,
                                e.getMessage(),
                                "Error occurred", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (InterruptedException e1) {
            } catch (InvocationTargetException e1) {
            }
        }
        
        fireComplete();
    }
    
    protected abstract void execute() throws ExecutionException;
    
    protected void setSubprogress(double offset, double size) {
        this.subprogressOffset = offset;
        this.subprogressSize = size;
    }
    
    protected void fireTitleChange(String message) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((ProgressListener) listeners[i + 1]).titleChanged(
                    new TitleChangeEvent(this, message));
        }
    }
    
    protected void fireStatusChange(String message) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((ProgressListener) listeners[i + 1]).statusChanged(
                    new StatusChangeEvent(this, message));
        }
    }
    
    protected void fireValueChange(double value) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((ProgressListener) listeners[i + 1]).valueChanged(
                    new ValueChangeEvent(this, value));
        }
    }
    
    protected void fireAdjustedValueChange(double value) {
        fireValueChange(value * subprogressSize + subprogressOffset);
    }
    
    protected void fireComplete() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((ProgressListener) listeners[i + 1]).completed(
                    new EventObject(this));
        }
    }
    
    public static TaskWorker startWorker(Window frame, Task task) {
        ProgressDialog dialog = new ProgressDialog(frame, task, task.getInitialTitle());
        task.setComponent(dialog);
        task.addProgressListener(dialog);
        TaskWorker thread = new TaskWorker(task);
        thread.start();
        dialog.setVisible(true);
        return thread;
    }

    public abstract Boolean cancel();
    
    protected static class ExecutionException extends Exception {
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

    protected static class CancelledExecutionException extends ExecutionException {
        private static final long serialVersionUID = 6214758720774734786L;
    }
    
}
