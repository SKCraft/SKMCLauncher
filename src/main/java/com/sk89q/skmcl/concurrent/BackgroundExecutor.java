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

package com.sk89q.skmcl.concurrent;

import lombok.NonNull;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * A convenient executor for {@link Worker}s.
 */
@Log
public class BackgroundExecutor extends AbstractExecutor {

    private final List<Task<?>> inProgress = new ArrayList<Task<?>>();

    public BackgroundExecutor(@NonNull ExecutorService executor) {
        super(executor);
    }

    public BackgroundExecutor() {
    }

    /**
     * Returns the total number of tasks that are currently running.
     *
     * @return the task count
     */
    public synchronized int getActiveTaskCount() {
        return inProgress.size();
    }

    @Override
    protected void addTask(Task<?> task) {
        synchronized (this) {
            inProgress.add(task);
            setChanged();
            notifyObservers();
        }

        task.addObserver(this);
    }

    @Override
    protected void removeTask(Task<?> task) {
        synchronized (this) {
            inProgress.remove(task);
            setChanged();
            notifyObservers();
        }
    }

    /**
     * Submit a worker to be executed and tracked by this class.
     *
     * @param worker the worker
     * @param <V>    the return value type
     * @return a task
     */
    @Override
    public synchronized <V> Task<V> submit(Worker<V> worker) {
        return super.submit(worker);
    }

    /**
     * Interrupt all currently running tasks.
     * <p/>
     * <p>This <code>BackgroundExecutor</code> can be re-used even after this
     * method is called.</p>
     */
    public synchronized void cancelAll() {
        for (Task<?> task : inProgress) {
            task.cancel(true);
        }

        inProgress.clear();
        setChanged();
        notifyObservers();
    }

    @Override
    public synchronized double getProgress() {
        if (inProgress.isEmpty()) {
            return -1;
        } else {
            double progress = 0;

            for (ProgressObservable w : inProgress) {
                double p = w.getProgress();
                if (p < 0) {
                    return -1;
                }
                progress += p / inProgress.size();
            }

            return progress;
        }
    }

    @Override
    public synchronized String getLocalizedTitle() {
        if (inProgress.isEmpty()) {
            return null;
        } else {
            return inProgress.get(0).getLocalizedTitle();
        }
    }

    @Override
    public synchronized String getLocalizedStatus() {
        if (inProgress.isEmpty()) {
            return null;
        } else {
            return inProgress.get(0).getLocalizedStatus();
        }
    }

    @Override
    public synchronized boolean shouldConfirmInterrupt() {
        for (ProgressObservable w : inProgress) {
            if (w.shouldConfirmInterrupt()) {
                return true;
            }
        }

        return false;
    }

}
