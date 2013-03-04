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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.config.ConfigurationsManager;
import com.sk89q.mclauncher.config.LauncherOptions;
import com.sk89q.mclauncher.util.UIUtil;

/**
 * The dialog for launcher options.
 * 
 * @author sk89q
 */
public class OptionsDialog extends JDialog {

    private static final long serialVersionUID = -1889866989693082182L;
    private LauncherOptions options;
    private JTable configsTable;
    private JTabbedPane tabs;
    private List<OptionsPanel> optionsPanels = new ArrayList<OptionsPanel>();

    /**
     * Construct the dialog.
     * 
     * @param owner owning frame
     * @param configuration configuration
     * @param options options object
     * @param initialTab index of the initial tab, 0 for the first
     */
    public OptionsDialog(LauncherFrame owner, Configuration configuration,
            LauncherOptions options, int initialTab) {
        super(owner, "Launcher Options", true);

        this.options = options;
        setResizable(true);
        buildUI();
        pack();
        setSize(400, 540);
        setLocationRelativeTo(owner);
        
        tabs.setSelectedIndex(initialTab);

        for (OptionsPanel panel : optionsPanels) {
            panel.copySettingsToFields();
        }
    }

    /**
     * Save the options.
     * 
     * @param copySettings true to also copy settings from the fields
     * @return true if successful
     */
    boolean save(boolean copySettings) {
        if (copySettings) {
            for (OptionsPanel panel : optionsPanels) {
                panel.copyFieldsToSettings();
            }
        }

        if (!options.save()) {
            JOptionPane.showMessageDialog(this,
                    "Your options could not be saved to disk.", "Save error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    /**
     * Adds the panel to the list of options panles.
     * 
     * @param panel panel
     * @return panel
     */
    private <T extends OptionsPanel> T wrap(T panel) {
        optionsPanels.add(panel);
        return panel;
    }

    /**
     * Build the interface.
     */
    private void buildUI() {
        final OptionsDialog self = this;

        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createEmptyBorder(8, 8, 5, 8));
        container.setLayout(new BorderLayout(3, 3));

        tabs = new JTabbedPane();
        tabs.addTab("Launcher",
                wrap(new LauncherOptionsPanel(options.getSettings(), false)));
        tabs.addTab("Environment",
                wrap(new EnvironmentOptionsPanel(options.getSettings(), false)));
        tabs.addTab("Configurations", buildConfigurationsPanel());
        tabs.addTab("About", buildAboutPanel());
        container.add(tabs, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        UIUtil.equalWidth(okButton, cancelButton);
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);
        container.add(buttonsPanel, BorderLayout.SOUTH);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (save(true)) {
                    self.dispose();
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                self.dispose();
            }
        });

        add(container, BorderLayout.CENTER);
    }

    /**
     * Build the configurations panel.
     * 
     * @return created panel
     */
    private JPanel buildConfigurationsPanel() {
        final OptionsDialog self = this;

        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
        JButton newBtn = new JButton("New...");
        buttonsPanel.add(newBtn);
        buttonsPanel.add(Box.createHorizontalStrut(3));
        JButton modifyBtn = new JButton("Modify...");
        buttonsPanel.add(modifyBtn);
        buttonsPanel.add(Box.createHorizontalGlue());
        JButton removeBtn = new JButton("Delete...");
        buttonsPanel.add(removeBtn);
        panel.add(buttonsPanel);

        panel.add(Box.createVerticalStrut(3));

        configsTable = new JTable();
        configsTable.setShowGrid(false);
        configsTable.setRowHeight(configsTable.getRowHeight() + 2);
        configsTable.setIntercellSpacing(new Dimension(0, 0));
        configsTable.setFillsViewportHeight(true);
        configsTable
                .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        configsTable.setModel(options.getConfigurations());
        configsTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        JScrollPane tableScroll = new JScrollPane(configsTable);
        panel.add(tableScroll, BorderLayout.CENTER);

        newBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new ConfigurationDialog(self, options.getConfigurations())
                        .setVisible(true);
            }
        });

        modifyBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int min = configsTable.getSelectionModel()
                        .getMinSelectionIndex();
                int max = configsTable.getSelectionModel()
                        .getMaxSelectionIndex();
                if (min == -1 || max == -1) {
                    UIUtil.showError(self, "Selection error",
                            "You have not selected any configurations.");
                    return;
                }
                if (min != max) {
                    UIUtil.showError(self, "Selection error",
                            "Select one configuration at a time.");
                    return;
                }

                Configuration config = options.getConfigurations()
                        .getConfigurationAt(min);
                new ConfigurationDialog(self, options.getConfigurations(),
                        config).setVisible(true);
            }
        });

        removeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int min = configsTable.getSelectionModel()
                        .getMinSelectionIndex();
                int max = configsTable.getSelectionModel()
                        .getMaxSelectionIndex();
                if (min == -1 || max == -1) {
                    UIUtil.showError(self, "Selection error",
                            "You have not selected any configurations.");
                    return;
                }

                if (JOptionPane
                        .showConfirmDialog(
                                self,
                                "Are you sure you want to remove the selected configuration? No files will be deleted.",
                                "Remove", JOptionPane.YES_NO_OPTION) != 0)
                    return;

                ConfigurationsManager configsManager = options
                        .getConfigurations();
                for (int i = min; i <= max; i++) {
                    Configuration config = configsManager.getConfigurationAt(i);
                    if (config.isBuiltIn()) {
                        UIUtil.showError(
                                self,
                                "Built-in configuration",
                                "The configuration '"
                                        + config.getName()
                                        + "' is built-in and cannot be removed.");
                    } else {
                        configsManager.remove(config);
                    }
                }

                save(false);
            }
        });

        UIUtil.removeOpaqueness(buttonsPanel);

        return panel;
    }

    /**
     * Build the about panel.
     * 
     * @return panel
     */
    private JPanel buildAboutPanel() {
        final OptionsDialog self = this;

        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label;
        label = new JLabel("SK's Minecraft Launcher");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        label = new JLabel("Version " + Launcher.VERSION);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);

        LinkButton btn = new LinkButton("http://www.sk89q.com");
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(btn);

        panel.add(Box.createVerticalStrut(20));

        final JTextArea text = new JTextArea();
        text.setEditable(false);
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.setFont(label.getFont());
        DefaultCaret caret = (DefaultCaret) text.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setPreferredSize(new Dimension(text.getPreferredSize().width, 200));
        panel.add(scroll);

        panel.add(Box.createVerticalGlue());

        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UIUtil.openURL("http://www.sk89q.com", self);
            }
        });
        
        // Fetch notices
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String notices = Launcher.getNotices();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        text.setText(notices);
                    }
                });
            }
        }).start();

        return panel;
    }

}
