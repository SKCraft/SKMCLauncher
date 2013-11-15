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
import com.sk89q.skmcl.profile.SimpleProfile;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;

import static com.sk89q.skmcl.util.SharedLocale._;

@Log
public class CreateProfileDialog extends JDialog {

    private final CreateProfileDialog self = this;
    @Getter
    private final Launcher launcher;
    private Application application;
    private Version version = new LatestStable();

    private JTextField nameField;

    public CreateProfileDialog(Window owner, @NonNull Launcher launcher) {
        super(owner, ModalityType.DOCUMENT_MODAL);

        this.launcher = launcher;
        this.application = new Minecraft();

        application.setVersion(new LatestStable());

        setTitle(_("createProfile.title"));
        initComponents();
        pack();
        setMinimumSize(new Dimension(350, 0));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        FormPanel form = new FormPanel();
        nameField = new JTextField();
        final JButton versionButton = new JButton(version.toString());
        LinedBoxPanel buttons = new LinedBoxPanel(true);
        JButton cancelButton = new JButton(_("button.cancel"));
        JButton createButton = new JButton(_("button.create"));

        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.addRow(new JLabel(_("createProfile.profileName")), nameField);
        form.addRow(new JLabel(_("createProfile.version")), versionButton);

        buttons.addGlue();
        buttons.addElement(createButton);
        buttons.addElement(cancelButton);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tryCreateProfile();
            }
        });
        versionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VersionListDialog dialog = new VersionListDialog(
                        self, application, version);
                dialog.setVisible(true);
                version = dialog.getVersion();
                versionButton.setText(version.toString());
            }
        });
        cancelButton.addActionListener(ActionListeners.dispose(this));

        SwingHelper.focusLater(nameField);
    }

    private void tryCreateProfile() {
        String name = nameField.getText();
        name = name.trim();

        if (name.isEmpty()) {
            SwingHelper.showErrorDialog(
                    this, _("createProfile.emptyNameError"), _("invalidInput.title"));
            return;
        }

        SimpleProfile profile = new SimpleProfile();
        application.setVersion(version);
        profile.setApplication(application);
        profile.setName(name);
        try {
            getLauncher().getProfiles().add(profile);
            dispose();
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to create a profile", e);
            SwingHelper.showErrorDialog(this, _("createProfile.failedToCreateError"),
                    _("createProfile.title"), e);
        }
    }

}
