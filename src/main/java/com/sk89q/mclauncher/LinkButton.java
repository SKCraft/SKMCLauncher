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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.Border;

public class LinkButton extends JButton {
    private static final long serialVersionUID = 8415038817435411359L;
    
    private static final Color LINK_COLOR = Color.blue;
    private static final Border LINK_BORDER = BorderFactory.createEmptyBorder(0, 0, 1, 0); 
    private static final Border HOVER_BORDER = BorderFactory.createMatteBorder(0, 0, 1, 0, LINK_COLOR);
    
    public LinkButton() {
        super();
        setupLink();
    }
    
    public LinkButton(Action a) {
        super(a);
        setupLink();
    }
    
    public LinkButton(Icon icon) {
        super(icon);
        setupLink();
    }
    
    public LinkButton(String text, Icon icon) {
        super(text, icon);
        setupLink();
    }
    
    public LinkButton(String text) {
        super(text);
        setupLink();
    }
    
    public void setupLink() {
        setBorder(LINK_BORDER); 
        setForeground(LINK_COLOR); 
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); 
        setFocusPainted(false); 
        setRequestFocusEnabled(false); 
        setContentAreaFilled(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                ((JComponent) e.getComponent()).setBorder(HOVER_BORDER);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ((JComponent) e.getComponent()).setBorder(LINK_BORDER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ((JComponent) e.getComponent()).setBorder(LINK_BORDER);
            }
        });
    }

}
