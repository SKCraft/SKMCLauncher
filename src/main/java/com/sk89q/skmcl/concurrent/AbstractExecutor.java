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

import javax.swing.event.EventListenerList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

@Log
abstract class AbstractExecutor extends Observable implements ProgressObservable, Observer {

    private final ExecutorService executor;
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * Create a new background executor with the given executor.
     *
     * @param executor the executor
     */
    public AbstractExecutor(@NonNull ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Create a new background executor using a default executor.
     */
    public AbstractExecutor() {
        this(Executors.newCachedThreadPool());
    }

    /**
     * Add a given task to the list of tasksin execution
     *
     * @param task the task
     */
    protected abstract void addTask(Task<?> task);

    /**
     * Remove a given task from the list of tasks in execution.
     *
     * @param task the task
     */
    protected abstract void removeTask(Task<?> task);

    /**
     * Submit a worker to be executed and tracked by this class.
     *
     * @param worker the worker
     * @param <V>    the return value type
     * @return a task
     */
    protected <V> Task<V> submit(Worker<V> worker) {
        Task<V> task = new Task<V>(worker);
        WorkerWrapper<V> wrapper = new WorkerWrapper<V>(worker, task);
        addTask(task);
        task.setFuture(executor.submit(wrapper));
        return task;
    }

    /**
     * Add an executor listener to this instance.
     *
     * @param l the listener
     */
    public void addExecutorListener(ExecutorListener l) {
        listenerList.add(ExecutorListener.class, l);
    }

    /**
     * Remove the executor listener from this instance.
     * * @param l the listener
     */
    public void removeExecutorListener(ExecutorListener l) {
        listenerList.remove(ExecutorListener.class, l);
    }

    /**
     * Fire an event for an exception that has occurred in a worker.
     *
     * @param throwable the error
     */
    private void fireException(Throwable throwable) {
        ExceptionEvent event = null;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ExecutorListener.class) {
                if (event == null)
                    event = new ExceptionEvent(this, throwable);
                ((ExecutorListener) listeners[i + 1]).exceptionThrown(event);
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        setChanged();
        notifyObservers();
    }

    /**
     * Manage a worker so that any errors that occur during execution can be reported to
     * the underlying {@link BackgroundExecutor}.
     *
     * @param <V> the return value type
     */
    private class WorkerWrapper<V> implements Callable<V> {
        private final Worker<V> worker;
        private final Task<V> task;

        /**
         * Create a new instance of this wrapper.
         *
         * @param worker the worker
         * @param task   the task
         */
        private WorkerWrapper(Worker<V> worker, Task<V> task) {
            this.worker = worker;
            this.task = task;
        }

        @Override
        public V call() throws Exception {
            try {
                log.log(Level.INFO, "Executing {0}...", worker);
                return worker.call();
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                fireException(e);
                throw e;
            } catch (Throwable t) {
                fireException(t);
                throw new RuntimeException(t);
            } finally {
                removeTask(task);
                log.log(Level.INFO, "{0} is done", worker);
            }
        }

    }
}
