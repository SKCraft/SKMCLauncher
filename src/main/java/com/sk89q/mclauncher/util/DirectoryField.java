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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

public class DirectoryField extends JPanel {
    
    private static final long serialVersionUID = 5706210803738919578L;
    
    private final JTextField textField;
    private final JButton browseButton;
    
    public DirectoryField() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        
        textField = new JTextField(30);
        textField.setMaximumSize(textField.getPreferredSize());
        add(textField);

        add(Box.createHorizontalStrut(3));
        
        browseButton = new JButton("Browse...");
        browseButton.setPreferredSize(new Dimension(
                browseButton.getPreferredSize().width,
                textField.getPreferredSize().height));
        add(browseButton);
        
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browse();
            }
        });
    }
    
    public JTextField getTextField() {
        return textField;
    }

    public JButton getBrowseButton() {
        return browseButton;
    }

    public void setPath(String path) {
        getTextField().setText(path);
    }

    public String getPath() {
        return getTextField().getText();
    }
    
    protected JFileChooser getFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) return true;
                return false;
            }

            @Override
            public String getDescription() {
                return "Directories";
            }
        });
        
        return chooser;
    }

    public void browse() {
        JFileChooser chooser = getFileChooser();
        File f = new File(getPath());
        if (f.exists() && f.isFile()) {
            f = f.getParentFile();
        }
        chooser.setCurrentDirectory(f);
        
        int returnVal = chooser.showOpenDialog(this);
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            setPath(chooser.getSelectedFile().getPath());
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        getTextField().setEnabled(enabled);
        getBrowseButton().setEnabled(enabled);
    }

}
