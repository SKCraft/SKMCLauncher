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

package com.sk89q.mclauncher.security;

import java.util.EnumMap;

/**
 * Stores {@link X509KeyStore}.
 * 
 * @author sk89q
 */
public class X509KeyRing {
    
    public static enum Ring {
        MINECRAFT_LOGIN,
        UPDATE
    }

    private EnumMap<Ring, X509KeyStore> rings = new EnumMap<X509KeyRing.Ring, X509KeyStore>(
            Ring.class);
    
    /**
     * Construct.
     */
    public X509KeyRing() {
        for (Ring ring : Ring.values()) {
            rings.put(ring, new X509KeyStore());
        }
    }
    
    /**
     * Get the key store for the given ring.
     * 
     * @param ring ring
     * @return key store
     */
    public X509KeyStore getKeyStore(Ring ring) {
        return rings.get(ring);
    }

}
