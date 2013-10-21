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

package com.sk89q.skmcl.profile;

import com.sk89q.skmcl.application.Application;

import java.io.File;

public class SimpleProfile implements Profile {

    private Application application;
    private File baseDir;

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
        application.setProfile(this);
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public File getContentDir() {
        return new File(getBaseDir(), "content");
    }

    public File getTemporaryDir() {
        return new File(getBaseDir(), "temp");
    }

}
