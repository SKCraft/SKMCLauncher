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

package com.sk89q.lpbuilder;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.LinkButton;
import com.sk89q.mclauncher.Task.ExecutionException;
import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.model.FileGroup;
import com.sk89q.mclauncher.model.PackageManifest;
import com.sk89q.mclauncher.model.UpdateManifest;
import com.sk89q.mclauncher.util.DirectoryField;
import com.sk89q.mclauncher.util.FileField;
import com.sk89q.mclauncher.util.MessageLog;
import com.sk89q.mclauncher.util.SimpleLogFormatter;
import com.sk89q.mclauncher.util.UIUtil;
import com.sk89q.mclauncher.util.XMLUtil;

public class UpdateBuilderGUI extends JFrame {

    private static final long serialVersionUID = 48131029351157284L;
    private static final Logger logger = Logger.getLogger(
            UpdateBuilderGUI.class.getCanonicalName());
    
    private final UpdateBuilderGUI self = this;
    private final MessageLog messageLog;
    private FileField configField;
    private JTextField idText;
    private JTextField nameText;
    private JTextField versionText;
    private DirectoryField sourceField;
    private DirectoryField outputField;
    private JTextField packageText;
    private JTextField updateText;
    private JCheckBox includeLibsCheck;
    private JCheckBox cleanCheck;
    private JCheckBox zipConfigsCheck;
    private JButton buildButton;
    
    private Thread buildThread;

