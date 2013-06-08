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

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class ProgressDialog extends JDialog implements ProgressListener {
    
    private static final long serialVersionUID = 7258236630228982935L;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private Task task;
    private JButton cancelButton;
    
    public ProgressDialog(Window owner, final Task task, String title) {
        super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);

        this.task = task;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                tryCancelling();
            }
        });
        
        buildUI();
        pack();
        setResizable(false);
        setSize(500, getHeight());
        setLocationRelativeTo(owner);
    }
    
    private void tryCancelling() {
        Boolean res = task.cancel();
        if (res == null) {
            cancelButton.setEnabled(true);
            progressBar.setIndeterminate(true);
            statusLabel.setText("Cancelling...");
        } else if (res) {
            dispose();
        }
    }
        
    private void buildUI() {
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(1000);
        buttonPanel.add(progressBar);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        cancelButton = new JButton("Cancel");
        buttonPanel.add(cancelButton);
        container.add(buttonPanel);

        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.LINE_AXIS));
        statusLabel = new JLabel("Initializing...", SwingConstants.LEADING);
        statusPanel.add(statusLabel);
        statusPanel.add(Box.createHorizontalGlue());
        container.add(statusPanel);
        
        add(container); 
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tryCancelling();
            }
        });
        
        pack();
    }

    @Override
    public void titleChanged(final TitleChangeEvent event) {
        final ProgressDialog self = this;
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                self.setTitle(event.getMessage());
            }
        });
    }

    @Override
    public void statusChanged(final StatusChangeEvent event) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                statusLabel.setText(event.getMessage());
            }
        });
    }

    @Override
    public void valueChanged(final ValueChangeEvent event) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                double value = event.getNewValue();
                if (value < 0) {
                    progressBar.setIndeterminate(true);
                } else {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue((int) (value * 1000));
                }
            }
        });
    }

    @Override
    public void completed(EventObject eventObject) {
        final ProgressDialog self = this;
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                self.dispose();
            }
        });
    }
    
}
