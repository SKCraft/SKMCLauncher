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

package com.sk89q.mclauncher.update;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import lzma.sdk.lzma.Decoder;
import lzma.streams.LzmaInputStream;

/**
 * Stores a database of file stream filters.
 * 
 * @author sk89q
 */
public class FileStreamFilters {
    
    private static Map<String, StreamFilter> filters =
            new HashMap<String, StreamFilter>();
    
    /**
     * Register a stream filter.
     * 
     * @param ext extension
     * @param filter filter
     */
    public static void register(String ext, StreamFilter filter) {
        filters.put(ext.toLowerCase(), filter);
    }
    
    /**
     * Get a stream filter.
     * 
     * @param ext extension
     * @return filter or null if not found
     */
    public static StreamFilter get(String ext) {
        return filters.get(ext.toLowerCase());
    }
    
    static {
        register("lzma", new StreamFilter() {
            @Override
            public InputStream filter(InputStream stream) throws IOException {
                return new LzmaInputStream(stream, new Decoder());
            }
        });

        register("gz", new StreamFilter() {
            @Override
            public InputStream filter(InputStream stream) throws IOException {
                return new GZIPInputStream(stream);
            }
        });
    }
    
}
