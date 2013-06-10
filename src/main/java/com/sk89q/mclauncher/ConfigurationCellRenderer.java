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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import com.sk89q.mclauncher.config.Configuration;

public class ConfigurationCellRenderer implements ListCellRenderer {
    
    private static final int PAD = 5;
    private static BufferedImage defaultIcon;
    
    static {
        try {
            InputStream in = Launcher.class
                    .getResourceAsStream("/resources/config_icon.png");
            if (in != null) {
                defaultIcon = ImageIO.read(in);
            }
        } catch (IOException e) {
        }
    }

    @Override
    public Component getListCellRendererComponent(final JList list, final Object value,
            int index, final boolean isSelected, boolean cellHasFocus) {
        final Configuration configuration = (Configuration) value;
        
        JIconPanel panel = new JIconPanel(configuration.getIcon());
        panel.setLayout(new GridLayout(2, 1, 0, 1));
        panel.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(PAD, PAD * 2 + 32, PAD, PAD));
        
        JLabel titleLabel = new JLabel();
        titleLabel.setText(configuration.getName());
        titleLabel.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
        Font font = titleLabel.getFont();
        font = font.deriveFont((float) (font.getSize() * 1.3)).deriveFont(Font.BOLD);
        titleLabel.setFont(font);
        panel.add(titleLabel);
        
        String infoText;
        if (configuration.isUsingDefaultPath()) {
            infoText = "Normal installation";
        } else if (configuration.getUpdateUrl() != null) {
            infoText = "via " + configuration.getUpdateUrl().getHost();
        } else {
            infoText = "Custom installation";
        }
        
        JLabel infoLabel = new JLabel();
        infoLabel.setText(infoText);
        Color color = isSelected ? list.getSelectionForeground() : list.getForeground();
        infoLabel.setForeground(color);
        panel.add(infoLabel);
        
        return panel;
    }
    
    private static class JIconPanel extends JPanel {
        
        private static final long serialVersionUID = 6455230127195332368L;
        private BufferedImage icon;
        
        public JIconPanel(BufferedImage icon) {
            this.icon = icon;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Dimension dim = getPreferredSize();
            if (icon != null) {
                g.drawImage(icon, PAD, (int) ((dim.getHeight() - 32) / 2), null);    
            } else if (defaultIcon != null) {
                g.drawImage(defaultIcon, PAD, (int) ((dim.getHeight() - 32) / 2), null);    
            }
        }
        
    }

}
