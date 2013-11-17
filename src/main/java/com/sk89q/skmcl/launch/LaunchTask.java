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
import com.sk89q.skmcl.application.Application;
import com.sk89q.skmcl.application.Instance;
import com.sk89q.skmcl.worker.Segment;
import com.sk89q.skmcl.util.Environment;
import com.sk89q.skmcl.worker.Task;
import lombok.Getter;

import static com.sk89q.skmcl.util.SharedLocale._;

/**
 * Launches a given application and then returns a {@link LaunchedProcess} on
 * success, which must be managed by the calling routine.
 */
public class LaunchTask extends Task<LaunchedProcess> {

    @Getter
    private final Application application;
    @Getter
    private final LaunchContext launchContext;

    public LaunchTask(Application application, LaunchContext launchContext) {
        this.application = application;
        this.launchContext = launchContext;
    }

    @Override
    public LaunchedProcess call() throws Exception {
        Instance instance = application.getInstance(Environment.getInstance());

        Segment step1 = segment(0.9),
                step2 = segment(0.1);

        try {
            Task<?> updater = instance.getUpdater();
            updater.addObserver(step1);
            updater.call();
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new LauncherException(e, _("updater.updateFailed"));
        }

        step2.push(0, _("launcher.launching"));

        return instance.launch(launchContext);
    }
}
