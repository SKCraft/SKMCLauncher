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

import com.sk89q.skmcl.util.Persistence;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import javax.swing.*;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ProfileManager extends AbstractListModel implements ComboBoxModel {

    private static final Pattern invalidChars = Pattern.compile(
            "[^\\w\\-. ',\\\\-_\\[\\]\\(\\)~\\{\\}<>]+");

    @Getter
    private transient File baseDir;
    private transient Profile selected;
    private transient final List<Profile> profiles = new ArrayList<Profile>();

    public ProfileManager(@NonNull File baseDir) {
        this.baseDir = baseDir;
    }

    public File getSharedDir() {
        return new File(baseDir, "shared");
    }

    public File getProfilesDir() {
        return new File(baseDir, "profiles");
    }

    private File getProfileFile(File dir) {
        return new File(dir, "profile.json");
    }

    public List<Profile> getProfiles() {
        return Collections.unmodifiableList(profiles);
    }

    public void add(@NonNull Profile profile) throws IOException {
        if (!profiles.contains(profile)) {
            File dir = findDir(profile.getName());
            File file = getProfileFile(dir);
            Persistence.bind(profile, file);
            Persistence.commit(profile);
            profiles.add(profile);
            fireContentsChanged(this, 0, profiles.size());
        }
    }

    private File findDir(@NonNull String name) {
        String baseName = normalizeName(name);
        int i = 0;
        File dir = new File(getProfilesDir(), baseName);

        while (dir.exists()) {
            dir = new File(getProfilesDir(), baseName + (i++));
        }

        return dir;
    }

    private String normalizeName(@NonNull String name) {
        String valid = invalidChars.matcher(name.trim()).replaceAll("");
        if (valid.length() == 0) {
            valid = "unnamed";
        }
        return valid;
    }

    public void remove(@NonNull Profile profile) {
        profiles.remove(profile);
        fireContentsChanged(this, 0, profiles.size() + 1);
    }

    public void load() {
        profiles.clear();

        File[] files = getProfilesDir()
                .listFiles((FileFilter) DirectoryFileFilter.INSTANCE);

        if (files != null) {
            for (File d : files) {
                File path = getProfileFile(d);
                Profile profile = Persistence.load(path, Profile.class, true);
                if (profile != null) {
                    profile.setSharedDir(getSharedDir());
                    profile.setBaseDir(d);
                    profiles.add(profile);
                }
            }
        }

        fireContentsChanged(this, 0, profiles.size());
    }

    @Override
    public int getSize() {
        return profiles.size();
    }

    @Override
    public Object getElementAt(int index) {
        return profiles.get(index);
    }

    @Override
    public void setSelectedItem(Object item) {
        if (item != null && item instanceof Profile) {
            Profile profile = (Profile) item;
            if (profiles.contains(profile)) {
                selected = profile;
            }
        }
    }

    @Override
    public Object getSelectedItem() {
        return selected;
    }
}
