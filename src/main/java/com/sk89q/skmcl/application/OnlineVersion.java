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

package com.sk89q.skmcl.application;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.IOException;

/**
 * A {@link Version} that may check an potentially unavailable resource in order to
 * resolve the version that will actually be updated and launched.
 *
 * <p>Because the required information may not be available at certain times,
 * this class allows the storage of a "cached version" that can be persisted
 * with an application.</p>
 */
public abstract class OnlineVersion extends Version {

    @Getter @Setter
    private Version cachedVersion;

    @Override
    public Version resolve(@NonNull Application application, boolean offline)
            throws OnlineRequiredException, ResolutionException {
        Version cached = cachedVersion;

        if (offline) {
            if (cached != null) {
                return cached;
            } else {
                throw new OnlineRequiredException("No cached version");
            }
        } else {
            try {
                Version version = resolveOnline(application);
                setCachedVersion(version);
                return version;
            } catch (IOException e) {
                throw new ResolutionException(e, cached != null);
            }
        }
    }

    public abstract Version resolveOnline(Application application) throws IOException;
}
