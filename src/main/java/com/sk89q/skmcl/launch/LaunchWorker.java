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

import com.sk89q.skmcl.LauncherException;
import com.sk89q.skmcl.application.*;
import com.sk89q.skmcl.concurrent.WorkUnit;
import com.sk89q.skmcl.profile.Profile;
import com.sk89q.skmcl.session.OfflineSession;
import com.sk89q.skmcl.session.Session;
import com.sk89q.skmcl.swing.SwingHelper;
import com.sk89q.skmcl.util.Environment;
import com.sk89q.skmcl.util.Persistence;
import com.sk89q.skmcl.concurrent.AbstractWorker;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.logging.Level;

import static com.sk89q.skmcl.util.SharedLocale._;

/**
 * Launches a given application and then returns a {@link LaunchedProcess} on
 * success, which must be managed by the calling routine.
 */
@Log
public class LaunchWorker extends AbstractWorker<LaunchedProcess> {

    @Getter
    private final Profile profile;
    @Getter
    private final Application application;
    @Getter @Setter
    private Environment environment = Environment.getInstance();
    @Getter @Setter
    private Session session = new OfflineSession();
    @Getter @Setter
    private boolean offline;

    public LaunchWorker(Profile profile) {
        this.profile = profile;
        this.application = profile.getApplication();
    }

    private Instance getInstance() throws InterruptedException, LauncherException {
        while (true) {
            try {
                return application.getInstance(environment, offline);
            } catch (OnlineRequiredException e) {
                throw new LauncherException(e, _("launch.onlineModeRequired"));
            } catch (ResolutionException e) {
                if (offline || !e.isOfflineAvailable()) {
                    throw new LauncherException(
                            "Failed to resolve version",
                            _("launch.cannotResolveVersion"));
                } else {
                    LaunchWorker.log.log(Level.WARNING, "Version resolution failure", e);

                    if (SwingHelper.confirmDialog(null,
                            _("launch.switchOffline"),
                            _("launch.switchOfflineTitle"))) {
                        offline = true;
                    } else {
                        throw new InterruptedException();
                    }
                }
            }
        }
    }

    private LaunchedProcess launch(Instance instance)
            throws IOException, UpdateRequiredException {
        LaunchContext context = new LaunchContext(environment, session);
        return instance.launch(context);
    }

    private void update(Instance instance, WorkUnit workUnit)
            throws LauncherException, InterruptedException {
        try {
            AbstractWorker<?> updater = instance.getUpdater();
            updater.addObserver(workUnit);
            updater.call();
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new LauncherException(e, _("updater.updateFailed"));
        }
    }

    @Override
    public LaunchedProcess call() throws LauncherException, InterruptedException {
        WorkUnit step1 = split(0.1),
                step2 = split(0.8),
                step3 = split(0.1);

        // First resolve the version (i.e. latest -> which version is "latest"?)

        setLocalizedTitle(_("launch.launchingTitle", profile.toString()));
        step1.push(0, _("launch.checkingVersion"));

        Instance instance = getInstance();
        Persistence.commitAndForget(profile);

        // Then attempt to launch
        // But an update MAY be required

        step2.push(0, _("launch.launching"));

        try {
            return launch(instance);
        } catch (UpdateRequiredException e) {
            // Update required, so we're going to go to the update step
        } catch (IOException e) {
            throw new LauncherException(e, _("launch.launchFailed"));
        }

        // If we're here, then it looks like an update is required

        if (!offline) {
            setLocalizedTitle(_("launch.updatingTitle", profile.toString()));
            step2.push(0, _("launch.updating"));
            update(instance, step2);
        } else {
            throw new LauncherException(
                    "Can't update if offline", _("launch.onlineModeRequired"));
        }

        // Update's done, so let's try launching one more time

        setLocalizedTitle(_("launch.launchingTitle", profile.toString()));
        step3.push(0, _("launch.launching"));

        try {
            return launch(instance);
        } catch (UpdateRequiredException e) {
            // This shouldn't be thrown here, since we've already updated,
            // but perhaps something failed
            throw new LauncherException(e, _("updater.launchFailed"));
        } catch (IOException e) {
            throw new LauncherException(e, _("updater.launchFailed"));
        }
    }
}
