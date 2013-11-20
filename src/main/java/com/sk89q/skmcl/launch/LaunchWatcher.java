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

package com.sk89q.skmcl.launch;

import com.sk89q.skmcl.Launcher;
import com.sk89q.skmcl.swing.MessageLog;
import com.sk89q.skmcl.swing.ProcessConsoleFrame;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.java.Log;

import javax.swing.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * An instance LaunchWatcher can be created and run in its own thread to manage the
 * running of an application, from start to finish.
 *
 * <p>A LauncherWatcher can provide a Swing window in order for the user to
 * view the application's stdout/stderr output, as well as force close the application
 * on command.</p>
 */
@Log
public class LaunchWatcher implements Runnable {

    private static final int CONSOLE_NUM_LINES = 10000;

    private final Launcher launcher;
    private final Future<LaunchedProcess> processFuture;
    private ProcessConsoleFrame consoleFrame;

    @Getter @Setter
    private boolean consoleEnabled = true;
    @Getter @Setter
    private boolean exitingOnLaunch = false;

    /**
     * Construct a new watcher.
     *
     * @param launcher the launcher instance
     * @param processFuture the future that will provide a {@link LaunchedProcess}
     */
    public LaunchWatcher(@NonNull Launcher launcher,
                         @NonNull Future<LaunchedProcess> processFuture) {
        this.launcher = launcher;
        this.processFuture = processFuture;
    }

    @Override
    public void run() {
        LaunchedProcess process;
        final Process p;

        log.info("Getting process...");

        try {
            process = processFuture.get();
        } catch (InterruptedException e) {
            // Abort, but we have an orphaned process
            return;
        } catch (ExecutionException e) {
            // Something went wrong with the launch
            return;
        }

        p = process.getProcess();

        // Dispose of the main launcher frame
        launcher.hideLauncher();

        log.info("Launcher hidden.");

        if (isExitingOnLaunch()) {
            log.info("exitingOnLaunch = true");
            System.exit(0);
            return;
        }

        // Show the console if it's enabled
        if (isConsoleEnabled()) {
            log.info("consoleEnabled = true");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    consoleFrame = new ProcessConsoleFrame(CONSOLE_NUM_LINES, true);
                    consoleFrame.setProcess(p);
                    consoleFrame.setVisible(true);
                    MessageLog messageLog = consoleFrame.getMessageLog();
                    messageLog.consume(p.getInputStream());
                    messageLog.consume(p.getErrorStream());
                }
            });
        }

        log.info("Waiting for process to end...");

        // Wait for the process to end
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            // Orphan process
        }

        log.info("Process ended, re-showing launcher...");

        // Restore the launcher
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                launcher.showLauncher();

                if (consoleFrame != null) {
                    consoleFrame.setProcess(null);
                    consoleFrame.requestFocus();
                }
            }
        });
    }
}
