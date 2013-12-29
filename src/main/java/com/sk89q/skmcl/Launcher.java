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

package com.sk89q.skmcl;

import com.google.common.util.concurrent.ListenableFuture;
import com.sk89q.skmcl.concurrent.ExecutorWorkerService;
import com.sk89q.skmcl.launch.LaunchWatcher;
import com.sk89q.skmcl.launch.LaunchWorker;
import com.sk89q.skmcl.launch.LaunchedProcess;
import com.sk89q.skmcl.profile.Profile;
import com.sk89q.skmcl.profile.ProfileManager;
import com.sk89q.skmcl.session.AccountList;
import com.sk89q.skmcl.session.Identity;
import com.sk89q.skmcl.swing.LauncherFrame;
import com.sk89q.skmcl.swing.LoginController;
import com.sk89q.skmcl.swing.SwingHelper;
import com.sk89q.skmcl.util.Persistence;
import com.sk89q.skmcl.util.SharedLocale;
import com.sk89q.skmcl.util.SimpleLogFormatter;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;

import static com.sk89q.skmcl.util.SharedLocale._;

@Log
public class Launcher {

    @Getter private final File baseDir;
    @Getter private final ProfileManager profiles;
    @Getter private final AccountList accounts;
    @Getter private final Configuration configuration;
    private LauncherFrame mainFrame;

    public Launcher(@NonNull File baseDir) {
        this.baseDir = baseDir;
        this.profiles = new ProfileManager(baseDir);
        this.accounts = Persistence.load(new File(baseDir, "accounts.dat"), AccountList.class);
        this.configuration = Persistence.load(new File(baseDir, "config.json"), Configuration.class);

        if (accounts.getSize() > 0) {
            accounts.setSelectedItem(accounts.getElementAt(0));
        }
    }

    public LauncherFrame showLauncher() {
        if (mainFrame == null) {
            mainFrame = new LauncherFrame(this);
            mainFrame.setVisible(true);
        }

        return mainFrame;
    }

    public void hideLauncher() {
        if (mainFrame != null) {
            mainFrame.removeListeners();
            mainFrame.dispose();
            mainFrame = null;
        }
    }

    public void launchApplication(Window owner, ExecutorWorkerService executor, Profile profile) {
        profile.setLastLaunchDate(new Date());
        Persistence.commitAndForget(profile);
        getProfiles().notifyUpdate();

        LoginController loginDialog = new LoginController(owner, this);
        loginDialog.setVisible(true);
        Identity identity = loginDialog.getIdentity();

        if (identity != null) {
            LaunchWorker task = new LaunchWorker(profile, identity);
            ListenableFuture<LaunchedProcess> future = executor.submit(task);
            SwingHelper.addErrorDialogCallback(future, owner);
            LaunchWatcher watcher = new LaunchWatcher(this, future);
            new Thread(watcher).start();
        }
    }

    public static void launchFromStub(boolean portable, File dataDir, String[] args) {
        SimpleLogFormatter.configureGlobalLogger();
        SharedLocale.loadBundle("lang.Launcher", Locale.getDefault());

        log.log(Level.INFO,
                "Using launcher data directory {0}", dataDir.getAbsolutePath());
        final Launcher launcher = new Launcher(dataDir);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    SwingHelper.setLookAndFeel();
                    launcher.showLauncher();
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Failed to load", t);
                    SwingHelper.setSafeLookAndFeel();
                    SwingHelper.showErrorDialog(null,
                            _("errors.criticalLoadError"),
                            _("errors.errorTitle"), t);
                    System.exit(1);
                }
            }
        });
    }

}
