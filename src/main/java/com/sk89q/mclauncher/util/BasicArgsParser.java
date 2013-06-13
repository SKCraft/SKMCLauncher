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

package com.sk89q.mclauncher.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BasicArgsParser {

    private Set<String> flagArgs = new HashSet<String>();
    private Set<String> valueArgs = new HashSet<String>();;
    
    public void addFlagArg(String arg) {
        flagArgs.add(arg);
    }
    
    public void addValueArg(String arg) {
        valueArgs.add(arg);
    }
    
    public ArgsContext parse(String[] args) {
        List<String> leftOver = new ArrayList<String>();
        Set<String> flags = new HashSet<String>();
        Map<String, String> values = new HashMap<String, String>();

        String wantingFlag = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                if (arg.length() == 1) {
                    throw new IllegalArgumentException("Flag with no name");
                }
                String flag = arg.substring(1);
                if (valueArgs.contains(flag)) {
                    wantingFlag = flag;
                } else if (flagArgs.contains(flag)) {
                    flags.add(flag);
                } else {
                    throw new IllegalArgumentException("Unknown flag " + flag);
                }
            } else if (wantingFlag != null) {
                values.put(wantingFlag, arg);
                wantingFlag = null;
            } else {
                leftOver.add(arg);
            }
        }
        
        return new ArgsContext(flags, values, leftOver);
    }
    
    public static class ArgsContext {
        
        private List<String> args;
        private Set<String> flags;
        private Map<String, String> values;
        
        private ArgsContext(Set<String> flags, Map<String, String> values, List<String> args) {
            this.args = args;
            this.flags = flags;
            this.values = values;
        }

        public List<String> getArgs() {
            return args;
        }

        public Set<String> getFlags() {
            return flags;
        }
        
        public boolean has(String flag) {
            return flags.contains(flag);
        }

        public Map<String, String> getValues() {
            return values;
        }
        
        public String get(int i) {
            return args.get(i);
        }
        
        public String get(String flag) {
            return values.get(flag);
        }
        
        public boolean getBool(String flag, boolean def) {
            String val = get(flag);
            if (val == null) return def;
            return val == "true";
        }
        
        public int getInt(String flag, int def) {
            String val = get(flag);
            if (val == null) return def;
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return def;
            }
        }

        public int length() {
            return args.size();
        }
        
    }
    
}
