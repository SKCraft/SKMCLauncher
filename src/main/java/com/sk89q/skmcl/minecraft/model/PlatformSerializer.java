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

package com.sk89q.skmcl.minecraft.model;

import com.sk89q.skmcl.util.Platform;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;

public class PlatformSerializer extends JsonDeserializer<Platform> {

    @Override
    public Platform deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        String text = jsonParser.getText();
        if (text.equalsIgnoreCase("windows")) {
            return Platform.WINDOWS;
        } else if (text.equalsIgnoreCase("linux")) {
            return Platform.LINUX;
        } else if (text.equalsIgnoreCase("solaris")) {
            return Platform.SOLARIS;
        } else if (text.equalsIgnoreCase("osx")) {
            return Platform.MAC_OS_X;
        } else {
            throw new IOException("Unknown platform: " + text);
        }
    }

}
