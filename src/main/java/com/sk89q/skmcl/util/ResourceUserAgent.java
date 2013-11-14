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

package com.sk89q.skmcl.util;

import lombok.extern.java.Log;
import org.xhtmlrenderer.swing.NaiveUserAgent;

import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;

@Log
public class ResourceUserAgent extends NaiveUserAgent {

    @Override
    public String resolveURI(String uri) {
        return uri;
    }

    @Override
    protected InputStream resolveAndOpenStream(String uri) {
        InputStream is = null;
        URL url = ResourceUserAgent.class.getResource(uri);

        if (url == null) {
            log.log(Level.WARNING, "Failed to fetch: " + uri);
            return null;
        }

        try {
            is = url.openStream();
        } catch (java.net.MalformedURLException e) {
            log.log(Level.WARNING, "Bad URI: " + uri, e);
        } catch (java.io.IOException e) {
            log.log(Level.WARNING, "I/O error: " + uri, e);
        }

        return is;
    }

}
