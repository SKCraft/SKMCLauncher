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

package com.sk89q.mclauncher.config;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.swing.ComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.util.LauncherUtils;

/**
 * Manages saved identities.
 */
public class IdentityList implements ComboBoxModel {

    private transient EventListenerList listenerList = new EventListenerList();

    private boolean hasLoggedIn;
    private String lastId;
    private List<Identity> identities = new ArrayList<Identity>();
    
    private transient Identity selected;

    public IdentityList() {
    }
    
    @XmlAttribute(name = "loggedIn")
    public boolean getHasLoggedIn() {
        return hasLoggedIn;
    }

    public void setHasLoggedIn(boolean hasLoggedIn) {
        this.hasLoggedIn = hasLoggedIn;
    }

    @XmlAttribute(name = "lastUsed")
    public String getLastUsedId() {
        return lastId;
    }
    
    public void setLastUsedId(String lastId) {
        this.lastId = lastId;
    }

    @XmlElement(name = "identity")
    public List<Identity> getIdentities() {
        return identities;
    }

    public void setIdentities(List<Identity> identities) {
        this.identities = identities;
    }
    
    /**
     * Return the last identity used, or the first identity if there is no last identity,
     * or null if there are no identities stored.
     * 
     * @return an identity, or null
     */
    @XmlTransient
    public Identity getFirstShown() {
        Identity identity = byId(getLastUsedId());
        if (identity == null) {
            Iterator<Identity> it = getIdentities().iterator();
            if (it.hasNext()) {
                identity = it.next();
            }
        }
        return identity;
    }
    
    /**
     * Return the last identity used, possibly null.
     * 
     * @return an identity, or null
     */
    @XmlTransient
    public Identity getLastUsed() {
        return byId(getLastUsedId());
    }
    
    /**
     * Set the last identity used, possibly null.
     * 
     * @param identity the last identity used, or null
     */
    public void setLastUsed(Identity identity) {
        if (identity == null) {
            setLastUsedId(null);
        } else {
            setLastUsedId(identity.getId());
        }
    }
    
    /**
     * Get an identity using a case-insensitive search on the id.
     * 
     * @param id the id
     * @return the identity, or null
     */
    public Identity byId(String id) {
        for (Identity identity : identities) {
            if (identity.getId().equalsIgnoreCase(id)) {
                return identity;
            }
        }
        
        return null;
    }

    /**
     * Get a list of identity IDs.
     * 
     * @return list of IDs
     */
    public Set<String> getIds() {
        Set<String> usernames = new HashSet<String>();
        for (Identity identity : identities) {
            usernames.add(identity.getId());
        }
        return usernames;
    }
    
    /**
     * Get a saved password.
     * 
     * @param id id
     * @return password or null if no password is saved
     */
    public String getPassword(String id) {
        Identity identity = byId(id);
        if (identity == null) {
            return null;
        }
        return identity.getPassword();
    }
    
    /**
     * Remember a given identity.
     * 
     * @param id id
     * @param password password, possibly null to only remember the name
     */
    public void remember(String id, String password) {
        Identity identity = byId(id);
        if (identity != null) {
            identity.setId(id);
            identity.setPassword(password);
        } else {
            identity = new Identity(id, password);
            identities.add(identity);
        }
        
        identity.updateLastLogin();
        setLastUsedId(id);
    }
    
    /**
     * Forget a user's password but not the user him/herself.
     * 
     * @param id the ID
     */
    public void forgetPassword(String id) {
        Identity identity = byId(id);
        if (identity == null) {
            return;
        }
        identity.setPassword(null);
    }
    
    /**
     * Forget a given identity.
     * 
     * @param id id
     */
    public void forget(String id) {
        Iterator<Identity> it = identities.iterator();
        while (it.hasNext()) {
            String identityId = it.next().getId();
            if (identityId == null) {
                it.remove(); // Shouldn't have identities with null IDs
            }
            if (identityId.equalsIgnoreCase(id)) {
                it.remove();
            }
        }
        
        if (getLastUsedId() != null && getLastUsedId().equalsIgnoreCase(id)) {
            setLastUsedId(null);
        }

        fireListDataEvent(new ListDataEvent(
                this, ListDataEvent.CONTENTS_CHANGED, 0, getSize()));
    }

    /**
     * Forget all remembered identities.
     */
    public void forgetAll() {
        identities.clear();
        setLastUsed(null);

        fireListDataEvent(new ListDataEvent(
                this, ListDataEvent.CONTENTS_CHANGED, 0, getSize()));
    }
    
    /**
     * Import old launcher settings.
     */
    public void importLogins() {
        File file = new File(Launcher.getOfficialDataDir(), "lastlogin");
        if (!file.exists()) return;
        
        DataInputStream in = null;
        try {
            Cipher cipher = Launcher.getInstance().getCipher(
                    Cipher.DECRYPT_MODE, Identity.MOJANG_ENCRYPTION_PASSWORD);
            in = new DataInputStream(new CipherInputStream(
                    new FileInputStream(file), cipher));
            
            String username = in.readUTF();
            String password = in.readUTF();
            
            if (username.trim().length() == 0) {
                return;
            }
            if (password.trim().length() == 0) {
                password = null;
            }

            remember(username, password);
        } catch (Throwable e) {
        } finally {
            LauncherUtils.close(in);
        }
    }

    @Override
    public int getSize() {
        return getIdentities().size();
    }

    @Override
    public Object getElementAt(int index) {
        return getIdentities().get(index);
    }

    @Override
    public void setSelectedItem(Object item) {
        if (item instanceof Identity) {
            this.selected = (Identity) item;
            return;
        }
        
        String id = String.valueOf(item);
        Identity identity = byId(id);
        if (identity != null) {
            this.selected = identity;
        } else {
            this.selected = new Identity(id, null);
        }

        fireListDataEvent(new ListDataEvent(
                this, ListDataEvent.CONTENTS_CHANGED, 0, getSize()));
    }

    @XmlTransient
    @Override
    public Object getSelectedItem() {
        if (selected == null) {
            selected = getFirstShown();
        }
        return selected;
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listenerList.add(ListDataListener.class, l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
    }
    
    private void fireListDataEvent(final ListDataEvent event) {
        final Object[] listeners = listenerList.getListenerList();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (int i = listeners.length - 2; i >= 0; i -= 2) {
                    if (listeners[i] == ListDataListener.class) {
                        ((ListDataListener) listeners[i + 1]).contentsChanged(event);
                    }
                }
            }
        });
    }

    void afterUnmarshal(Unmarshaller u, Object parent) {
        Collections.sort(identities);
    }

}
