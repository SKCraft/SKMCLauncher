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

import lombok.NonNull;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.WeakHashMap;

/**
 * Simple persistence framework that can bind an object to a file and later allow for
 * code utilizing the object to save it globally.
 */
public final class Persistence {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final WeakHashMap<Object, File> bound =
            new WeakHashMap<Object, File>();

    private Persistence() {
    }

    /**
     * Bind an object to a path where the object will be saved.
     *
     * @param object the object
     * @param file the path to save to
     */
    public static void bind(@NonNull Object object, @NonNull File file) {
        synchronized (bound) {
            bound.put(object, file);
        }
    }

    /**
     * Save an object to file.
     *
     * @param object the object
     * @throws IOException on save error
     */
    public static void commit(@NonNull Object object) throws IOException {
        File file;
        synchronized (bound) {
            file = bound.get(object);
            if (file == null) {
                throw new IOException("Cannot persist unbound object: " + object);
            }
        }

        file.getParentFile().mkdirs();
        mapper.writeValue(file, object);
    }

    /**
     * Read an object from file.
     *
     * <p>If the file does not exist or loading fails, construct a new instance of
     * the given class by using its no-arg constructor.</p>
     *
     * @param file the file
     * @param cls the class
     * @param <V> the type of class
     * @return an object
     */
    public static <V> V load(File file, Class<V> cls) {
        try {
            return mapper.readValue(file, cls);
        } catch (IOException e) {
            try {
                return cls.newInstance();
            } catch (InstantiationException e1) {
                throw new RuntimeException(
                        "Failed to construct object with no-arg constructor", e1);
            } catch (IllegalAccessException e1) {
                throw new RuntimeException(
                        "Failed to construct object with no-arg constructor", e1);
            }
        }
    }

}
