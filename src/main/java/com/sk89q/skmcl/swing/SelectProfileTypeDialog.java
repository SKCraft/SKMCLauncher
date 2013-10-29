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

package com.sk89q.skmcl.swing;

import com.sk89q.mclauncher.util.ActionListeners;
import com.sk89q.skmcl.Launcher;
import lombok.Getter;
import lombok.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.sk89q.skmcl.util.SharedLocale._;

public class SelectProfileTypeDialog extends JDialog {

    @Getter
    private final Launcher launcher;

    public SelectProfileTypeDialog(Window owner, @NonNull Launcher launcher) {
        super(owner, ModalityType.DOCUMENT_MODAL);

        this.launcher = launcher;

        setTitle(_("createProfile.title"));
        initComponents();
        pack();
        setMinimumSize(new Dimension(200, 0));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        FormPanel form = new FormPanel();
        JButton regularButton = new JButton(_("profileType.regularProfile"));
        JButton urlButton = new JButton(_("profileType.urlModpackProfile"));
        JButton folderButton = new JButton(_("profileType.dirModpackProfile"));
        JButton cancelButton = new JButton(_("button.cancel"));

        form.addRow(new JLabel("<html>" + _("profileType.label")));
        form.addRow(regularButton);
        form.addRow(urlButton);
        form.addRow(folderButton);
        form.addRow(Box.createVerticalStrut(16));
        form.addRow(cancelButton);

        add(form, BorderLayout.CENTER);

        regularButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                launcher.showCreateRegularProfile(getOwner());
            }
        });
        cancelButton.addActionListener(ActionListeners.dispose(this));

        SwingHelper.focusLater(regularButton);
    }

}
