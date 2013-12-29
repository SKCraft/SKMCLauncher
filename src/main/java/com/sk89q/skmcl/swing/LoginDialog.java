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

import com.sk89q.skmcl.session.Account;
import com.sk89q.skmcl.session.AccountList;
import com.sk89q.skmcl.util.Persistence;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import static com.sk89q.skmcl.util.SharedLocale._;

public abstract class LoginDialog extends JDialog {

    @Getter
    private final AccountList accounts;

    private JComboBox idCombo;
    private final JPasswordField passwordText = new JPasswordField();
    private final JCheckBox rememberIdCheck = new JCheckBox(_("loginDialog.rememberId"));
    private final JCheckBox rememberPassCheck = new JCheckBox(_("loginDialog.rememberPassword"));
    private final JButton loginButton = new JButton(_("loginDialog.login"));
    private final JButton forgotLogin = new JButton(_("loginDialog.forgotLogin"));
    private final JButton cancelButton = new JButton(_("button.cancel"));

    public LoginDialog(Window owner, AccountList accounts) {
        super(owner, ModalityType.DOCUMENT_MODAL);

        this.accounts = accounts;

        setTitle(_("loginDialog.title"));
        initComponents();
        bindListeners();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(420, 0));
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        idCombo = new JComboBox(getAccounts());
        updateSelection();

        FormPanel formPanel = new FormPanel();
        LinedBoxPanel buttonsPanel = new LinedBoxPanel(true);

        rememberIdCheck.setBorder(BorderFactory.createEmptyBorder());
        rememberPassCheck.setBorder(BorderFactory.createEmptyBorder());
        idCombo.setEditable(true);
        idCombo.getEditor().selectAll();

        formPanel.addRow(new JLabel(_("loginDialog.id")), idCombo);
        formPanel.addRow(new JLabel(_("loginDialog.password")), passwordText);
        formPanel.addRow(new JLabel(), rememberIdCheck);
        formPanel.addRow(new JLabel(), rememberPassCheck);
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(26, 13, 13, 13));

        buttonsPanel.addElement(forgotLogin);
        buttonsPanel.addGlue();
        buttonsPanel.addElement(loginButton);
        buttonsPanel.addElement(cancelButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(loginButton);
    }

    private void bindListeners() {
        idCombo.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);
        passwordText.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);

        idCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSelection();
            }
        });

        forgotLogin.addActionListener(ActionListeners.openURL(forgotLogin, "https://minecraft.net/resetpassword"));

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                prepareLogin();
            }
        });

        cancelButton.addActionListener(ActionListeners.dispose(this));

        rememberPassCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (rememberPassCheck.isSelected()) {
                    rememberIdCheck.setSelected(true);
                }
            }
        });

        rememberIdCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!rememberIdCheck.isSelected()) {
                    rememberPassCheck.setSelected(false);
                }
            }
        });
    }

    private void updateSelection() {
        Object selected = idCombo.getSelectedItem();

        if (selected != null && selected instanceof Account) {
            Account account = (Account) selected;
            String password = account.getPassword();

            rememberIdCheck.setSelected(true);
            if (password != null) {
                rememberPassCheck.setSelected(true);
                passwordText.setText(password);
            } else {
                rememberPassCheck.setSelected(false);
                passwordText.setText("");
            }
        } else {
            passwordText.setText("");
            rememberIdCheck.setSelected(true);
            rememberPassCheck.setSelected(false);
        }
    }

    private void prepareLogin() {
        Object selected = idCombo.getSelectedItem();

        if (selected != null && selected instanceof Account) {
            Account account = (Account) selected;
            String password = passwordText.getText();

            if (password == null || password.isEmpty()) {
                SwingHelper.showErrorDialog(this, _("loginDialog.missingPassword"), _("errors.errorTitle"));
            } else {
                if (rememberPassCheck.isSelected()) {
                    account.setPassword(password);
                } else {
                    account.setPassword(null);
                }

                if (rememberIdCheck.isSelected()) {
                    accounts.add(account);
                } else {
                    accounts.remove(account);
                }

                account.setLastUsed(new Date());

                Persistence.commitAndForget(accounts);

                attemptLogin(account, password);
            }
        } else {
            SwingHelper.showErrorDialog(this, _("loginDialog.missingId"), _("errors.errorTitle"));
        }
    }

    protected abstract void attemptLogin(Account account, String password);

}
