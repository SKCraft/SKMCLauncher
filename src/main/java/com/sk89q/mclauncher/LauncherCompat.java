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

package com.sk89q.mclauncher;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to handle arguments from the official launcher.
 * 
 * @author sk89q
 */
public class LauncherCompat {

    public static void main(String[] args) {
        List<String> usedArgs = new ArrayList<String>();
        
        if (args.length >= 1) {
            usedArgs.add("-username");
            usedArgs.add(args[0]);
            if (args.length >= 2) {
                usedArgs.add("-password");
                usedArgs.add(args[1]);
                usedArgs.add("-launch");
            }
        }
        
        if (args.length >= 3) {
            String ip = args[2];
            String port = "25565";
            if (ip.contains(":")) {
                String parts[] = ip.split(":");
                ip = parts[0];
                port = parts[1];
            }
            usedArgs.add("-address");
            usedArgs.add(ip + ":" + port);
        }
        
        String[] argsFinal = new String[usedArgs.size()];
        usedArgs.toArray(argsFinal);
        
        Launcher.main(argsFinal);
    }

}
