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
import com.sk89q.skmcl.session.IdentityManagerModel;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.sk89q.skmcl.util.SharedLocale._;

public class LoginDialog extends JDialog {

    private final Launcher launcher;
    private final IdentityManagerModel selectedIdentity;

    public LoginDialog(Window owner, Launcher launcher, IdentityManagerModel selectedIdentity) {
        super(owner, ModalityType.DOCUMENT_MODAL);

        this.launcher = launcher;
        this.selectedIdentity = selectedIdentity;

        setTitle(_("loginDialog.title"));
        initComponents();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(420, 0));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        final IdentityManagerModel tempSelectedIdentity = selectedIdentity.clone();

        FormPanel formPanel = new FormPanel();
        JComboBox idCombo = new JComboBox(tempSelectedIdentity);
        JPasswordField passwordText = new JPasswordField();
        LinedBoxPanel buttonsPanel = new LinedBoxPanel(true);
        JCheckBox rememberCheck = new JCheckBox(_("loginDialog.rememberAccount"));
        JButton loginButton = new JButton(_("loginDialog.login"));
        JButton forgotLogin = new JButton(_("loginDialog.forgotLogin"));
        JButton cancelButton = new JButton(_("button.cancel"));

        rememberCheck.setBorder(BorderFactory.createEmptyBorder());
        idCombo.setEditable(true);
        idCombo.getEditor().selectAll();

        formPanel.addRow(new JLabel(_("loginDialog.id")), idCombo);
        formPanel.addRow(new JLabel(_("loginDialog.password")), passwordText);
        formPanel.addRow(new JLabel(), rememberCheck);
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(26, 13, 13, 13));

        buttonsPanel.addElement(forgotLogin);
        buttonsPanel.addGlue();
        buttonsPanel.addElement(loginButton);
        buttonsPanel.addElement(cancelButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

        idCombo.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);
        passwordText.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);
        getRootPane().setDefaultButton(loginButton);

        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedIdentity.setSelectedItem(tempSelectedIdentity.getSelectedItem());
                dispose();
            }
        });

        cancelButton.addActionListener(ActionListeners.dispose(this));
    }

}
