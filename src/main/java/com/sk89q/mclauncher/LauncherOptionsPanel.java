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

import javax.swing.JCheckBox;

import com.sk89q.mclauncher.config.Def;
import com.sk89q.mclauncher.util.SettingsList;

public class LauncherOptionsPanel extends OptionsPanel {

    private static final long serialVersionUID = 2686672020024079731L;

    public LauncherOptionsPanel(SettingsList settings, boolean withUse) {
        super(settings, withUse);
    }
    
    @Override
    protected void buildControls() {
        createFieldGroup("Launcher");
        addField(Def.LAUNCHER_NO_NEWS, new JCheckBox("Don't load the news"));
        addField(Def.LAUNCHER_HIDE_NEWS, new JCheckBox("Hide the news panel"));
        addField(Def.LAUNCHER_REOPEN, new JCheckBox("Show the launcher on Minecraft close"));

        createFieldGroup("Console");
        /*addField(Def.LOCAL_CONSOLE, new JCheckBox("Open console in launcher process"))
        .setToolTipText("Unchecked, the console opens in the same process as Minecraft, which can crash");*/
        addField(Def.COLORED_CONSOLE, new JCheckBox("Use colors in the console"));
        addField(Def.CONSOLE_KILLS_PROCESS, new JCheckBox("Kill Minecraft on console close"));

        createFieldGroup("Addons");
        addField(Def.FAST_TEST, new JCheckBox("Addon test uses offline mode"));
        
        createFieldGroup("Updates");
        addField(Def.BACKUP_MINEJAR, new JCheckBox("Backup minecraft.jar"));
    }

}
