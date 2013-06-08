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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import com.sk89q.mclauncher.addons.Addon;
import com.sk89q.mclauncher.addons.AddonsProfile;
import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.config.Def;
import com.sk89q.mclauncher.config.MinecraftJar;
import com.sk89q.mclauncher.config.SettingsList;
import com.sk89q.mclauncher.util.UIUtil;

/**
 * The dialog for managing addons.
 * 
 * @author sk89q
 */
public class AddonManagerDialog extends JDialog {

    private static final long serialVersionUID = 3904227517893227045L;

    private LauncherFrame launcherFrame;
    private Configuration configuration;
    private JTable table;
    private JComboBox jarCombo;
    private AddonsProfile addonsProfile;
    private TaskWorker worker = new TaskWorker();
    private String lastSelectedJar = null;

    /**
     * Construct the dialog.
     * 
     * @param owner
     *            owning frame
     * @param config
     *            configuration
     * @param jar
     *            jar to use
     */
    public AddonManagerDialog(LauncherFrame owner, Configuration config,
            String jar) {
        super(owner, "Manage Addons for configuration '" + config.getName()
                + "'", true);

        final AddonManagerDialog self = this;
        this.configuration = config;
        this.launcherFrame = owner;

        setResizable(true);
        buildUI();
        pack();
        setSize(550, 350);
        setLocationRelativeTo(owner);

        // Select a JAR
        for (int i = 0; i < jarCombo.getItemCount(); i++) {
            Object o = jarCombo.getItemAt(i);
            if (o.toString().equals(jar)) {
                jarCombo.setSelectedItem(o);
            }
        }

        // Load the addons profile
        loadAddonsProfile();
        lastSelectedJar = jar;

        // Add a listener for changes to the active jar.
        jarCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                Object selected = ((JComboBox) e.getSource()).getSelectedItem();
                if (selected == null)
                    return;
                String jar = ((MinecraftJar) selected).getName();
                if (lastSelectedJar == null || lastSelectedJar != jar) {
                    lastSelectedJar = jar;
                    loadAddonsProfile();
                }
            }
        });

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                saveAddonsProfile();
                Launcher.getInstance().getOptions().save();
                self.dispose();
            }
        });
    }

    /**
     * Get the currently active JAR.
     * 
     * @return name of jar
     */
    public String getActiveJar() {
        Object o = jarCombo.getSelectedItem();
        if (o == null) {
            return "minecraft.jar";
        }
        return ((MinecraftJar) o).getName();
    }

    /**
     * Load the addons profile for the given JAR. If a load task is already in
     * progress, nothing will happen.
     */
    private void loadAddonsProfile() {
        if (worker.isAlive())
            return;
        this.addonsProfile = null;
        AddonsProfile addonsProfile = configuration
                .getAddonsProfile(getActiveJar());
        worker = Task.startWorker(this, new AddonsProfileLoaderTask(
                addonsProfile, this));
    }

    /**
     * Used to set the addons profile from a task. This is for internal usage.
     * 
     * @param addonsProfile
     *            profile
     */
    void setAddonsProfile(AddonsProfile addonsProfile) {
        saveAddonsProfile();
        this.addonsProfile = addonsProfile;
        table.setModel(addonsProfile);
        table.getColumnModel().getColumn(0).setMaxWidth(30);
    }

    /**
     * Try saving the addons profile, popping up an error dialog if an error
     * should occur.
     */
    private void saveAddonsProfile() {
        if (addonsProfile == null)
            return;
        if (!addonsProfile.save()) {
            JOptionPane.showMessageDialog(this,
                    "Changes to addons profile could not be saved.",
                    "Save error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Used to quick check if a procedure can continue only if an addons profile
     * has been loaded. Sometimes an addons profile load may fail.
     * 
     * @return true if it is loaded
     */
    private boolean checkAddonsProfileLoaded() {
        if (addonsProfile == null) {
            UIUtil.showError(this, "Not loaded",
                    "The addon list could not be loaded, and so you cannot do this.");
            return false;
        }
        return true;
    }

    /**
     * Open the install addon dialog.
     */
    private void openInstallDialog() {
        if (worker.isAlive()) return;
        if (!checkAddonsProfileLoaded()) return;
        
        SettingsList settings = Launcher.getInstance().getOptions().getSettings();
        
        // Ask for a file.
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select addon to install");
        String lastFolder = settings.get(Def.LAST_INSTALL_DIR); 
        if (lastFolder != null) 
            chooser.setCurrentDirectory(new File(lastFolder));
        
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) return true;
                String name = pathname.getName();
                int lastIndex = name.lastIndexOf('.');
                if (lastIndex == -1) {
                    return false;
                }
                String ext = name.substring(lastIndex + 1, name.length());
                return ext.equalsIgnoreCase("zip") ||
                        ext.equalsIgnoreCase("jar");
            }

            @Override
            public String getDescription() {
                return "Addon files (.zip; .jar)";
            }
        });
        
        int returnVal = chooser.showOpenDialog(this);
        settings.set(Def.LAST_INSTALL_DIR, 
                chooser.getCurrentDirectory().getAbsolutePath());
        
        // Proceed with installation
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            worker = Task.startWorker(this, new AddonInstallerTask(addonsProfile,
                    chooser.getSelectedFile()));
        }
    }

    /**
     * Start an uninstall of the given addons.
     * 
     * @param addons
     *            list of addons to uninstall
     */
    private void startUninstall(List<Addon> addons) {
        if (worker.isAlive())
            return;
        if (!checkAddonsProfileLoaded())
            return;

        worker = Task.startWorker(this, new AddonUninstallerTask(addonsProfile,
                addons));
    }

    /**
     * Open the tools menu.
     * 
     * @param component
     *            component to open from
     */
    private void popupToolsMenu(Component component) {
        final AddonManagerDialog self = this;

        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;

        menuItem = new JMenuItem("Open Minecraft data folder...");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UIUtil.browse(configuration.getMinecraftDir(), self);
            }
        });
        popup.add(menuItem);

        menuItem = new JMenuItem("Open texture packs folder...");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File f = new File(configuration.getMinecraftDir(),
                        "texturepacks");
                f.mkdirs();
                UIUtil.browse(f, self);
            }
        });
        popup.add(menuItem);

        popup.show(component, 0, component.getHeight());
    }

    /**
     * Build the UI.
     */
    private void buildUI() {
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout(8, 8));
        container.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        table = new JTable();
        table.setShowGrid(false);
        table.setRowHeight(table.getRowHeight() + 2);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane tableScroll = new JScrollPane(table);
        container.add(tableScroll, BorderLayout.CENTER);

        container.add(buildButtons(), BorderLayout.SOUTH);
        container.add(buildActiveJarSelector(), BorderLayout.NORTH);
        container.add(buildManageButtons(), BorderLayout.WEST);

        add(container, BorderLayout.CENTER);
    }

    /**
     * Build the UI for the active JAR.
     * 
     * @return panel
     */
    private JPanel buildActiveJarSelector() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        panel.add(new JLabel("Active JAR:"));

        panel.add(Box.createHorizontalStrut(5));

        jarCombo = new JComboBox();
        jarCombo.setModel(configuration.getJars());
        panel.add(jarCombo);

        panel.add(Box.createHorizontalGlue());

        JButton toolsBtn = new JButton("Tools...");
        panel.add(toolsBtn);
        toolsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popupToolsMenu((Component) e.getSource());
            }
        });

        return panel;
    }

    /**
     * Build the UI for the manage addon buttons.
     * 
     * @return panel
     */
    private JPanel buildManageButtons() {
        final AddonManagerDialog self = this;

        JPanel panel = new JPanel();

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 1, 2, 1);

        GridBagConstraints vertFill = new GridBagConstraints();
        vertFill.anchor = GridBagConstraints.NORTH;
        vertFill.fill = GridBagConstraints.VERTICAL;
        vertFill.weighty = 1.0;

        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        JButton installBtn = new JButton("Install...");
        panel.add(installBtn, c);
        installBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openInstallDialog();
            }
        });

        JButton moveUpBtn = new JButton("Move Up");
        moveUpBtn
                .setToolTipText("Higher entries are loaded first, so addons that are depended on by others should be near the top.");
        panel.add(moveUpBtn, c);
        moveUpBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!checkAddonsProfileLoaded())
                    return;
                int min = table.getSelectionModel().getMinSelectionIndex();
                int max = table.getSelectionModel().getMaxSelectionIndex();
                if (min != -1 && max != -1) {
                    if (addonsProfile.moveUp(min, max)) {
                        table.getSelectionModel().setSelectionInterval(min - 1,
                                max - 1);
                    }
                }
            }
        });

        JButton moveDownBtn = new JButton("Move Down");
        moveDownBtn.setToolTipText(moveUpBtn.getToolTipText());
        panel.add(moveDownBtn, c);
        moveDownBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!checkAddonsProfileLoaded())
                    return;
                int min = table.getSelectionModel().getMinSelectionIndex();
                int max = table.getSelectionModel().getMaxSelectionIndex();
                if (min != -1 && max != -1) {
                    if (addonsProfile.moveDown(min, max)) {
                        table.getSelectionModel().setSelectionInterval(min + 1,
                                max + 1);
                    }
                }
            }
        });

        JButton deleteBtn = new JButton("Uninstall...");
        panel.add(deleteBtn, c);
        deleteBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!checkAddonsProfileLoaded())
                    return;
                int min = table.getSelectionModel().getMinSelectionIndex();
                int max = table.getSelectionModel().getMaxSelectionIndex();
                if (min == -1 || max == -1) {
                    JOptionPane.showMessageDialog(self,
                            "You have not selected any addons.",
                            "Selection error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (JOptionPane
                        .showConfirmDialog(
                                self,
                                "Are you sure you want to uninstall the selected addons?",
                                "Uninstall", JOptionPane.YES_NO_OPTION) != 0)
                    return;

                List<Addon> addons = new ArrayList<Addon>();
                for (int i = min; i <= max; i++) {
                    addons.add(addonsProfile.getAddonAt(i));
                }

                startUninstall(addons);
            }
        });

        panel.add(Box.createVerticalGlue(), vertFill);

        return panel;
    }

    /**
     * Build the bottom button bar.
     * 
     * @return panel
     */
    private JPanel buildButtons() {
        final AddonManagerDialog self = this;

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        panel.add(Box.createHorizontalGlue());

        JButton launchBtn = new JButton("Launch...");
        panel.add(launchBtn);
        launchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAddonsProfile();
                Launcher.getInstance().getOptions().save();
                self.dispose();
                launcherFrame.launch();
            }
        });

        panel.add(Box.createHorizontalStrut(3));

        JButton testBtn = new JButton("Test...");
        panel.add(testBtn);
        testBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAddonsProfile();
                Launcher.getInstance().getOptions().save();
                self.dispose();
                
                launcherFrame.getLaunchSettings().setTestMode();
                launcherFrame.launch();
            }
        });

        panel.add(Box.createHorizontalStrut(3));

        JButton okBtn = new JButton("Close");
        Dimension pref = okBtn.getPreferredSize();
        okBtn.setPreferredSize(new Dimension(Math.max(pref.width, 80),
                pref.height));
        panel.add(okBtn);
        okBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAddonsProfile();
                Launcher.getInstance().getOptions().save();
                self.dispose();
            }
        });

        UIUtil.equalWidth(launchBtn, testBtn, okBtn);

        return panel;
    }

}
