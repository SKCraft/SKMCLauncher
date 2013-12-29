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
import org.codehaus.jackson.annotate.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Manages a list of accounts.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(value = JsonMethod.NONE)
public class AccountList extends AbstractListModel implements ComboBoxModel {

    private List<Account> accounts = new ArrayList<Account>();
    private transient Account selected;

    public synchronized void add(@NonNull Account account) {
        if (!accounts.contains(account)) {
            accounts.add(account);
            Collections.sort(accounts);
            fireContentsChanged(this, 0, accounts.size());
        }
    }

    public synchronized void remove(@NonNull Account account) {
        Iterator<Account> it = accounts.iterator();
        while (it.hasNext()) {
            Account other = it.next();
            if (other.equals(account)) {
                it.remove();
                fireContentsChanged(this, 0, accounts.size() + 1);
                break;
            }
        }
    }

    @JsonProperty
    public synchronized List<Account> getAccounts() {
        return accounts;
    }

    public synchronized void setAccounts(@NonNull List<Account> accounts) {
        this.accounts = accounts;
        Collections.sort(accounts);
    }

    @Override
    @JsonIgnore
    public synchronized int getSize() {
        return accounts.size();
    }

    @Override
    public synchronized Account getElementAt(int index) {
        try {
            return accounts.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public void setSelectedItem(Object item) {
        if (item == null) {
            selected = null;
            return;
        }

        if (item instanceof Account) {
            this.selected = (Account) item;
        } else {
            String id = String.valueOf(item).trim();
            Account account = new Account(id);
            for (Account test : accounts) {
                if (test.equals(account)) {
                    account = test;
                    break;
                }
            }
            selected = account;
        }

        if (selected.getId() == null || selected.getId().isEmpty()) {
            selected = null;
        }
    }

    @Override
    @JsonIgnore
    public Account getSelectedItem() {
        return selected;
    }

}
