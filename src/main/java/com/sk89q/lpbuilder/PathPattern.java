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

package com.sk89q.lpbuilder;

import java.util.Collection;
import java.util.EnumSet;

import javax.xml.bind.annotation.XmlValue;

import util.FnMatch;
import util.FnMatch.Flag;

public abstract class PathPattern {
    
    private static EnumSet<FnMatch.Flag> fnMatchFlags = 
            EnumSet.of(Flag.CASEFOLD, Flag.PERIOD);
    
    private String value;
    
    public PathPattern() {
    }
    
    public PathPattern(String value) {
        this.value = value;
    }

    @XmlValue
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static boolean matches(Collection<PathPattern> patterns, String path) {
        boolean matches = false;
        
        for (PathPattern pattern : patterns) {
            if (FnMatch.fnmatch(pattern.getValue(), path, fnMatchFlags)) {
                if (pattern instanceof Include) {
                    matches = true;
                } else if (pattern instanceof Exclude) {
                    matches = false;
                }
            }
        }
        
        return matches;
    }

    public static class Include extends PathPattern {
        public Include() {
            super();
        }

        public Include(String value) {
            super(value);
        }
    }

    public static class Exclude extends PathPattern {
        public Exclude() {
            super();
        }

        public Exclude(String value) {
            super(value);
        }
    }
    
}
