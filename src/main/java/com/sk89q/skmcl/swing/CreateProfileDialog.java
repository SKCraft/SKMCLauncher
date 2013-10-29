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

import com.sk89q.skmcl.Launcher;
import com.sk89q.skmcl.application.Application;
import com.sk89q.skmcl.application.LatestStable;
import com.sk89q.skmcl.application.Version;
import com.sk89q.skmcl.minecraft.Minecraft;
import lombok.Getter;
import lombok.NonNull;

import javax.swing.*;
import java.awt.*;

import static com.sk89q.skmcl.util.SharedLocale._;

public class CreateProfileDialog extends JDialog {

    @Getter
    private final Launcher launcher;
    private Application application = new Minecraft();
    private Version version = new LatestStable();

    public CreateProfileDialog(Window owner, @NonNull Launcher launcher) {
        super(owner, ModalityType.DOCUMENT_MODAL);

        this.launcher = launcher;

        setTitle(_("createProfile.title"));
        initComponents();
        pack();
        setMinimumSize(new Dimension(350, 0));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        FormPanel form = new FormPanel();
        JTextField nameField = new JTextField();
        JButton versionButton = new JButton(version.getName());
        LinedBoxPanel buttons = new LinedBoxPanel(true);
        JButton cancelButton = new JButton(_("button.cancel"));
        JButton createButton = new JButton(_("button.create"));

        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.addRow(new JLabel(_("createProfile.profileName")), nameField);
        form.addRow(new JLabel(_("createProfile.version")), versionButton);

        buttons.addGlue();
        buttons.addElement(cancelButton);
        buttons.addElement(createButton);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        cancelButton.addActionListener(ActionListeners.dispose(this));

        SwingHelper.focusLater(nameField);
    }

}
