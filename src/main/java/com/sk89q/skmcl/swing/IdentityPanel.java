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
import com.sk89q.skmcl.session.Identity;
import lombok.Getter;

import javax.swing.*;

import java.util.List;

import static com.sk89q.skmcl.util.SharedLocale._;

public class IdentityPanel extends JPanel {

    @Getter
    private final JButton identityButton;
    private final MinecraftFacePanel facePanel;
    private ComboBoxModel model;

    public IdentityPanel() {
        this(null);
    }

    public IdentityPanel(ComboBoxModel model) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        identityButton = new JButton();
        facePanel = new MinecraftFacePanel();

        identityButton.setAlignmentY(BOTTOM_ALIGNMENT);
        facePanel.setAlignmentY(BOTTOM_ALIGNMENT);

        add(identityButton);
        add(Box.createHorizontalStrut(5));
        add(facePanel);

        updateSelected();
        setModel(model);
    }

    public ComboBoxModel getModel() {
        return model;
    }

    public void setModel(ComboBoxModel newModel) {
        this.model = newModel;
        updateSelected();
    }

    public void updateSelected() {
        Object item = model != null ? model.getSelectedItem() : null;

        if (item == null) {
            facePanel.setUsername(null);
            identityButton.setText(_("identityPanel.notLoggedIn"));
        } else if (item instanceof Account) {
            List<Identity> identities = ((Account) item).getIdentities();
            if (identities.size() > 0) {
                String username = identities.get(0).getName();
                facePanel.setUsername(username);
                identityButton.setText(_("identityPanel.hello", username));
            } else {
                identityButton.setText(_("identityPanel.loggedIn"));
            }
        } else {
            facePanel.setUsername(null);
            identityButton.setText(_("identityPanel.notLoggedIn"));
        }
    }

}
