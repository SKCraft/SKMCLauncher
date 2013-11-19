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

import lombok.NonNull;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class IdentityManager extends AbstractListModel {

    private List<Identity> identities = new ArrayList<Identity>();

    public synchronized void add(@NonNull Identity identity) {
        if (!identities.contains(identity)) {
            identities.add(identity);
            Collections.sort(identities);
            fireContentsChanged(this, 0, identities.size());
        }
    }

    public synchronized void remove(@NonNull Identity identity) {
        Iterator<Identity> it = identities.iterator();
        while (it.hasNext()) {
            Identity other = it.next();
            if (other.equals(identity)) {
                it.remove();
                fireContentsChanged(this, 0, identities.size() + 1);
                break;
            }
        }
    }

    public synchronized List<Identity> getIdentities() {
        return identities;
    }

    public synchronized void setIdentities(List<Identity> identities) {
        this.identities = identities;
        Collections.sort(identities);
    }

    @Override
    @JsonIgnore
    public synchronized int getSize() {
        return identities.size();
    }

    @Override
    public synchronized Object getElementAt(int index) {
        try {
            return identities.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
}
