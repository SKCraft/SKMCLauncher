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
import lombok.NonNull;
import lombok.extern.java.Log;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

@Log
public final class SwingProgressObserver implements Observer {

    private static final int PROGRESS_INTERVAL = 500;
    private static final Timer timer = new Timer();

    private final Window parent;
    private final WorkerService workerService;
    private boolean dialogRequested = false;
    private Window dialog;

    public SwingProgressObserver(@NonNull Window parent, @NonNull WorkerService workerService) {
        this.parent = parent;
        this.workerService = workerService;
        workerService.addObserver(this);
        checkDialogVisibility();
    }

    private void createDialog() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (dialogRequested) {
                    dialog = new ProgressDialog(parent, workerService);
                    dialog.setVisible(true);
                }
            }
        });
    }

    private synchronized void setVisibility(boolean visible) {
        if (dialogRequested != visible) {
            dialogRequested = visible;

            if (visible) {
                createDialog();
            } else {
                final Window lastDialog = dialog;
                dialog = null;

                if (lastDialog != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            lastDialog.dispose();
                        }
                    });
                }
            }
        }
    }

    private synchronized void checkDialogVisibility() {
        setVisibility(workerService.size() > 0);
    }

    @Override
    public synchronized void update(Observable o, Object arg) {
        checkDialogVisibility();
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
