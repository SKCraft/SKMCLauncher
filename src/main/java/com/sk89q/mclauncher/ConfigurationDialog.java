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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.config.ConfigurationsManager;
import com.sk89q.mclauncher.util.SettingsList;
import com.sk89q.mclauncher.util.UIUtil;

/**
 * Dialog for adding or modifying a {@link Configuration}.
 * 
 * @author sk89q
 */
public class ConfigurationDialog extends JDialog {

    private static final long serialVersionUID = -7347791965966294361L;
    private OptionsDialog optionsDialog;
    private ConfigurationsManager configsManager;
    private JButton browseBtn;
    private JTextField nameText;
    private JTextField pathText;
    private JCheckBox customPathCheck;
    private JTextField urlText;
    private JCheckBox customUpdateCheck;
    private Configuration configuration;
    private SettingsList settings;
    private List<OptionsPanel> optionsPanels = new ArrayList<OptionsPanel>();
    
    /**
     * Start editing a given configuration.
     * 
     * @param owner owning dialog
     * @param configsManager configurations manager
     * @param configuration configuration to edit
     */
    public ConfigurationDialog(OptionsDialog owner, ConfigurationsManager configsManager,
            Configuration configuration) {
        super(owner, "Edit Configuration", true);
        this.configuration = configuration;
        this.settings = configuration.getSettings();
        setup(owner, configsManager);
    }

    /**
     * Start a new configuration.
     * 
     * @param owner owning dialog
     * @param configsManager configurations manager
     */
    public ConfigurationDialog(OptionsDialog owner, ConfigurationsManager configsManager) {
        super(owner, "New Configuration", true);
        this.settings = new SettingsList();
        setup(owner, configsManager);
    }
    
    /**
     * Setup.
     * 
     * @param owner owning dialog
     * @param configsManager configurations manager
     */
    private void setup(OptionsDialog owner, ConfigurationsManager configsManager) {
        this.optionsDialog = owner;
        this.configsManager = configsManager;
        
        setResizable(false);
        buildUI();
        pack();
        setSize(400, 500);
        setLocationRelativeTo(owner);

        for (OptionsPanel panel : optionsPanels) {
            panel.copySettingsToFields();
        }

        if (configuration != null) {
            nameText.setText(configuration.getName());
            if (configuration.isBuiltIn()) {
                customPathCheck.setSelected(true);
                pathText.setText(configuration.getBaseDir().getPath());
            } else {
                boolean usingDefault = configuration.isUsingDefaultPath();
                customPathCheck.setSelected(!usingDefault);
                if (!usingDefault) {
                    pathText.setText(configuration.getDirForOptions());
                }
            }
            URL updateUrl = configuration.getUpdateUrl();
            customUpdateCheck.setSelected(updateUrl != null);
            urlText.setEnabled(updateUrl != null);
            urlText.setText(updateUrl != null ? updateUrl.toString() : "");
            
            if (configuration.isBuiltIn()) {
                nameText.setEnabled(false);
                customPathCheck.setEnabled(false);
                pathText.setEnabled(false);
                customUpdateCheck.setEnabled(false);
                urlText.setEnabled(false);
                browseBtn.setEnabled(false);
            }
        }
    }
    
    /**
     * Adds an option panel to the index.
     * 
     * @param panel panel
     * @return panel
     */
    private <T extends OptionsPanel> T wrap(T panel) {
        optionsPanels.add(panel);
        return panel;
    }
    
