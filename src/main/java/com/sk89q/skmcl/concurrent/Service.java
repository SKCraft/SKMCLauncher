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

import java.util.concurrent.ExecutorService;

public abstract class Service<V> extends AbstractExecutor {

    private Task<V> task;

    public Service(@NonNull ExecutorService executor) {
        super(executor);
    }

    public Service() {
    }

    /**
     * Attempts to cancel the running worker. If no worker is running, or the current
     * worker was already cancelled, then nothing will happen.
     *
     * @return true if the worker was successfully cancelled
     */
    public synchronized final boolean cancel() {
        if (task != null) {
            return task.cancel(true);
        }

        return true;
    }

    /**
     * Cancel the current worker if one exists and start a new one.
     *
     * @return the created task
     */
    public synchronized final Task<V> start() {
        cancel();
        return task = submit(createWorker());
    }

    /**
     * Invoked to create a new worker.
     *
     * @return a worker
     */
    protected abstract Worker<V> createWorker();

    @Override
    protected void addTask(Task<?> task) {
    }

    @Override
    protected void removeTask(Task<?> task) {
    }

    @Override
    public double getProgress() {
        Task<V> task = this.task;
        return task != null ? task.getProgress() : -1;
    }

    @Override
    public String getLocalizedTitle() {
        Task<V> task = this.task;
        return task != null ? task.getLocalizedTitle() : null;
    }

    @Override
    public String getLocalizedStatus() {
        Task<V> task = this.task;
        return task != null ? task.getLocalizedStatus() : null;
    }

    @Override
    public boolean shouldConfirmInterrupt() {
        Task<V> task = this.task;
        return task != null && task.shouldConfirmInterrupt();
    }
}
