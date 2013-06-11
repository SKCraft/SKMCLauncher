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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.config.Constants;
import com.sk89q.mclauncher.config.Def;
import com.sk89q.mclauncher.config.ServerEntry;
import com.sk89q.mclauncher.config.ServerList;
import com.sk89q.mclauncher.util.ActionListeners;
import com.sk89q.mclauncher.util.SwingHelper;

/**
 * The classic view with a list of configurations on the left and news on
 * the right side.
 */
public class ClassicView extends LauncherView implements ListSelectionListener {

    private static final long serialVersionUID = -2985136389385217242L;
    private static final int PAD = 12;

    private JList configurationList;
    private JButton playBtn;
    private WebpagePanel newsPanel;
    
    public ClassicView(LauncherFrame frame, final LaunchOptions launchOptions) {
        super(frame, launchOptions);
        
        addComponents();
        
        // Select the initial configuration
        ListModel model = configurationList.getModel();
        Configuration startupConfiguration = options.getConfigurations()
                .getStartupConfiguration();
        if (configurationList.getSelectedValue() != startupConfiguration) {
            for (int i = 0; i < model.getSize(); i++) {
                if (model.getElementAt(i) == startupConfiguration) {
                    configurationList.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Focus initial item
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (launchOptions.hasLoginSet()) {
                    playBtn.requestFocusInWindow();
                } else {
                    launchOptions.focusEmptyField();
                }
            }
        });
    }

    private void addComponents() {
        setLayout(new BorderLayout(0, 0));
        
        boolean hideNews = options.getSettings().getBool(Def.LAUNCHER_HIDE_NEWS, false);
        if (!hideNews) {
            boolean lazyLoad = options.getSettings().getBool(Def.LAUNCHER_NO_NEWS, false);
            newsPanel = new WebpagePanel(lazyLoad);
            newsPanel.setBorder(BorderFactory.createEmptyBorder(PAD, 0, PAD, PAD));
            add(newsPanel, BorderLayout.CENTER);
        }
        
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        if (!hideNews) {
            add(leftPanel, BorderLayout.LINE_START);
        } else {
            add(leftPanel, BorderLayout.CENTER);
        }

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(1, 3, 3, 0));
        playBtn = new JButton("Play");
        final JButton optionsBtn = new JButton("Options...");
        JButton addonsBtn = new JButton("Addons...");
        buttonsPanel.add(playBtn);
        buttonsPanel.add(addonsBtn);
        buttonsPanel.add(optionsBtn);

        JPanel root = new JPanel();
        root.setBorder(BorderFactory.createEmptyBorder(0, PAD, PAD, PAD));
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.add(launchOptions);
        root.add(buttonsPanel);
        leftPanel.add(root, BorderLayout.SOUTH);

        JPanel configurationsPanel = new JPanel();
        configurationsPanel.setLayout(new BorderLayout(0, 0));
        configurationsPanel.setBorder(BorderFactory.createEmptyBorder(PAD / 2, PAD, PAD, PAD));
        configurationList = new JList(options.getConfigurations());
        configurationList.setCellRenderer(new ConfigurationCellRenderer());
        configurationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configurationList.addListSelectionListener(this);
        configurationList.addListSelectionListener(launchOptions);
        JScrollPane configScroll = new JScrollPane(configurationList);
        configurationsPanel.add(configScroll, BorderLayout.CENTER);
        leftPanel.add(configurationsPanel, BorderLayout.CENTER);

