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

package com.sk89q.mclauncher.launch;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.sk89q.mclauncher.Launcher;

class GameFrame extends JFrame {
    
    private static final Logger logger = Logger.getLogger(GameFrame.class.getCanonicalName());
    
    private static final long serialVersionUID = 5499648340202625650L;
    private JPanel wrapper;
    private Applet applet;

    GameFrame(Dimension dim) {
        setTitle("Minecraft");
        setBackground(Color.BLACK);
        
        try {
            InputStream in = Launcher.class.getResourceAsStream("/resources/icon.png");
            if (in != null) {
                setIconImage(ImageIO.read(in));
            }
        } catch (IOException e) {
        }
        
        wrapper = new JPanel();
        wrapper.setBackground(Color.BLACK);
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(dim != null ? dim : new Dimension(854, 480));
        wrapper.setLayout(new BorderLayout());
        add(wrapper, BorderLayout.CENTER);
        
        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                if (applet != null) {
                    applet.stop();
                    applet.destroy();
                }

                System.exit(0);
            }
        });
    }

    public void start(Applet applet) {
        logger.info("Starting " + applet.getClass().getCanonicalName());
        
        applet.init();
        wrapper.add(applet, BorderLayout.CENTER);
        validate();
        applet.start();
    }
}
