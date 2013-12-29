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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.sk89q.skmcl.Launcher;
import com.sk89q.skmcl.concurrent.AbstractWorker;
import com.sk89q.skmcl.concurrent.ExecutorWorkerService;
import com.sk89q.skmcl.concurrent.SwingProgressObserver;
import com.sk89q.skmcl.profile.Profile;
import lombok.Getter;
import lombok.NonNull;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Executors;

import static com.sk89q.skmcl.util.SharedLocale._;

public class LauncherFrame extends JFrame implements ListDataListener {

    private final Window self = this;
    @Getter
    private final Launcher launcher;
    private final ListeningExecutorService executorService =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    private final ExecutorWorkerService executor = new ExecutorWorkerService(executorService);

    private JList profilesList;
    //private IdentityPanel identityPanel;

    public LauncherFrame(@NonNull Launcher launcher) {
        this.launcher = launcher;
        new SwingProgressObserver(this, executor);

        setTitle(_("launcher.title"));
        SwingHelper.setIconImage(this, "/resources/icon.png");
        initComponents();
        initMenu();
        setResizable(true);
        setMinimumSize(new Dimension(300, 200));
        setSize(new Dimension(800, 500));
        setLocationRelativeTo(null);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                removeListeners();
                dispose();
                System.exit(0);
            }
        });

        executorService.submit(new AbstractWorker<Object>() {
            @Override
            protected void run() throws Exception {
                getLauncher().getProfiles().load();
            }
        });
    }

    public void removeListeners() {
        profilesList.setModel(new DefaultListModel());
        //identityPanel.setModel(null);
    }

    private void initComponents() {
        JPanel contentPanel = new JPanel();
        JPanel leftPanel = new JPanel();
        JPanel searchPanel = new JPanel();
        //JTextField filterText = new JTextField();
        LinedBoxPanel bottomPanel = new LinedBoxPanel(true);
        JSplitPane splitPane;
        profilesList = new JList(getLauncher().getProfiles());
        ProfilePanel profilePanel = new ProfilePanel(profilesList);
        JButton newProfileButton = new JButton(_("launcher.createProfile"));
        JButton installModPackButton = new JButton(_("launcher.installModPack"));
        //identityPanel = new IdentityPanel(getLauncher().getIdentities());

        //filterText.setMargin(new Insets(2, 2, 2, 2));
        //TextPrompt prompt = new TextPrompt(_("launcher.filterProfilesPlaceholder"), filterText);
        //prompt.changeAlpha(0.5f);
        //prompt.changeStyle(Font.ITALIC);
        profilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        profilesList.setCellRenderer(new ProfileListCellRenderer());
        profilesList.setFixedCellHeight(20);
        SelectionKeeper.attach(profilesList);

        newProfileButton.setAlignmentY(BOTTOM_ALIGNMENT);
        installModPackButton.setAlignmentY(BOTTOM_ALIGNMENT);
        //identityPanel.setAlignmentY(BOTTOM_ALIGNMENT);

        leftPanel.setLayout(new BorderLayout());
        searchPanel.setLayout(new BorderLayout());
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        //searchPanel.add(filterText);
        bottomPanel.addElement(newProfileButton);
        bottomPanel.addElement(installModPackButton);
        bottomPanel.addGlue();
        //bottomPanel.addElement(identityPanel);
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
                getLauncher().launchApplication(self, executor,
                        (Profile) profilesList.getSelectedValue());
            }
        });

        profilesList.addMouseListener(
                new DoubleClickToButtonAdapter(
                        profilePanel.getLaunchButton()));

        newProfileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showCreateProfile();
            }
        });

        /*identityPanel.getIdentityButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showLogin();
            }
        });*/

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

    private void showCreateProfile() {
        CreateProfileDialog dialog = new CreateProfileDialog(this, getLauncher());
        dialog.setVisible(true);
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
        selectDefaultProfile();
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        selectDefaultProfile();
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        selectDefaultProfile();
    }
}
