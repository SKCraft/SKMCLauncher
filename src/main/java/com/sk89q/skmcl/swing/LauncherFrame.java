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
import com.sk89q.skmcl.profile.Profile;
import com.sk89q.skmcl.worker.Task;
import com.sk89q.skmcl.worker.Worker;
import lombok.Getter;
import lombok.NonNull;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.sk89q.skmcl.util.SharedLocale._;

public class LauncherFrame extends JFrame implements ListDataListener {

    private final Window self = this;
    @Getter
    private final Launcher launcher;
    private final Worker worker = new Worker(this);

    private JList profilesList;

    public LauncherFrame(@NonNull Launcher launcher) {
        this.launcher = launcher;

        setTitle(_("launcher.title"));
        SwingHelper.setIconImage(this, "/resources/icon.png");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        initComponents();
        initMenu();
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
        JPanel contentPanel = new JPanel();
        JPanel leftPanel = new JPanel();
        JPanel searchPanel = new JPanel();
        JTextField filterText = new JTextField();
        LinedBoxPanel bottomPanel = new LinedBoxPanel(true);
        JSplitPane splitPane;
        profilesList = new JList(getLauncher().getProfiles());
        ProfilePanel profilePanel = new ProfilePanel(profilesList);
        JButton newProfileButton = new JButton(_("launcher.createProfile"));
        JButton installModPackButton = new JButton(_("launcher.installModPack"));

        filterText.setMargin(new Insets(2, 2, 2, 2));
        TextPrompt prompt = new TextPrompt(_("launcher.filterProfilesPlaceholder"), filterText);
        prompt.changeAlpha(0.5f);
        prompt.changeStyle(Font.ITALIC);
        profilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        profilesList.setCellRenderer(new ProfileListCellRenderer());
        profilesList.setFixedCellHeight(20);
        SelectionKeeper.attach(profilesList);

        leftPanel.setLayout(new BorderLayout());
        searchPanel.setLayout(new BorderLayout());
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));

        searchPanel.add(filterText);
        bottomPanel.addElement(newProfileButton);
        bottomPanel.addElement(installModPackButton);
        leftPanel.add(searchPanel, BorderLayout.NORTH);
        leftPanel.add(profilesList, BorderLayout.CENTER);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftPanel,
                profilePanel);
        splitPane.setDividerLocation(200);
        contentPanel.add(splitPane, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(contentPanel, BorderLayout.CENTER);

        profilesList.getModel().addListDataListener(this);

        profilePanel.getLaunchButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getLauncher().launchApplication(self, worker,
                        (Profile) profilesList.getSelectedValue());
            }
        });

        newProfileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launcher.showCreateProfile(self);
            }
        });

        SwingHelper.focusLater(profilePanel.getLaunchButton());
    }

    private void initMenu() {
        JMenuBar menuBar;

        menuBar = new JMenuBar();
        menuBar.add(new JMenu(_("launcher.menu.launcher")));
        menuBar.add(new JMenu(_("launcher.menu.profiles")));
        menuBar.add(new JMenu(_("launcher.menu.help")));

        menuBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setJMenuBar(menuBar);
    }

    private void selectDefaultProfile() {
        ListModel model = profilesList.getModel();
        if (profilesList.getSelectedValue() == null && model.getSize() > 0) {
            profilesList.setSelectedValue(model.getElementAt(0), true);
            model.removeListDataListener(this);
        }
    }

    public void intervalAdded(ListDataEvent e) {
        selectDefaultProfile();
    }

    public void intervalRemoved(ListDataEvent e) {
        selectDefaultProfile();
    }

    public void contentsChanged(ListDataEvent e) {
        selectDefaultProfile();
    }
}