    /**
     * Build the UI.
     */
    private void buildUI() {
        final ConfigurationDialog self = this;
        
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createEmptyBorder(8, 8, 5, 8));
        container.setLayout(new BorderLayout(3, 3));
        
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Configuration", buildConfigurationPanel());
        tabs.addTab("Environment", wrap(new EnvironmentOptionsPanel(settings, true)));
        container.add(tabs, BorderLayout.CENTER);
        
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        JButton okBtn = new JButton("OK");
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(70, (int) cancelBtn.getPreferredSize().getHeight()));
        okBtn.setPreferredSize(cancelBtn.getPreferredSize());
        buttonsPanel.add(okBtn);
        buttonsPanel.add(cancelBtn);
        container.add(buttonsPanel, BorderLayout.SOUTH);

        okBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (complete()) {
                    self.dispose();
                }
            }
        });
        
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                self.dispose();
            }
        });
        
        add(container, BorderLayout.CENTER);
    }
    
    /**
     * Build the main configuration tab.
     * 
     * @return panel
     */
    private JPanel buildConfigurationPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new GridBagLayout());
        
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
        fieldConstraints.insets = new Insets(2, 1, 2, 1);
        
        GridBagConstraints labelConstraints = (GridBagConstraints) fieldConstraints.clone();
        labelConstraints.weightx = 0.0;
        labelConstraints.gridwidth = 1;
        labelConstraints.insets = new Insets(1, 1, 1, 10);
        
        GridBagConstraints fullFieldConstraints = (GridBagConstraints) fieldConstraints.clone();
        fullFieldConstraints.insets = new Insets(5, 2, 1, 2);
        
        if (configuration != null && configuration.isBuiltIn()) {
            panel.add(new JLabel("This is a built-in configuration and editing is restricted."), fullFieldConstraints);
            panel.add(Box.createVerticalStrut(10), fullFieldConstraints);
        }
        
        JLabel nameLabel = new JLabel("Name:");
        panel.add(nameLabel, labelConstraints);
        nameText = new JTextField(30);
        nameLabel.setLabelFor(nameText);
        panel.add(nameText, fieldConstraints);

        JLabel pathLabel = new JLabel("Path:");
        panel.add(pathLabel, labelConstraints);
        customPathCheck = new JCheckBox("Use a custom path");
        customPathCheck.setBorder(null);
        panel.add(customPathCheck, fieldConstraints);
        panel.add(Box.createGlue(), labelConstraints);
        JPanel pathPanel = new JPanel();
        pathPanel.setLayout(new BoxLayout(pathPanel, BoxLayout.X_AXIS));
        pathText = new JTextField(30);
        pathText.setMaximumSize(pathText.getPreferredSize());
        nameLabel.setLabelFor(pathText);
        browseBtn = new JButton("Browse...");
        browseBtn.setPreferredSize(new Dimension(
                browseBtn.getPreferredSize().width,
                pathText.getPreferredSize().height));
        pathPanel.add(pathText);
        pathPanel.add(Box.createHorizontalStrut(3));
        pathPanel.add(browseBtn);
        panel.add(pathPanel, fieldConstraints);

        panel.add(Box.createVerticalStrut(10), fullFieldConstraints);

        JLabel urlLabel = new JLabel("Update URL:");
        panel.add(urlLabel, labelConstraints);
        customUpdateCheck = new JCheckBox("Use a custom update URL");
        customUpdateCheck.setBorder(null);
        panel.add(customUpdateCheck, fieldConstraints);
        panel.add(Box.createGlue(), labelConstraints);
        urlText = new JTextField("http://");
        urlLabel.setLabelFor(urlText);
        panel.add(urlText, fieldConstraints);
        
        browseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openPathBrowser();
            }
        });

        pathText.setEnabled(false);
        browseBtn.setEnabled(false);
        customPathCheck.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                pathText.setEnabled(((JCheckBox) e.getSource()).isSelected());
                browseBtn.setEnabled(((JCheckBox) e.getSource()).isSelected());
            }
        });
        
        urlText.setEnabled(false);
        customUpdateCheck.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                urlText.setEnabled(((JCheckBox) e.getSource()).isSelected());
            }
        });

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(panel);
        container.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, 10000), new Dimension(0, 10000)));
        UIUtil.removeOpaqueness(container);
        return container;
    }
    
    /**
     * Validate and save.
     * 
     * @return true if successful
     */
    private boolean complete() {
        boolean builtIn = configuration != null && configuration.isBuiltIn();

        String name = nameText.getText().trim();
        String pathStr = customPathCheck.isSelected() ? pathText.getText().trim() : null;
        String updateURLStr = customUpdateCheck.isSelected() ? urlText.getText() : null;
        URL updateUrl = null;
        
        if (!builtIn) {
            if (name.length() == 0) {
                UIUtil.showError(this, "No name", "A name must be entered.");
                return false;
            }
            
            if (pathStr != null && pathStr.length() == 0) {
                UIUtil.showError(this, "No path", "A path must be entered.");
                return false;
            }
            
            if (updateURLStr != null && updateURLStr.length() == 0) {
                UIUtil.showError(this, "No URL", "An update URL must be entered.");
                return false;
            }
            
            if (updateURLStr != null) {
                try {
                    updateUrl = new URL(updateURLStr);
                } catch (MalformedURLException e) {
                    UIUtil.showError(this, "Invalid URL", "The update URL that you entered is invalid.");
                    return false;
                }
            }

            if (pathStr != null) {
                File f = Launcher.replacePathTokens(pathStr);
                f.mkdirs();
                if (!f.isDirectory()) {
                    UIUtil.showError(this, "Invalid path", "The path that you entered does not exist or is not a directory.");
                    return false;
                }
            }
        }
        
        for (OptionsPanel panel : optionsPanels) {
            panel.copyFieldsToSettings();
        }
        
        if (configuration == null) { // New configuration
            String id = UUID.randomUUID().toString();
            Configuration config = new Configuration(id, name, pathStr, updateUrl, true);
            config.setSettings(settings);
            configsManager.register(config);
            this.configuration = config;
        } else {
            if (!builtIn) {
                configuration.setName(name);
                configuration.setCustomBasePath(pathStr);
                configuration.setUpdateUrl(updateUrl);
            }
        }
        
        optionsDialog.save(false);
        
        return true;
    }
    
    /**
     * Open the path browser.
     */
    private void openPathBrowser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) return true;
                return false;
            }

            @Override
            public String getDescription() {
                return "Directories";
            }
        });
        
        int returnVal = chooser.showOpenDialog(this);
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            pathText.setText(chooser.getSelectedFile().getPath());
        }
    }

}
