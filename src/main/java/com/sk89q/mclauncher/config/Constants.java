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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.security.X509KeyRing;
import com.sk89q.mclauncher.security.X509KeyStore;
import com.sk89q.mclauncher.security.X509KeyRing.Ring;

/**
 * Registers built-in items.
 * 
 * @author sk89q
 */
public class Constants {

    public static final boolean VERIFY_CUSTOM_DOWNLOADS = false;
    public static final URL NEWS_URL;
        
    private static final String NEWS_URL_BASE = "http://minecraft.update.sk89q.com/updates/?v=%version%";
    
    static {
        try {
            String urlStr = NEWS_URL_BASE;

            try {
                urlStr = urlStr.replace("%version%",
                        URLEncoder.encode(Launcher.VERSION, "UTF-8"));
            } catch (UnsupportedEncodingException e1) {
            }

            NEWS_URL = new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private Constants() {
    }

    /**
     * Register built in configurations.
     * 
     * @param configsManager
     *            configurations manager
     */
    public static void register(ConfigurationsManager configsManager) {
        configsManager.setDefault(configsManager.registerBuiltIn("minecraft",
                "Default", null, null));
    }

    /**
     * Register built in certificates.
     * 
     * @param keyRing
     *            key ring
     * @throws IOException
     *             on I/O error
     * @throws CertificateException
     *             on certificate load error
     */
    public static void register(X509KeyRing keyRing)
            throws CertificateException, IOException {
        X509KeyStore keyStore;

        keyStore = keyRing.getKeyStore(Ring.MINECRAFT_LOGIN);
        keyStore.addRootCertificates(Launcher.class
                .getResourceAsStream("/resources/mclogin.cer"));

        keyStore = keyRing.getKeyStore(Ring.UPDATE);
        keyStore.addRootCertificates(Launcher.class
                .getResourceAsStream("/resources/mcupdate.cer"));
        keyStore.addRootCertificates(Launcher.class
                .getResourceAsStream("/resources/sk89q.cer"));
    }
    
    /**
     * Register built-in host lists.
     * 
     * @param hotListManager host list manager
     */
    public static void register(ServerHotListManager hotListManager) {
    }

}
