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

import lombok.Delegate;
import lombok.Getter;
import lombok.NonNull;

import javax.swing.*;

public class IdentityManagerModel implements ComboBoxModel, Cloneable {

    @Getter @Delegate
    private final IdentityManager identityManager;
    private Identity selected;

    public IdentityManagerModel(@NonNull IdentityManager identityManager) {
        this.identityManager = identityManager;
        selected = (Identity) identityManager.getElementAt(0);
    }

    @Override
    public synchronized void setSelectedItem(Object item) {
        if (item == null) {
            this.selected = null;
        } else if (item instanceof Identity) {
            Identity identity = (Identity) item;
            this.selected = identity;
        } else {
            String id = String.valueOf(item).trim();
            if (id.isEmpty()) {
                this.selected = null;
            } else {
                this.selected = new Identity(id);
            }
        }
    }

    @Override
    public synchronized Object getSelectedItem() {
        return selected;
    }

    @Override
    public IdentityManagerModel clone() {
        IdentityManagerModel object = new IdentityManagerModel(identityManager);
        object.setSelectedItem(getSelectedItem());
        return object;
    }
}
