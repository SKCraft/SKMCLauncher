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

import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.sk89q.mclauncher.config.Def;
import com.sk89q.mclauncher.config.SettingsList;

public class EnvironmentOptionsPanel extends OptionsPanel {

    private static final long serialVersionUID = -8660113726951738860L;

    public EnvironmentOptionsPanel(SettingsList settings, boolean withUse) {
        super(settings, withUse);
    }
    
    @Override
    protected void buildControls() {
        createFieldGroup("Game window");
        addField(Def.WINDOW_WIDTH, "Width (def. 854)", new JSpinner(new SpinnerNumberModel(0, 0, 1024 * 15, 128)));
        addField(Def.WINDOW_HEIGHT, "Height (def. 480):", new JSpinner(new SpinnerNumberModel(0, 0, 1024 * 15, 128)));
        addField(Def.WINDOW_FULLSCREEN, new JCheckBox("Always open full screen"));
        
        createFieldGroup("Java");
        addField(Def.JAVA_MIN_MEM, "Initial memory (MB):", new JSpinner(new SpinnerNumberModel(0, 0, 1024 * 15, 128)));
        addField(Def.JAVA_MAX_MEM, "Maximum memory (MB):", new JSpinner(new SpinnerNumberModel(0, 0, 1024 * 15, 128)));
        addField(Def.JAVA_RUNTIME, "Java runtime path:", new JTextField())
            .setToolTipText("The path to Java's directory containing 'java' and 'javaw' executables.");
        addField(Def.JAVA_ARGS, "JVM arguments:", new JTextField())
            .setToolTipText("Extra JVM arguments to append.");
        addField(Def.JAVA_CLASSPATH, "Extra classpath:", new JTextField())
            .setToolTipText("List of extra classpath entries (separated by " + File.pathSeparator + ").");
        addField(Def.JAVA_WRAPPER_PROGRAM, "Wrapper program path:", new JTextField())
            .setToolTipText("A path to a program that will wrap around Java.");
        addField(Def.JAVA_CONSOLE, new JCheckBox("Always show console"));
        
        createFieldGroup("Display");
        addField(Def.LWJGL_DEBUG, new JCheckBox("LWJGL debugging mode"));
    }

}
