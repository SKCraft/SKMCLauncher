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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.sk89q.mclauncher.config.LauncherOptions;
import com.sk89q.mclauncher.util.ActionListeners;
import com.sk89q.mclauncher.util.UIUtil;

public class InstallFromURLDialog extends JDialog {

    private static final long serialVersionUID = 3904227517893227045L;

    private LauncherOptions options;
    
    private JTextField urlText;
    private TaskWorker worker = new TaskWorker();

    public InstallFromURLDialog(LauncherFrame owner, LauncherOptions options) {
        super(owner, "Install From URL", true);
        
        this.options = options;

        setResizable(false);
        buildUI();
        pack();
        setLocationRelativeTo(owner);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }
    
    private void startInstall(String url) {
        if (worker.isAlive())
            return;

        worker = Task.startWorker(this, 
                new InstallFromURLTask(this, options, url));
    }

    /**
     * Build the UI.
     */
    private void buildUI() {
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        container.setLayout(new BorderLayout(3, 3));
        
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
        fieldConstraints.insets = new Insets(2, 1, 2, 1);
        
        GridBagConstraints labelConstraints = (GridBagConstraints) fieldConstraints.clone();
        labelConstraints.weightx = 0.0;
        labelConstraints.gridwidth = 1;
        labelConstraints.insets = new Insets(1, 1, 1, 10);
        
        GridBagConstraints fullFieldConstraints = (GridBagConstraints) fieldConstraints.clone();
        fullFieldConstraints.insets = new Insets(5, 2, 1, 2);
        
        JLabel urlLabel = new JLabel("URL to Install:");
        panel.add(urlLabel, labelConstraints);
        urlText = new JTextField(getURLFromClipboard());
        urlText.selectAll();
        urlText.setPreferredSize(new Dimension(300, (int) urlText.getPreferredSize().getHeight()));
        panel.add(urlText, fullFieldConstraints);
        
        container.add(panel, BorderLayout.CENTER);

        JLabel infoLabel = new JLabel(
                "<html>Copy and paste the URL that you have been given in order to install the mod pack.");
        container.add(infoLabel, BorderLayout.NORTH);
        
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS));
        LinkButton createButton = new LinkButton("Build your own package...");
        JButton okBtn = new JButton("Install");
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(70, (int) cancelBtn.getPreferredSize().getHeight()));
        okBtn.setPreferredSize(cancelBtn.getPreferredSize());
        buttonsPanel.add(createButton);
        buttonsPanel.add(Box.createHorizontalGlue());
        buttonsPanel.add(okBtn);
        buttonsPanel.add(Box.createHorizontalStrut(6));
        buttonsPanel.add(cancelBtn);
        container.add(buttonsPanel, BorderLayout.SOUTH);
        
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openBuilder();
            }
        });

        okBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = urlText.getText();
                if (!text.isEmpty()) {
                    startInstall(text);
                }
                // Need error dialog
            }
        });
        
        cancelBtn.addActionListener(ActionListeners.dipose(this));
        
        urlText.requestFocus();
        
        add(container, BorderLayout.CENTER);
    }
    
    private void openBuilder() {
        try {
            Class<?> cls = Class.forName("com.sk89q.lpbuilder.UpdateBuilderGUI");
            if (cls != null) {
                final JFrame frame = (JFrame) cls.newInstance();
                frame.setVisible(true);
                dispose();
                
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        frame.requestFocus();
                    }
                });
            } else {
                throw new Exception();
            }
        } catch (Throwable t) {
            UIUtil.showError(this, "Error", "Couldn't open builder GUI.");
        }
    }
    
    private String getURLFromClipboard() {
        try {
            String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            if (data != null && data.toLowerCase().matches("^https?://.*$")) {
                return data;
            }
        } catch (HeadlessException e) {
        } catch (UnsupportedFlavorException e) {
        } catch (IOException e) {
        }
        
        return "";
    }

}
