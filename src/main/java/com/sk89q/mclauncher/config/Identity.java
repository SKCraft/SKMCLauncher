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

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import util.Base64;

import com.sk89q.mclauncher.Launcher;

public class Identity implements Comparable<Identity> {

    private static final Logger logger = Logger.getLogger(
            Identity.class.getCanonicalName());
    
    /**
     * The password that Mojang uses in its launcher.
     */
    public static final String MOJANG_ENCRYPTION_PASSWORD = "passwordfile";

    private String id;
    private String key;
    private Date lastLogin;
    
    private transient String password;

    public Identity() {
    }

    public Identity(String id, String password) {
        setId(id);
        setPassword(password);
    }

    @XmlElement(name = "name")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;

        // Clear cached password
        password = null;
        
        // Attempt to decrypt password
        if (key != null && key.length() > 0) {
            try {
                Cipher cipher = Launcher.getInstance().getCipher(
                        Cipher.DECRYPT_MODE, MOJANG_ENCRYPTION_PASSWORD);
                byte[] decrypted = cipher.doFinal(Base64.decode(key));
                password = new String(decrypted, "UTF-8");
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Failed to decrypt password", e);
            }
        }
    }

    @XmlTransient
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;

        // Clear key
        key = null;

        // Attempt to encrypt password
        if (password != null && password.length() > 0) {
            try {
                Cipher cipher = Launcher.getInstance().getCipher(
                        Cipher.ENCRYPT_MODE, MOJANG_ENCRYPTION_PASSWORD);
                byte[] encrypted = cipher.doFinal(password.getBytes());
                key = Base64.encodeToString(encrypted, false);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Failed to decrypt password", e);
            }
        }
    }

    @XmlElement(name = "lastLogin")
    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void updateLastLogin() {
        setLastLogin(new Date());
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().toLowerCase().hashCode() : -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof Identity) {
            return getId() != null ? getId().toLowerCase().equals(
                    ((Identity) obj).getId().toLowerCase()) : false;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(Identity o) {
        if (getLastLogin() == null && o.getLastLogin() == null) {
            return 0;
        } else if (getLastLogin() == null) {
            return 1;
        } else if (o.getLastLogin() == null) {
            return -1;
        } else {
            return -getLastLogin().compareTo(o.getLastLogin());
        }
    }

}
