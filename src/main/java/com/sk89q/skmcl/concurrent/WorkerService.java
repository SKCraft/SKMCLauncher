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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;

/**
 * A service that handles the tracking of {@link Worker}s.
 */
public interface WorkerService extends ProgressObservable {

    /**
     * Submit a task for execution.
     *
     * @param task the task
     * @param <V> return value type
     * @return a Future representing the pending return value of the task
     */
    <V> ListenableFuture<V> submit(Worker<V> task);

    /**
     * Submit a task for execution and wrap it in a {@link Worker}.
     *
     * @param task the task
     * @param title the localized title for the task
     * @param status the localized status for the task
     * @param <V> return value type
     * @return a Future representing the pending return value of the task
     */
    <V> ListenableFuture<V> submit(Callable<V> task, String title, String status);

    /**
     * Cancel all tasks.
     */
    void cancelAll();

    /**
     * Get the number of pending tasks.
     *
     * @return the number of pending tasks
     */
    int size();

}
