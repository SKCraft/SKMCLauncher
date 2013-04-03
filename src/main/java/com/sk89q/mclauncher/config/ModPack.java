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

import static com.sk89q.mclauncher.util.XMLUtil.getString;
import static com.sk89q.mclauncher.util.XMLUtil.getStringOrNull;
import static com.sk89q.mclauncher.util.XMLUtil.parseXml;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.util.Util;
import com.sk89q.mclauncher.util.XMLUtil;

/**
 * Registers built-in items.
 * 
 * @author sk89q
 */
public class ModPack {

    private static final Logger logger = Logger.getLogger(LauncherOptions.class.getCanonicalName());

    private ModPack() {
    }

    /**
     * Register packaged configurations.
     * 
     * @param configsManager
     *            configurations manager
     */
    public static void register(ConfigurationsManager configsManager) {
        // Attempt to read in a packaged modpack.xml
        InputStream in = Launcher.class.getResourceAsStream("/resources/modpack.xml");

        if (in != null) {
            try {
                Document doc = parseXml(in);
                XPath xpath = XPathFactory.newInstance().newXPath();

                Node node = XMLUtil.getNode(doc, xpath.compile("/modpack/configuration"));

                String id = getString(node, xpath.compile("id/text()"));
                String name = getString(node, xpath.compile("name/text()"));
                String appDir = getStringOrNull(node, xpath.compile("appDir/text()"));

                String urlString = getString(node, xpath.compile("updateURL/text()"));

                if (appDir == null) {
                    appDir = id;
                }

                configsManager.setDefault(configsManager.registerBuiltIn(id, name, appDir, urlString));
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Could not read modpack.xml", e);
            }
        }

        Util.close(in);
    }

    /**
     * Register packaged host lists.
     * 
     * @param hotListManager
     *            host list manager
     */
    public static void register(ServerHotListManager hotListManager) {
    }
}
