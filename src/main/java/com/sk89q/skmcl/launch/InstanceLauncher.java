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

import com.sk89q.skmcl.application.Application;
import com.sk89q.skmcl.application.Instance;
import com.sk89q.skmcl.worker.Segment;
import com.sk89q.skmcl.util.Environment;
import com.sk89q.skmcl.worker.Task;
import lombok.Getter;

public class InstanceLauncher extends Task<LaunchedProcess> {

    @Getter
    private final Application application;

    public InstanceLauncher(Application application) {
        this.application = application;
    }

    @Override
    protected void run() throws Exception {
        Instance instance = application.getInstance(Environment.getInstance());

        Segment step1 = segment(0.9),
                step2 = segment(0.1);

        Task<?> updater = instance.getUpdater();
        updater.addObserver(step1);
        updater.call();
    }
}
