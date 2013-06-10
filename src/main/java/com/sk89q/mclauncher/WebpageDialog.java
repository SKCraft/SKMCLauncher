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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.sk89q.mclauncher.util.ActionListeners;

public class WebpageDialog extends JDialog {

    private static final long serialVersionUID = 2853690259765404630L;

    private final WebpagePanel panel;
    private boolean agreed = false;
    
    public WebpageDialog(Window owner, String title, WebpagePanel panel) {
        this(owner, title, panel, false);
    }
    
    public WebpageDialog(Window owner, String title, 
            WebpagePanel panel, boolean requestAgreement) {
        super(owner, title, ModalityType.DOCUMENT_MODAL);
        this.panel = panel;
        
        setResizable(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(500, 400);
        
        addComponents(requestAgreement);
        
        setLocationRelativeTo(null);
    }
    
    private void addComponents(boolean requestAgreement) {
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        container.add(panel, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        if (requestAgreement) {
            buttonsPanel.add(Box.createHorizontalGlue());
            JButton acceptButton = new JButton("I accept");
            buttonsPanel.add(acceptButton);
            acceptButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    agreed = true;
                    dispose();
                }
            });
            buttonsPanel.add(Box.createHorizontalStrut(6));
            JButton cancelButton = new JButton("Cancel");
            buttonsPanel.add(cancelButton);
            cancelButton.addActionListener(ActionListeners.dipose(this));
        } else {
            buttonsPanel.add(Box.createHorizontalGlue());
            JButton closeButton = new JButton("Close Window");
            buttonsPanel.add(closeButton);
            closeButton.addActionListener(ActionListeners.dipose(this));
        }
        add(buttonsPanel, BorderLayout.SOUTH);
        
        add(container, BorderLayout.CENTER);
    }
    
    public boolean hasAgreed() {
        return agreed;
    }

}
