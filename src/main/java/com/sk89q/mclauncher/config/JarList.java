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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class JarList implements ComboBoxModel {

    private transient EventListenerList listenerList = new EventListenerList();
    
    private final File dir;
    private DefaultJar defaultJar;
    private MinecraftJar selected;
    private List<MinecraftJar> jars = new ArrayList<MinecraftJar>();
    
    public JarList(File dir) {
        this.dir = dir;
        update();
    }
    
    public void update() {
        List<MinecraftJar> jars = new ArrayList<MinecraftJar>();
        
        // The default JAR
        jars.add(defaultJar = new DefaultJar(new File(dir, "minecraft.jar")));
        
        File[] files = dir.listFiles();
        
        if (files != null) {
            Arrays.sort(files);
            for (File f : files) {
                String name = f.getName();
                
                if (name.matches("^[^\\/:;]+\\.jar$") && !name.equalsIgnoreCase("jinput.jar")
                        && !name.equalsIgnoreCase("lwjgl.jar")
                        && !name.equalsIgnoreCase("lwjgl_util.jar")) {
                    jars.add(new MinecraftJar(f));
                }
            }
        }
        
        fireListDataEvent(new ListDataEvent(
                this, ListDataEvent.CONTENTS_CHANGED, 0, getSize()));

        selected = defaultJar;
        this.jars = jars;
    }

    @Override
    public int getSize() {
        return jars.size();
    }

    @Override
    public Object getElementAt(int index) {
        return jars.get(index);
    }

    @Override
    public void setSelectedItem(Object item) {
        if (item == null) {
            selected = defaultJar;
        } else if (item instanceof MinecraftJar) {
            selected = (MinecraftJar) item;
        } else if (item instanceof String) {
            for (MinecraftJar jar : jars) {
                if (jar.getName().equals(item)) {
                    selected = jar;
                }
            }
        }

        fireListDataEvent(new ListDataEvent(
                this, ListDataEvent.CONTENTS_CHANGED, 0, getSize()));
    }

    @Override
    public Object getSelectedItem() {
        return selected;
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listenerList.add(ListDataListener.class, l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
    }
    
    private void fireListDataEvent(final ListDataEvent event) {
        final Object[] listeners = listenerList.getListenerList();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (int i = listeners.length - 2; i >= 0; i -= 2) {
                    if (listeners[i] == ListDataListener.class) {
                        ((ListDataListener) listeners[i + 1]).contentsChanged(event);
                    }
                }
            }
        });
    }

}
