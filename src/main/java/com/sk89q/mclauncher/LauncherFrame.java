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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.config.Def;
import com.sk89q.mclauncher.config.Identity;
import com.sk89q.mclauncher.config.IdentityList;
import com.sk89q.mclauncher.config.LauncherOptions;
import com.sk89q.mclauncher.config.MinecraftJar;
import com.sk89q.mclauncher.config.ServerEntry;
import com.sk89q.mclauncher.config.ServerList;
import com.sk89q.mclauncher.util.UIUtil;

/**
 * Main launcher GUI frame.
 * 
 * @author sk89q
 */
public class LauncherFrame extends JFrame implements ListSelectionListener {

    private static final long serialVersionUID = 4122023031876609883L;
    private static final int PAD = 12;

    private final LauncherOptions options;
    private final LaunchOptions launchOptions;
    
    private JList configurationList;
    private JButton playBtn;
    private TaskWorker worker = new TaskWorker();

    /**
     * Construct the launcher.
     */
    public LauncherFrame() {
        setTitle("SK's Minecraft Launcher");
        setSize(620, 500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        UIUtil.setLookAndFeel();
        UIUtil.setIconImage(this, "/resources/icon.png");

        options = Launcher.getInstance().getOptions();
        launchOptions = new LaunchOptions(this);

        addComponents();
        setLocationRelativeTo(null);

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
    
    public Configuration getCurrentConfiguration() {
        return (Configuration) configurationList.getSelectedValue();
    }

    public LaunchOptions getLaunchSettings() {
        return launchOptions;
    }

    private void addComponents() {
        setLayout(new BorderLayout(0, 0));
        boolean hidenews = options.getSettings().getBool(Def.LAUNCHER_HIDE_NEWS, false);
        
        if (!hidenews) {
            if (options.getSettings().getBool(Def.LAUNCHER_NO_NEWS, false)) {
                final JLayeredPane newsPanel = new JLayeredPane();
                
                newsPanel.setBorder(new CompoundBorder(BorderFactory
                        .createEmptyBorder(PAD, 0, PAD, PAD), new CompoundBorder(
                        BorderFactory.createEtchedBorder(), BorderFactory
                                .createEmptyBorder(4, 4, 4, 4))));
                newsPanel.setLayout(new BoxLayout(newsPanel, BoxLayout.Y_AXIS));
                
                final JButton showNews = new JButton("Show news");
                showNews.setAlignmentX(Component.CENTER_ALIGNMENT);
                showNews.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        showNews.setVisible(false);
                        showNews(newsPanel);
                    }
                });
                
                // Center the button vertically.
                newsPanel.add(new Box.Filler(new Dimension(0,0), 
                        new Dimension(0,0), new Dimension(1000,1000)));
                newsPanel.add(showNews);
                newsPanel.add(new Box.Filler(new Dimension(0,0), 
                        new Dimension(0,0), new Dimension(1000,1000)));
                
                add(newsPanel, BorderLayout.CENTER);
            } else {
                JLayeredPane newsPanel = new JLayeredPane();
                showNews(newsPanel);
                add(newsPanel, BorderLayout.CENTER);
            }
        }
        
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        if (!hidenews) {
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
        installBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openInstallFromURL();
            }
        });
        
        playBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launch();
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
        
        if (hidenews) {
            setSize(300, 500);
        }
    }

    private OptionsDialog openOptions() {
        return openOptions(0);
    }

    private OptionsDialog openOptions(int index) {
        OptionsDialog dialog = new OptionsDialog(this, getCurrentConfiguration(), options, index);
        dialog.setVisible(true);
        return dialog;
    }
    
    private AddonManagerDialog openAddons() {
        AddonManagerDialog dialog = new AddonManagerDialog(this, 
                getCurrentConfiguration(), launchOptions.getActiveJar().getName());
        dialog.setVisible(true);
        return dialog;
    }
    
    private InstallFromURLDialog openInstallFromURL() {
        InstallFromURLDialog dialog = new InstallFromURLDialog(this, options);
        dialog.setVisible(true);
        return dialog;
    }
    
    private void showNews(JLayeredPane newsPanel) {
        final LauncherFrame self = this;
        newsPanel.setLayout(new NewsLayoutManager());
        newsPanel
            .setBorder(BorderFactory.createEmptyBorder(PAD, 0, PAD, PAD));
        JEditorPane newsView = new JEditorPane();
        newsView.setEditable(false);
        newsView.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    UIUtil.openURL(e.getURL(), self);
                }
            }
        });
        JScrollPane newsScroll = new JScrollPane(newsView);
        newsPanel.add(newsScroll, new Integer(1));
        JProgressBar newsProgress = new JProgressBar();
        newsProgress.setIndeterminate(true);
        newsPanel.add(newsProgress, new Integer(2));
        NewsFetcher.update(newsView, newsProgress);
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
                    getLaunchSettings().setAutoConnect(server.getAddress());
                    launch();
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
    
    /**
     * Launch the game.
     */
    public void launch() {
        if (worker.isAlive()) {
            return;
        }

        Configuration configuration = getCurrentConfiguration();
        IdentityList identities = options.getIdentities();
        
        if (!launchOptions.verifyAndNotify()) {
            return;
        }

        Identity identity = launchOptions.getIdentity();
        boolean remember = launchOptions.shouldRememberPassword();
        MinecraftJar jar = launchOptions.getActiveJar();

        // Save the identity
        if (!launchOptions.isPlayingOffline()) {
            if (remember) {
                identities.remember(identity.getId(), identity.getPassword());
            } else {
                identities.forgetPassword(identity.getId());
            }
        } else {
            identity = new Identity("Player", null);
        }
        
        configuration.updateLastLaunch();
        configuration.setLastJar(jar);
        
        options.getConfigurations().sortByDate();
        
        options.save();

        LaunchTask task = new LaunchTask(
                this, getCurrentConfiguration(), 
                identity.getId(), identity.getPassword(), jar);
        task.setForceUpdate(launchOptions.isForcingUpdate());
        task.setForceIncrementalUpdate(launchOptions.isForcingIncrementalUpdate());
        task.setPlayOffline(launchOptions.isPlayingOffline());
        task.setForceConsole(launchOptions.getShowConsole());

        worker = Task.startWorker(this, task);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        Configuration configuration = 
                (Configuration) ((JList) e.getSource()).getSelectedValue();
        for (ServerEntry entry : configuration.detectUserServers()) {
            entry.setTemporary(true);
            options.getServers().add(entry);
        }
    }
    
}