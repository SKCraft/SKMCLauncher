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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.sk89q.mclauncher.config.SettingsList;

/**
 * Easy panel for options, based off of SettingsList.
 * 
 * @see SettingsList
 * @author sk89q
 */
public abstract class OptionsPanel extends JPanel {
    
    private static final long serialVersionUID = 2156199128379585102L;
    
    protected GridBagConstraints fieldConstraints;
    protected GridBagConstraints labelConstraints;
    protected SettingsList settings;
    private List<SettingField<?>> fields = new ArrayList<SettingField<?>>();
    private JPanel groupPanel;
    protected boolean withUse;

    /**
     * Construct the panel.
     * 
     * @param settings settings to source from
     * @param withUse show checkboxes to allow the option of not specifying a setting
     */
    public OptionsPanel(SettingsList settings, boolean withUse) {
        this.settings = settings;
        this.withUse = withUse;
        
        fieldConstraints = new GridBagConstraints();
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
        fieldConstraints.insets = new Insets(2, 5, 2, 5);
        
        labelConstraints = (GridBagConstraints) fieldConstraints.clone();
        labelConstraints.weightx = 0.0;
        labelConstraints.gridwidth = 1;
        labelConstraints.insets = new Insets(1, 5, 1, 10);
        
        buildUI();
        
        copySettingsToFields();
    }
    
    /**
     * Build the UI.
     */
    private void buildUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        buildControls();
        
        // Vertical glue not working
        add(new Box.Filler(new Dimension(0, 0), new Dimension(0, 10000), new Dimension(0, 10000)));
    }
    /**
     * Pop the menu to adjust fields.
     * 
     * @param component component to display from
     * @param x top left x
     * @param y top left y
     * @param field field to set
     */
    private void popupFieldMenu(Component component, int x, int y,
            final SettingField<?> field) {
        if (settings.getParents() == null) {
            return; // Nothing to reset to!
        }
        
        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;
        
        menuItem = new JMenuItem("Reset to default"); 
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySettingToField(field, new SettingsList(settings.getParents()));
            }
        });
        popup.add(menuItem);
        
        popup.show(component, x, y);
    }
    
    /**
     * Build the controls.
     */
    protected abstract void buildControls();

    /**
     * Copy settings from {@link SettingsList} to this panel.
     */
    public void copySettingsToFields() {
        for (SettingField<?> field : fields) {
            if (withUse) {
                boolean isSet = settings.has(field.settingName);
                field.useCheck.setSelected(isSet);
                field.component.setEnabled(isSet);
                if (!isSet) {
                    continue;
                }
            }
            
            copySettingToField(field, settings);
        }
    }
    
    /**
     * Load a setting into a field.
     * 
     * @param field field
     * @param settings settings
     */
    private void copySettingToField(SettingField<?> field, SettingsList settings) {
        // Number spinners
        if (field.component instanceof JSpinner &&
                ((JSpinner) field.component).getModel() instanceof SpinnerNumberModel) {
            ((JSpinner) field.component).setValue(settings.getInt(field.settingName, 0));
        // Checkbox
        } else if (field.component instanceof JCheckBox) {
            ((JCheckBox) field.component).setSelected(settings.getBool(field.settingName, false));
        // Text
        } else if (field.component instanceof JTextField) {
            ((JTextField) field.component).setText(settings.get(field.settingName, ""));
        // ??
        } else {
            throw new IllegalArgumentException("Don't know what to do with a " +
                    field.component.getClass().getCanonicalName());
        }
    }

    /**
     * Copy settings from this panel to a {@link SettingsList}.
     */
    public void copyFieldsToSettings() {
        for (SettingField<?> field : fields) {
            if (withUse) {
                if (!field.useCheck.isSelected()) {
                    settings.unset(field.settingName);
                    continue;
                }
            }
            
            // Number spinners
            if (field.component instanceof JSpinner &&
                    ((JSpinner) field.component).getModel() instanceof SpinnerNumberModel) {
                settings.set(field.settingName, ((JSpinner) field.component).getValue());
            // Checkbox
            } else if (field.component instanceof JCheckBox) {
                settings.set(field.settingName, ((JCheckBox) field.component).isSelected());
            // Text
            } else if (field.component instanceof JTextField) {
                settings.set(field.settingName, ((JTextField) field.component).getText());
            // ??
            } else {
                throw new IllegalArgumentException("Don't know what to do with a " +
                        field.component.getClass().getCanonicalName());
            }
        }
    }

    /**
     * Create a field group and set the current active field group to the
     * created one.
     * 
     * @param title title of group
     * @return field group
     */
    protected JPanel createFieldGroup(String title) {
        JPanel parent = this;
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.setLayout(new GridBagLayout());
        parent.add(panel);
        groupPanel = panel;
        return panel;
    }

    /**
     * Add a labeled field.
     * 
     * @param settingName setting name
     * @param label label
     * @param component component to add
     * @return the component
     */
    protected <T extends Component> T addField(String settingName,
            String label, T component) {
        JPanel parent = groupPanel;
        GridBagLayout layout = (GridBagLayout) parent.getLayout();
        JCheckBox useCheck = null;
        if (withUse) {
            useCheck = buildSetCheck(component);
            useCheck.setBorder(null);
            layout.setConstraints(useCheck, labelConstraints);
            parent.add(useCheck);
        }
        JLabel labelObj = new JLabel(label);
        layout.setConstraints(labelObj, labelConstraints);
        layout.setConstraints(component, fieldConstraints);
        labelObj.setLabelFor(component);
        parent.add(labelObj);
        parent.add(component);
        final SettingField<T> field = new SettingField<T>(settingName, component, useCheck);
        fields.add(field);
        
        if (!withUse) {
            component.addMouseListener(new MouseAdapter() {
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
                        popupFieldMenu(e.getComponent(), e.getX(), e.getY(), field);
                    }
                }
            });
        }
        
        return component;
    }

    /**
     * Add a non-labeled field (like a checkbox).
     * 
     * @param settingName setting name
     * @param component component to add
     * @return the component
     */
    protected <T extends Component> T addField(String settingName, T component) {
        JPanel parent = groupPanel;
        if (component instanceof JComponent) {
            ((JComponent) component).setOpaque(false);
        }
        if (component instanceof JCheckBox) {
            ((JCheckBox) component).setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        }
        GridBagLayout layout = (GridBagLayout) parent.getLayout();
        JCheckBox useCheck = null;
        if (withUse) {
            useCheck = buildSetCheck(component);
            layout.setConstraints(useCheck, labelConstraints);
            parent.add(useCheck);
        }
        layout.setConstraints(component, fieldConstraints);
        parent.add(component);
        fields.add(new SettingField<T>(settingName, component, useCheck));
        return component;
    }

    /**
     * Build the "set" checkbox.
     * 
     * @param component component to control
     * @return checkbox
     */
    private JCheckBox buildSetCheck(final Component component) {
        final JCheckBox check = new JCheckBox("Set:");
        check.setBorder(null);
        check.setOpaque(false);
        check.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                component.setEnabled(check.isSelected());
            }
        });
        return check;
    }
    
    /**
     * Keeps tracks of fields.
     *
     * @param <T> component type
     */
    private class SettingField<T extends Component> {
        private String settingName;
        private T component;
        private JCheckBox useCheck;
        
        public SettingField(String settingName, T component, JCheckBox useCheck) {
            this.settingName = settingName;
            this.component = component;
            this.useCheck = useCheck;
        }
    }

}