        JPanel topPanel = new JPanel();
        topPanel.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, 0, PAD));
        topPanel.setLayout(new BorderLayout());
        JButton installBtn = new JButton("Install from URL...");
        topPanel.add(installBtn, BorderLayout.CENTER);
        leftPanel.add(topPanel, BorderLayout.NORTH);

        // Add listener
        configurationList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    configurationList.setSelectedIndex(
                            configurationList.locationToIndex(e.getPoint()));
                    popupConfigurationMenu(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
        installBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openInstallFromURL();
            }
        });
        
        playBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.launch();
            }
        });

        playBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupServerHotListMenu(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        optionsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openOptions();
            }
        });

        addonsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openAddons();
            }
        });
        
        if (hideNews) {
            setSize(300, 500);
        }
    }
    
    @Override
    public Configuration getSelected() {
        return (Configuration) configurationList.getSelectedValue();
    }

    private OptionsDialog openOptions() {
        return openOptions(0);
    }

    private OptionsDialog openOptions(int index) {
        OptionsDialog dialog = new OptionsDialog(
                frame, getSelected(), options, index);
        dialog.setVisible(true);
        return dialog;
    }
    
    private AddonManagerDialog openAddons() {
        AddonManagerDialog dialog = new AddonManagerDialog(
                frame, getSelected(), launchOptions.getActiveJar().getName());
        dialog.setVisible(true);
        return dialog;
    }
    
    private InstallFromURLDialog openInstallFromURL() {
        InstallFromURLDialog dialog = new InstallFromURLDialog(frame, options);
        dialog.setVisible(true);
        return dialog;
    }
    
    /**
     * Open the configuration menu.
     * 
     * @param component component to open from
     */
    private void popupConfigurationMenu(Component component, int x, int y) {
        final Configuration configuration = getSelected();
        
        if (configuration != null) {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem menuItem;
            
            menuItem = new JMenuItem("Edit '" + configuration.getName() + "'...");
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new ConfigurationDialog(frame, configuration).setVisible(true);
                }
            });
            popup.add(menuItem);
            
            menuItem = new JMenuItem("Delete '" + configuration.getName() + "'...");
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!SwingHelper.confirm(
                            frame, "Remove",
                            "Are you sure you want to remove the selected " +
                            "configuration? No files will be deleted.")) {
                        return;
                    }
                    
                    if (configuration.isBuiltIn()) {
                        SwingHelper.showError(
                                frame,
                                "Built-in configuration",
                                "The configuration '"
                                        + configuration.getName()
                                        + "' is built-in and cannot be removed.");
                        return;
                    }
                    
                    options.getConfigurations().remove(configuration);
                    options.save();
                }
            });
            popup.add(menuItem);
            
            menuItem = new JMenuItem("Open Minecraft data folder...");
            menuItem.addActionListener(
                    ActionListeners.browseDir(
                            this, configuration.getMinecraftDir(), false));
            popup.add(menuItem);

            menuItem = new JMenuItem("Open texture packs folder...");
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    File f = new File(configuration.getMinecraftDir(),
                            "texturepacks");
                    f.mkdirs();
                    SwingHelper.browseDir(f, frame);
                }
            });
            popup.add(menuItem);

            popup.show(component, x, y);
        }
    }

    /**
     * Open the server hot list menu.
     * 
     * @param component component to open from
     */
    private void popupServerHotListMenu(Component component, int x, int y) {
        final ServerList servers = options.getServers();

        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;

        for (final ServerEntry server : servers.getServers()) {
            menuItem = new JMenuItem("Connect to " + server.getName());
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    launchOptions.setAutoConnect(server.getAddress());
                    frame.launch();
                }
            });
            popup.add(menuItem);
        }

        if (servers.size() == 0) {
            menuItem = new JMenuItem("No servers in the hot list.");
            menuItem.setEnabled(false);
            popup.add(menuItem);
        }

        popup.show(component, x, y);
    }
    
    private void updateNews() {
        Configuration configuration = (Configuration) configurationList.getSelectedValue();
        
        if (newsPanel != null) {
            URL url = configuration.getNewsUrl();
            if (url == null) {
                url = Constants.NEWS_URL;
            }
            newsPanel.browse(url, true);
        }
    }
    
    @Override
    public void selectAfterSort() {
        configurationList.setSelectedIndex(0);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        Configuration configuration = 
                (Configuration) ((JList) e.getSource()).getSelectedValue();
        for (ServerEntry entry : configuration.detectUserServers()) {
            entry.setTemporary(true);
            options.getServers().add(entry);
        }
        updateNews();
    }

}
