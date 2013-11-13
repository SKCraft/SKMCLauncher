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
import com.sk89q.skmcl.util.Task;
import com.sk89q.skmcl.util.Worker;
import lombok.Getter;
import lombok.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.sk89q.skmcl.util.SharedLocale._;

public class LauncherFrame extends JFrame {

    private final Window window = this;
    @Getter
    private final Launcher launcher;
    private final Worker worker = new Worker(this);

    public LauncherFrame(@NonNull Launcher launcher) {
        this.launcher = launcher;

        setTitle(_("launcher.title"));
        SwingHelper.setIconImage(this, "/resources/icon.png");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        initComponents();
        setResizable(true);
        setMinimumSize(new Dimension(300, 200));
        setSize(new Dimension(800, 500));
        setLocationRelativeTo(null);

        worker.submit(new Task<Object>() {
            @Override
            protected void run() throws Exception {
                getLauncher().getProfiles().load();
            }

            @Override
            public String getLocalizedTitle() {
                return _("launcher.loadingProfiles");
            }
        });
    }

    private void initComponents() {
        LinedBoxPanel bottomPanel;
        JList profilesList;
        JButton launchButton = new JButton(_("launcher.launch"));
        JButton newProfileButton = new JButton(_("launcher.createProfile"));
        JButton optionsButton = new JButton(_("launcher.options"));

        bottomPanel = new LinedBoxPanel(true).fullyPadded();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));

        profilesList = new JList(getLauncher().getProfiles());
        launchButton.setFont(launchButton.getFont().deriveFont(Font.BOLD));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, profilesList, new JLabel("Something will go here."));
        splitPane.setDividerLocation(200);
        add(splitPane, BorderLayout.CENTER);

        bottomPanel.addElement(newProfileButton);
        bottomPanel.addGlue();
        bottomPanel.addElement(optionsButton);
        bottomPanel.addElement(launchButton);
        add(bottomPanel, BorderLayout.NORTH);

        newProfileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launcher.showCreateProfile(window);
            }
        });

        SwingHelper.focusLater(launchButton);
    }

}