    public UpdateBuilderGUI() {
        setTitle("Update Package Builder");
        setSize(450, 600);
        UIUtil.setIconImage(this, "/resources/icon.png");
        setLocationRelativeTo(null);

        messageLog = new MessageLog(1000, true);
        messageLog.registerLoggerHandler();
        
        addComponents();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                close();
            }
        });
    }
    
    private void close() {
        setBuilding(false);
        messageLog.detachGlobalHandler();
        self.dispose();
    }
    
    private void build() {
        try {
            completeBuild();
        } catch (ExecutionException e) {
            UIUtil.showError(this, "Error with parameters", e.getMessage());
        } catch (Throwable e) {
            UIUtil.showError(this, "Error", "Something went wrong: " + e.toString());
            logger.log(Level.WARNING, "Error", e);
        }
    }
    
    private void cancel() {
        Thread thread = buildThread;
        if (thread != null) {
            thread.interrupt();
            buildThread = null;
        }
    }
    
    private void completeBuild() throws Throwable {
        if (buildThread != null) {
            cancel();
            return;
        }
        
        String configPath = validate("Builder configuration", configField.getPath(), false, null);
        String id = validate("ID", idText.getText(), false, Configuration.ID_PATTERN);
        String name = validate("Name", nameText.getText(), false, null);
        String version = validate("Version", versionText.getText(), false, null);
        File sourceDir = validateDirectory("Source directory", sourceField.getPath(), false);
        final File outputDir = validateDirectory("Output directory", outputField.getPath(), true);
        String packageFilename = validate("Package filename", packageText.getText(), true, null);
        String updateFilename = validate("Update filename", updateText.getText(), true, null);
        boolean includeLibs = includeLibsCheck.isSelected();
        final boolean clean = cleanCheck.isSelected();
        boolean zipConfigs = zipConfigsCheck.isSelected();
        
        // Check if source == output
        if (sourceDir.equals(outputDir)) {
            throw new ExecutionException(
                    "The output directory cannot be the same as the source directory!");
        }

        // Confirm clean
        if (clean && JOptionPane
                .showConfirmDialog(
                        this,
                        "Continue and delete everything in " + outputDir.getAbsolutePath() + "?",
                        "Clean Directory", JOptionPane.YES_NO_OPTION) != 0) {
            return;
        }

        final UpdateBuilder builder = new UpdateBuilder(sourceDir, outputDir);
        
        // Load some basic details
        if (configPath != null)
            builder.loadConfiguration(new File(configPath));

        UpdateManifest updateManifest = builder.getUpdateManifest();
        updateManifest.setLatestVersion(version);
        
        if (packageFilename != null)
            builder.setPackageFilename(packageFilename);
        
        if (updateFilename != null)
            builder.setUpdateFilename(updateFilename);
        
        if (id != null) 
            updateManifest.setId(id);
        
        if (name != null) 
            updateManifest.setName(name);
        
        // Double check some fields
        if (updateManifest.getId() == null)
            throw new ExecutionException("Package ID not set!");
        
        if (!Configuration.isValidId(updateManifest.getId()))
            throw new ExecutionException(
                    "The package ID (from the builder configuration) is invalid!");
        
        if (updateManifest.getName() == null)
            throw new ExecutionException("Package name not set!");

        List<FileGroup> fileGroups = builder.getPackageManifest().getFileGroups();
        UpdateBuilderConfig config = builder.getConfiguration();
        
        // Add libraries
        if (includeLibs) {
            PackageManifest libsManifest = XMLUtil.parseJaxb(
                    PackageManifest.class, 
                    Launcher.class.getResourceAsStream("/resources/libs_builder_config.xml"));
            for (FileGroup group : libsManifest.getFileGroups()) {
                fileGroups.add(group);
            }
        }
        
        // Add extra configuration
        if (zipConfigs) {
            FilePattern pattern = new FilePattern();
            pattern.getPathPatterns().add(new PathPattern.Include("config/*"));
            pattern.setArchiveName("configs.extract.zip");
            config.getFilePatterns().add(pattern);
        }
        
        // Clear log
        messageLog.clear();

        buildThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (clean) {
                        UpdateBuilder.clean(outputDir);
                    }
                    
                    builder.run();
                } catch (InterruptedException e) {
                } finally {
                    setBuilding(false);
                }
            }
        });
        buildThread.setName("Builder");
        buildThread.start();

        setBuilding(true);
    }
    
    private void setBuilding(boolean building) {
        if (building) {
            buildButton.setText("Cancel Build");
        } else {
            cancel();
            buildButton.setText("Build Package...");
        }
    }
    
    private static File validateDirectory(
            String name, String path, boolean mkdirs) throws ExecutionException {
        path = path.trim();
        if (path.isEmpty()) {
            throw new ExecutionException("Required field '" + name + "' not set.");
        }
        File file = new File(path);
        if (mkdirs) {
            file.mkdirs();
        }
        if (!file.exists()) {
            throw new ExecutionException("The path '" + path + "' doesn't exist.");
        }
        if (!file.isDirectory()) {
            throw new ExecutionException("The path '" + path + "' is not a directory.");
        }
        return file;
    }
    
    private static String validate(
            String name, String text, boolean required, Pattern pattern) 
            throws ExecutionException {
        text = text.trim();
        if (required) {
            if (text.isEmpty()) {
                throw new ExecutionException("Required field '" + name + "' not set.");
            }
        } else {
            if (text.isEmpty()) {
                return null;
            }
        }
        
        if (pattern != null) {
            if (!pattern.matcher(text).matches()) {
                throw new ExecutionException(
                        "Required field '" + name + "' has invalid characters.\n\n" +
                        "(Must match regex. " + pattern.toString() + ")");
            }
        }
        
        return text;
    }
    
    private void addComponents() {
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
        
        final JLabel buildConfigLabel = new JLabel("Build configuration:");
        panel.add(buildConfigLabel, labelConstraints);
        configField = new FileField("Select builder configuration", 
                "Builder configuration");
        buildConfigLabel.setLabelFor(configField);
        panel.add(configField, fieldConstraints);
        configField.setVisible(false);
        buildConfigLabel.setVisible(false);
        
        final JButton useConfigBtn = new JButton("Use Builder Configuration");
        final JPanel useConfigPanel = new JPanel();
        useConfigPanel.add(new JLabel("Have a configuration file? "));
        useConfigPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        useConfigPanel.add(useConfigBtn);
        useConfigBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buildConfigLabel.setVisible(true);
                configField.setVisible(true);
                useConfigPanel.setVisible(false);
            }
        });
        panel.add(useConfigPanel, fullFieldConstraints);

        panel.add(Box.createVerticalStrut(3), fullFieldConstraints);
        
        JLabel label;
        
        label = new JLabel("Package ID:");
        panel.add(label, labelConstraints);
        idText = new JTextField();
        idText.setText("my-mod-pack");
        label.setLabelFor(idText);
        panel.add(idText, fieldConstraints);
        
        label = new JLabel("Package name:");
        panel.add(label, labelConstraints);
        nameText = new JTextField();
        nameText.setText("My Mod Pack");
        label.setLabelFor(nameText);
        panel.add(nameText, fieldConstraints);
        
        label = new JLabel("Version:");
        panel.add(label, labelConstraints);
        versionText = new JTextField();
        versionText.setText(new Date().toString());
        label.setLabelFor(versionText);
        panel.add(versionText, fieldConstraints);
        
        label = new JLabel("Source directory:");
        panel.add(label, labelConstraints);
        sourceField = new DirectoryField();
        label.setLabelFor(sourceField);
        panel.add(sourceField, fieldConstraints);

        panel.add(Box.createGlue(), labelConstraints);
        includeLibsCheck = new JCheckBox("Add Mojang's LWJGL into package");
        includeLibsCheck.setBorder(null);
        includeLibsCheck.setSelected(true);
        panel.add(includeLibsCheck, fieldConstraints);

        panel.add(Box.createGlue(), labelConstraints);
        zipConfigsCheck = new JCheckBox("Put files from config/ into one .zip");
        zipConfigsCheck.setBorder(null);
        zipConfigsCheck.setSelected(true);
        panel.add(zipConfigsCheck, fieldConstraints);
        
        label = new JLabel("Output directory:");
        panel.add(label, labelConstraints);
        outputField = new DirectoryField();
        label.setLabelFor(outputField);
        panel.add(outputField, fieldConstraints);

        panel.add(Box.createGlue(), labelConstraints);
        cleanCheck = new JCheckBox("Delete everything in the output directory first");
        cleanCheck.setBorder(null);
        cleanCheck.setSelected(true);
        panel.add(cleanCheck, fieldConstraints);
        
        label = new JLabel("Package filename:");
        panel.add(label, labelConstraints);
        packageText = new JTextField();
        packageText.setText("package.xml");
        label.setLabelFor(packageText);
        panel.add(packageText, fieldConstraints);
        
        label = new JLabel("Update filename:");
        panel.add(label, labelConstraints);
        updateText = new JTextField();
        updateText.setText("update.xml");
        label.setLabelFor(updateText);
        panel.add(updateText, fieldConstraints);
        
        add(panel, BorderLayout.NORTH);
        
        add(messageLog, BorderLayout.CENTER);

        Box buttonsPanel = Box.createHorizontalBox();
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        LinkButton helpButton = new LinkButton("Help! What do I do?");
        buildButton = new JButton("Build Package...");
        JButton closeButton = new JButton("Close");
        buttonsPanel.add(helpButton);
        buttonsPanel.add(Box.createHorizontalGlue());
        buttonsPanel.add(buildButton);
        buttonsPanel.add(Box.createHorizontalStrut(6));
        buttonsPanel.add(closeButton);
        add(buttonsPanel, BorderLayout.SOUTH);
        
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UIUtil.openURL(
                        "https://github.com/sk89q/skmclauncher/blob/master/README.md", self);
            }
        });
        
        buildButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                build();
            }
        });
        
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                idText.selectAll();
                idText.requestFocusInWindow();
            }
        });
    }
    
    public static void main(String[] args) {
        SimpleLogFormatter.setAsFormatter();
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                UIUtil.setLookAndFeel();
                UpdateBuilderGUI gui = new UpdateBuilderGUI();
                gui.setVisible(true);
            }
        });
    }

}
