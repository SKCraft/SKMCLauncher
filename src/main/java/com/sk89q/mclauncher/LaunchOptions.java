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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;

import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.config.Def;
import com.sk89q.mclauncher.config.Identity;
import com.sk89q.mclauncher.config.IdentityList;
import com.sk89q.mclauncher.config.LauncherOptions;
import com.sk89q.mclauncher.config.MinecraftJar;
import com.sk89q.mclauncher.util.PopupMouseAdapter;
import com.sk89q.mclauncher.util.SwingHelper;

public class LaunchOptions extends JPanel implements ListSelectionListener {
    
    private static final long serialVersionUID = -4971412532761639415L;
    
    private final LauncherFrame frame;
    private final LauncherOptions options;

    private GridBagConstraints fieldC;
    private GridBagConstraints labelC;
    private GridBagConstraints checkboxC;
    
    private JComboBox jarCombo;
    private JComboBox userText;
    private JTextField passText;
    private JCheckBox rememberPass;
    private JCheckBox forceUpdateCheck;
    private JCheckBox changeComponentsCheck;
    private JCheckBox playOfflineCheck;
    private JCheckBox showConsoleCheck;
    private JCheckBox autoConnectCheck;
    private LinkButton expandBtn;

    private String autoConnect;
    
    public LaunchOptions(LauncherFrame frame) {
        this.frame = frame;
        this.options = Launcher.getInstance().getOptions();
        
        createGridBagConstraints();
        addComponents();

        if (options.getSettings().getBool(Def.LAUNCHER_ALWAYS_MORE_OPTIONS, false)) {
            expandBtn.doClick();
        }
    }
    
    private void createGridBagConstraints() {
        fieldC = new GridBagConstraints();
        fieldC.fill = GridBagConstraints.HORIZONTAL;
        fieldC.weightx = 1.0;
        fieldC.gridwidth = GridBagConstraints.REMAINDER;
        fieldC.insets = new Insets(2, 1, 2, 1);
    
        labelC = (GridBagConstraints) fieldC.clone();
        labelC.weightx = 0.0;
        labelC.gridwidth = 1;
        labelC.insets = new Insets(1, 1, 1, 10);
    
        checkboxC = (GridBagConstraints) fieldC.clone();
        checkboxC.insets = new Insets(5, 2, 1, 2);
    }

    private void addComponents() {
        setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
    
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);
    
        final JLabel jarLabel = new JLabel("Active JAR:", SwingConstants.LEFT);
        JLabel userLabel = new JLabel("Username:", SwingConstants.LEFT);
        JLabel passLabel = new JLabel("Password:", SwingConstants.LEFT);
    
        jarCombo = new JComboBox();
        userText = new JComboBox();
        userText.setModel(options.getIdentities());
        userText.setEditable(true);
        passText = new JPasswordField(20);
        jarLabel.setLabelFor(jarCombo);
        userLabel.setLabelFor(userText);
        passLabel.setLabelFor(passText);
        layout.setConstraints(jarCombo, fieldC);
        layout.setConstraints(userText, fieldC);
        layout.setConstraints(passText, fieldC);
    
        rememberPass = new JCheckBox("Remember my password");
        rememberPass.setBorder(null);
    
        autoConnectCheck = new JCheckBox("Auto-connect");
        autoConnectCheck.setBorder(null);
    
        forceUpdateCheck = new JCheckBox("Force a game update");
        forceUpdateCheck.setBorder(null);
        
        changeComponentsCheck = new JCheckBox("Re-select install options (if any)");
        changeComponentsCheck.setBorder(null);
    
        playOfflineCheck = new JCheckBox("Play in offline mode");
        playOfflineCheck.setBorder(null);
    
        showConsoleCheck = new JCheckBox("Show messages and errors");
        showConsoleCheck.setBorder(null);
    
        expandBtn = new LinkButton("More options...");
        final JPanel expandContainer = new JPanel();
        expandContainer.setLayout(new BoxLayout(expandContainer,
                BoxLayout.X_AXIS));
        expandContainer.setBorder(null);
        expandContainer.add(expandBtn);
        expandContainer.add(Box.createHorizontalGlue());
    
        add(jarLabel, labelC);
        add(jarCombo, fieldC);
        add(userLabel, labelC);
        add(userText, fieldC);
        add(passLabel, labelC);
        add(passText, fieldC);
        add(rememberPass, checkboxC);
        add(autoConnectCheck, checkboxC);
        add(forceUpdateCheck, checkboxC);
        add(changeComponentsCheck, checkboxC);
        add(playOfflineCheck, checkboxC);
        add(showConsoleCheck, checkboxC);
        add(expandContainer, checkboxC);
    
        autoConnectCheck.setVisible(false);
        jarLabel.setVisible(false);
        jarCombo.setVisible(false);
        forceUpdateCheck.setVisible(false);
        changeComponentsCheck.setVisible(false);
        showConsoleCheck.setVisible(false);
        playOfflineCheck.setVisible(false);
    
