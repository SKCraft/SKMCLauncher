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

package com.sk89q.skmcl.util;

import com.sk89q.skmcl.swing.ProgressDialog;
import com.sk89q.skmcl.swing.SwingHelper;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.sk89q.skmcl.util.SharedLocale._;

/**
 * A background task executor that will show a progress dialog as needed.
 */
public final class Worker extends Observable implements Watchable, Observer {

    private static final int DIALOG_DISPLAY_DELAY = 300;
    private static final Timer timer = new Timer();

    private final Worker self = this;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private final List<Task<?>> inProgress = new ArrayList<Task<?>>();
    private final Window parent;
    private final Object dialogLock = new Object();
    private ProgressDialog dialog;
    private TimerTask dialogShowTask;

    public Worker(Window parent) {
        this.parent = parent;
    }

    public void execute(Runnable command) {
        executor.execute(command);
    }

    private void track(Task<?> task) {
        synchronized (inProgress) {
            inProgress.add(task);
            setDialogVisiblity(!inProgress.isEmpty());
            notifyObservers();
        }

        task.addObserver(this);
    }

    private void stopTracking(Task<?> task) {
        synchronized (inProgress) {
            inProgress.remove(task);
            setDialogVisiblity(!inProgress.isEmpty());
            notifyObservers();
        }
    }

    private void showError(Throwable t) {
        SwingHelper.showError(parent,
                t.getLocalizedMessage(),
                _("errorDialog.title"), t);
    }

    private void createDialog() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                synchronized (dialogLock) {
                    dialogShowTask = null;
                    dialog = new ProgressDialog(parent, self);
                }

                dialog.setVisible(true);
            }
        });
    }

    private void setDialogVisiblity(boolean visible) {
        synchronized (dialogLock) {
            if (visible) {
                if (dialog == null && dialogShowTask == null) {
                    dialogShowTask = new TimerTask() {
                        @Override
                        public void run() {
                            createDialog();
                        }
                    };

                    timer.schedule(dialogShowTask, DIALOG_DISPLAY_DELAY);
                }
            } else {
                if (dialog != null) {
                    dialog.dispose();
                    dialog = null;
                }

                if (dialogShowTask != null) {
                    dialogShowTask.cancel();
                }
            }
        }
    }

    /**
     * Submit a task to be run.
     *
     * @param task the task
     * @param <T> the return value
     * @return a future
     */
    public synchronized <T> Future<T> submit(final Task<T> task) {
        synchronized (inProgress) {
            track(task);
        }

        return executor.submit(new Callable<T>() {
            @Override
            public T call() throws Exception {
                try {
                    return task.call();
                } catch (Exception e) {
                    showError(e);
                    throw e;
                } catch (Throwable t) {
                    showError(t);
                    throw new RuntimeException(t);
                } finally {
                    stopTracking(task);
                }
            }
        });
    }

    /**
     * Attempt to cancel all tasks.
     *
     * <p>New tasks can still be queued after this method is called.</p>
     */
    public synchronized void cancelAll() {
        executor.shutdownNow();
        executor = Executors.newCachedThreadPool();
    }

    @Override
    public double getProgress() {
        synchronized (inProgress) {
            if (inProgress.isEmpty()) {
                return -1;
            } else {
                double progress = 0;

                for (Watchable w : inProgress) {
                    double p = w.getProgress();
                    if (p < 0) {
                        return -1;
                    }

                    progress += p / 100.0 / inProgress.size();
                }

                return progress;
            }
        }
    }

    @Override
    public String getLocalizedTitle() {
        synchronized (inProgress) {
            if (inProgress.isEmpty()) {
                return null;
            } else {
                return inProgress.get(0).getLocalizedTitle();
            }
        }
    }

    @Override
    public String getLocalizedStatus() {
        synchronized (inProgress) {
            if (inProgress.isEmpty()) {
                return null;
            } else {
                return inProgress.get(0).getLocalizedStatus();
            }
        }
    }

    @Override
    public boolean shouldConfirmInterrupt() {
        synchronized (inProgress) {
            if (inProgress.isEmpty()) {
                return false;
            } else {
                for (Watchable w : inProgress) {
                    if (w.shouldConfirmInterrupt()) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        notifyObservers();
    }
}
