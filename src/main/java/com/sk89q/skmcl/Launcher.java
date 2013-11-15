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

import com.sk89q.skmcl.util.SimpleLogFormatter;
import com.sk89q.skmcl.launch.InstanceLauncher;
import com.sk89q.skmcl.profile.Profile;
import com.sk89q.skmcl.profile.ProfileManager;
import com.sk89q.skmcl.swing.CreateProfileDialog;
import com.sk89q.skmcl.swing.LauncherFrame;
import com.sk89q.skmcl.swing.SwingHelper;
import com.sk89q.skmcl.util.Persistence;
import com.sk89q.skmcl.util.SharedLocale;
import com.sk89q.skmcl.worker.Worker;
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

/**
 * Main launcher class.
 */
@Log
public class Launcher {

    @Getter
    private final File baseDir;
    @Getter
    private final ProfileManager profiles;

    public Launcher(@NonNull File baseDir) {
        this.baseDir = baseDir;
        this.profiles = new ProfileManager(baseDir);
    }

    public LauncherFrame showLauncher() {
        LauncherFrame frame = new LauncherFrame(this);
        frame.setVisible(true);
        return frame;
    }

    public CreateProfileDialog showCreateProfile(Window owner) {
        CreateProfileDialog dialog = new CreateProfileDialog(owner, this);
        dialog.setVisible(true);
        return dialog;
    }

    public void launchApplication(Window owner, Worker worker, Profile profile) {
        profile.setLastLaunchDate(new Date());
        Persistence.commitAndForget(profile);
        getProfiles().notifyUpdate();

        InstanceLauncher task = new InstanceLauncher(profile.getApplication());
        worker.submit(task);
    }

    public static void main(String[] args) {
        // Configure the logger
        SimpleLogFormatter.configureGlobalLogger();

        // Load language
        SharedLocale.loadBundle("lang.Launcher", Locale.getDefault());

        // Initialize launcher
        File dir = new File("_tempdata");
        log.log(Level.INFO, "Using launcher data directory {0}", dir.getAbsolutePath());
        final Launcher launcher = new Launcher(dir);

        // Load up the UI
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
