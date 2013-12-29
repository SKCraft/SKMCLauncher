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

package com.sk89q.skmcl.session;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An offline session.
 */
@ToString
public class OfflineSession implements Session {

    @Getter
    private final List<Identity> identities;

    public OfflineSession() {
        List<Identity> identities = new ArrayList<Identity>();
        Identity identity = new Identity();
        identity.setId("0");
        identity.setName("Player");
        identities.add(identity);
        this.identities = Collections.unmodifiableList(identities);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public List<Identity> getIdentities() {
        return identities;
    }

    @Override
    public String getAccessToken() {
        return null;
    }

    @Override
    public String getClientToken() {
        return null;
    }

}
