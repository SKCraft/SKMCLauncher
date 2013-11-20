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

import com.sk89q.skmcl.swing.ProgressDialog;
import com.sk89q.skmcl.swing.SwingHelper;
import lombok.NonNull;
import lombok.extern.java.Log;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.Timer;
import java.util.logging.Level;

import static com.sk89q.skmcl.util.SharedLocale._;

/**
 * An observer for a {@link BackgroundExecutor} to observe progress updates and
 * show a progress window if necessary.
 */
@Log
public final class SwingProgressObserver implements Observer, ExecutorListener {

    private static final int DIALOG_DISPLAY_DELAY = 250;
    private static final int PROGRESS_INTERVAL = 500;
    private static final Timer timer = new Timer();

    private BackgroundExecutor executor;
    private final Window parent;
    private ProgressDialog dialog;
    private TimerTask dialogShowTask;
    private boolean wantDialog = false;

    /**
     * Create a new worker with the parent window given.
     *
     * @param parent the parent window from which to show dialogs from
     */
    public SwingProgressObserver(@NonNull Window parent) {
        this.parent = parent;
    }

    /**
     * Set the current executor to observe.
     *
     * @param executor the executor, or null to stop observing
     */
    public void setExecutor(BackgroundExecutor executor) {
        if (this.executor != null) {
            this.executor.deleteObserver(this);
            this.executor.removeExecutorListener(this);
        }

        this.executor = executor;
        if (executor != null) {
            executor.addObserver(this);
            executor.addExecutorListener(this);
        }

        update(executor, null);
    }

    /**
     * Create the dialog.
     */
    private void createDialog() {
        final SwingProgressObserver self = this;
        final BackgroundExecutor executor = this.executor;

        synchronized (this) {
            if (executor == null) {
                wantDialog = false;
                dialogShowTask = null;
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                synchronized (self) {
                    if (!wantDialog) {
                        return;
                    }

                    dialogShowTask = null;
                    dialog = new ProgressDialog(parent, executor);
                }

                dialog.setVisible(true);
            }
        });
    }

    /**
     * Set whether the dialog should be visible.
     *
     * @param visible true if visible
     */
    private synchronized void setDialogVisibility(boolean visible) {
        wantDialog = visible;

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
                final JDialog d = dialog;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        d.dispose();
                    }
                });
                dialog = null;
            }

            if (dialogShowTask != null) {
                dialogShowTask.cancel();
                dialogShowTask = null;
            }
        }
    }

    @Override
    public synchronized void update(Observable o, Object arg) {
        setDialogVisibility(executor != null && executor.getActiveTaskCount() > 0);
    }

    @Override
    public void exceptionThrown(ExceptionEvent event) {
        final Throwable throwable = event.getThrowable();

        log.log(Level.WARNING,
                "An uncaught exception was thrown in a worker", throwable);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SwingHelper.showErrorDialog(parent,
                        throwable.getLocalizedMessage(),
                        _("errorDialog.title"), throwable);
            }
        });
    }

    /**
     * Schedule a progress updater.
     *
     * @param updater the object to call
     * @return a timer task that can be cancelled
     */
    public static TimerTask updatePeriodically(ProgressUpdater updater) {
        TimerTask timerTask = new ProgressTimerTask(updater);
        timer.schedule(timerTask, PROGRESS_INTERVAL, PROGRESS_INTERVAL);
        return timerTask;
    }

}
