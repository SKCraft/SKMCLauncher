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

import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public class ButtonsPanel extends JPanel {
    
    private static final long serialVersionUID = 8289993126126654728L;
    
    public ButtonsPanel(int top, int left, int bottom, int right) {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
    }

    public ButtonsPanel gap() {
        add(Box.createHorizontalGlue());
        return this;
    }

    public ButtonsPanel spacer() {
        add(Box.createHorizontalStrut(6));
        return this;
    }

    public ButtonsPanel button(String title, ActionListener listener) {
        JButton button = addButton(title);
        button.addActionListener(listener);
        return this;
    }

    public JButton addButton(String title) {
        JButton button = new JButton(title);
        add(button);
        return button;
    }

}
