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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class ExecutorWorkerService extends Observable implements WorkerService, Observer {

    private final ListeningExecutorService executor;
    private final List<WorkerFuture> inProgress = new ArrayList<WorkerFuture>();

    public ExecutorWorkerService(@NonNull ListeningExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public double getProgress() {
        synchronized (inProgress) {
            if (inProgress.isEmpty()) {
                return -1;
            }

            double progress = 0;

            for (WorkerFuture wf : inProgress) {
                double p = wf.getWorker().getProgress();

                if (p < 0) {
                    return -1;
                } else {
                    progress += p / inProgress.size();
                }
            }

            return progress;
        }
    }

    @Override
    public String getLocalizedTitle() {
        synchronized (inProgress) {
            if (inProgress.isEmpty()) {
                return null;
            } else {
                // Use the first title
                return inProgress.get(0).getWorker().getLocalizedTitle();
            }
        }
    }

    @Override
    public String getLocalizedStatus() {
        synchronized (inProgress) {
            if (inProgress.isEmpty()) {
                return null;
            } else {
                // Use the first status
                return inProgress.get(0).getWorker().getLocalizedStatus();
            }
        }
    }

    @Override
    public boolean shouldConfirmInterrupt() {
        synchronized (inProgress) {
            for (WorkerFuture wf : inProgress) {
                if (wf.getWorker().shouldConfirmInterrupt()) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        setChanged();
        notifyObservers();
    }

    private void add(WorkerFuture wf) {
        synchronized (inProgress) {
            inProgress.add(wf);
            wf.getWorker().addObserver(this);
        }

        setChanged();
        notifyObservers();
    }

    private void remove(WorkerFuture wf) {
        synchronized (inProgress) {
            inProgress.remove(wf);
            wf.getWorker().deleteObserver(this);
        }

        setChanged();
        notifyObservers();
    }

    @Override
    public <V> ListenableFuture<V> submit(@NonNull Worker<V> task) {
        ListenableFuture<V> future = executor.submit(task);
        final WorkerFuture wf = new WorkerFuture(task, future);

        add(wf);

        future.addListener(new Runnable() {
            @Override
            public void run() {
                remove(wf);
            }
        }, MoreExecutors.sameThreadExecutor());

        return future;
    }

    @Override
    public <V> ListenableFuture<V> submit(Callable<V> task, String title, String status) {
        return submit(makeWorker(task, title, status));
    }

    @Override
    public void cancelAll() {
        synchronized (inProgress) {
            for (WorkerFuture wf : inProgress) {
                wf.getFuture().cancel(true);
            }
        }
    }

    @Override
    public int size() {
        synchronized (inProgress) {
            return inProgress.size();
        }
    }

    private static <V> Worker<V> makeWorker(
            final Callable<V> callable,
            final String localizedTitle,
            final String localizedStatus) {

        return new AbstractWorker<V>() {
            @Override
            public V call() throws Exception {
                return callable.call();
            }

            @Override
            public String getLocalizedTitle() {
                return localizedTitle;
            }

            @Override
            public String getLocalizedStatus() {
                return localizedStatus;
            }
        };
    }

    private static class WorkerFuture {
        @Getter private final Worker<?> worker;
        @Getter private final Future<?> future;

        private WorkerFuture(Worker<?> worker, Future<?> future) {
            this.worker = worker;
            this.future = future;
        }
    }

}
