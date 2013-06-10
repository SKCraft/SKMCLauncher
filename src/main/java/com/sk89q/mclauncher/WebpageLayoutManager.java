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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

import javax.swing.JProgressBar;

public class WebpageLayoutManager implements LayoutManager {

    private static final int PROGRESS_WIDTH = 100;
    
    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        throw new UnsupportedOperationException("Can't remove things!");
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return new Dimension(0, 0);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(0, 0);
    }

    @Override
    public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets();
        int maxWidth = parent.getWidth() - (insets.left + insets.right);
        int maxHeight = parent.getHeight() - (insets.top + insets.bottom);
        
        int numComps = parent.getComponentCount();
        for (int i = 0 ; i < numComps ; i++) {
            Component comp = parent.getComponent(i);
            
            if (comp instanceof JProgressBar) {
                Dimension size = comp.getPreferredSize();
                comp.setLocation((parent.getWidth() - PROGRESS_WIDTH) / 2,
                        (int) (parent.getHeight() / 2.0 - size.height / 2.0));
                comp.setSize(PROGRESS_WIDTH,
                        (int) comp.getPreferredSize().height);
            } else {
                comp.setLocation(insets.left, insets.top);
                comp.setSize(maxWidth, maxHeight);
            }
        }
    }

}