        userText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadSavedPassword();
            }
        });
        
        loadSavedPassword();
    
        userText.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    frame.launch();
                }
            }
        });
    
        userText.getEditor().getEditorComponent().addMouseListener(new PopupMouseAdapter() {
            @Override
            protected void showPopup(MouseEvent e) {
                popupIdentityMenu(e.getComponent(), e.getX(), e.getY());
            }
        });
    
        passText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    frame.launch();
                }
            }
        });
        
        playOfflineCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean selected = ((JCheckBox) e.getSource()).isSelected();
                userText.setEnabled(!selected);
                passText.setEnabled(!selected);
                rememberPass.setEnabled(!selected);
            }
        });
    
        expandBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                expandContainer.setVisible(false);
                jarLabel.setVisible(true);
                jarCombo.setVisible(true);
                forceUpdateCheck.setVisible(true);
                changeComponentsCheck.setVisible(true);
                showConsoleCheck.setVisible(true);
                playOfflineCheck.setVisible(true);
                // registerAccount.setVisible(true);
            }
        });
    }

    /**
     * Pop the menu that appears when right clicking the username box.
     * 
     * @param component component to display from
     * @param x top left x
     * @param y top left y
     */
    private void popupIdentityMenu(Component component, int x, int y) {
        final IdentityList identities = options.getIdentities();
        
        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;

        final String username = getLoginId();

        if (identities.getPassword(username) != null) {
            menuItem = new JMenuItem("Forget '" + username + "' and its password");
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    identities.forget(username);
                    options.save();
                }
            });
            popup.add(menuItem);
        }

        menuItem = new JMenuItem("Forget all saved passwords...");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (JOptionPane
                        .showConfirmDialog(
                                frame,
                                "Are you sure that you want to forget all saved passwords?",
                                "Forget passwords", JOptionPane.YES_NO_OPTION) == 0) {
                    identities.forgetAll();
                    options.save();
                }
            }
        });
        popup.add(menuItem);

        popup.show(component, x, y);
    }

    private void loadSavedPassword() {
        Identity selected = (Identity) userText.getSelectedItem();
        
        if (selected == null) {
            passText.setText("");
            rememberPass.setSelected(true);
            return;
        }
        
        Identity saved = options.getIdentities().byId(selected.getId());

        if (saved != null) {
            rememberPass.setSelected(saved.getPassword() != null);
            passText.setText(saved.getPassword());
        } else {
            rememberPass.setSelected(true);
        }
    }
    
    public Identity getIdentity() {
        if (!hasLoginSet()) {
            return null;
        }
        return new Identity(getLoginId(), getPassword());
    }
    
    public String getLoginId() {
        Object object = userText.getSelectedItem();
        if (object == null) {
            return null;
        }
        String name = String.valueOf(object).trim();
        return !name.isEmpty() ? name : null;
    }

    public String getPassword() {
        String password = passText.getText().trim();
        return !password.isEmpty() ? password : null;
    }

    public void setLogin(String username, String password) {
        ((JTextComponent) userText.getEditor().getEditorComponent())
                .setText(username);
        if (passText != null) {
            passText.setText(password);
        }
    }

    public boolean shouldRememberPassword() {
        return rememberPass.isSelected();
    }

    public MinecraftJar getActiveJar() {
        return (MinecraftJar) jarCombo.getSelectedItem();
    }

    public String getAutoConnect() {
        return autoConnect;
    }

    public String getEffectiveAutoConnect() {
        return autoConnect;
    }

    public void setAutoConnect(String address) {
        this.autoConnect = address;

        if (address == null) {
            autoConnectCheck.setSelected(false);
            autoConnectCheck.setVisible(false);
        } else {
            autoConnectCheck.setText("Auto-connect to '" + address + "'");
            autoConnectCheck.setSelected(true);
            autoConnectCheck.setVisible(true);
        }
    }
    
    public boolean getShowConsole() {
        return showConsoleCheck.isSelected();
    }

    public void setShowConsole(boolean show) {
        showConsoleCheck.setSelected(show);
    }

    public boolean isPlayingOffline() {
        return playOfflineCheck.isSelected();
    }
    
    public void setPlayOffline(boolean playOffline) {
        playOfflineCheck.setSelected(playOffline);
    }

    public boolean isForcingUpdate() {
        return forceUpdateCheck.isSelected();
    }

    public boolean isForcingIncrementalUpdate() {
        return changeComponentsCheck.isSelected();
    }
    
    public void focusEmptyField() {
        if (getLoginId() == null) {
            userText.requestFocusInWindow();
        } else {
            passText.requestFocusInWindow();
        }
    }

    public boolean hasLoginSet() {
        return (getLoginId() != null && getPassword() != null);
    }

    public void setTestMode() {
        setShowConsole(true);
        if (options.getSettings().getBool(Def.FAST_TEST, false)) {
            setPlayOffline(true);
        }
    }

    /**
     * Check whether the username/password/etc. are set properly.
     * 
     * @return true if set properly
     */
    public boolean verifyAndNotify() {
        if (!playOfflineCheck.isSelected() && getLoginId() == null) {
            SwingHelper.showError(frame, "No username", "A username must be entered.");
            return false;
        }

        if (!playOfflineCheck.isSelected() && getPassword() == null) {
            SwingHelper.showError(frame, "No password", "A password must be entered.");
            return false;
        }
        
        return true;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        Configuration configuration = 
                (Configuration) ((JList) e.getSource()).getSelectedValue();
        jarCombo.setModel(configuration.getJars());
    }
    
}
