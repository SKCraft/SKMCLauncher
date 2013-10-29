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

import javax.swing.*;
import java.awt.*;

public class FormPanel extends JPanel {

    private static final GridBagConstraints labelConstraints;
    private static final GridBagConstraints fieldConstraints;
    private static final GridBagConstraints wideFieldConstraints;

    private final GridBagLayout layout;

    static {
        fieldConstraints = new GridBagConstraints();
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
        fieldConstraints.insets = new Insets(2, 5, 2, 5);

        labelConstraints = (GridBagConstraints) fieldConstraints.clone();
        labelConstraints.weightx = 0.0;
        labelConstraints.gridwidth = 1;
        labelConstraints.insets = new Insets(1, 5, 1, 10);

        wideFieldConstraints = (GridBagConstraints) fieldConstraints.clone();
        wideFieldConstraints.insets = new Insets(5, 2, 1, 2);
    }

    public FormPanel() {
        setLayout(layout = new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public void addRow(Component label, Component component) {
        add(label);
        add(component);
        layout.setConstraints(label, labelConstraints);
        layout.setConstraints(component, fieldConstraints);
    }

    public void addRow(Component component) {
        add(component);
        layout.setConstraints(component, wideFieldConstraints);
    }

}
