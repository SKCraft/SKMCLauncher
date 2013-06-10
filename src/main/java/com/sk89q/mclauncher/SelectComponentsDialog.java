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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;

import com.sk89q.mclauncher.model.Component;
import com.sk89q.mclauncher.model.ComponentTableModel;
import com.sk89q.mclauncher.model.PackageManifest;
import com.sk89q.mclauncher.util.ActionListeners;

public class SelectComponentsDialog extends JDialog {

    private static final long serialVersionUID = -8771235345163527656L;
    
    private final PackageManifest manifest;
    
    private JTable table;

    public SelectComponentsDialog(JFrame frame, PackageManifest manifest) {
        super(frame, "Select Install Options", true);

        this.manifest = manifest;

        setResizable(true);
        buildUI();
        pack();
        setSize(320, 500);
        setLocationRelativeTo(frame);
    }

    private void buildUI() {
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout(8, 8));
        container.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        JLabel infoLabel = new JLabel(
                "<html>To change these later, select 'Re-select install options' " +
                "after clicking 'More Options' on the launcher.");
        container.add(infoLabel, BorderLayout.NORTH);

        table = new JTable() {
            private static final long serialVersionUID = 4897191210730076435L;

            @Override
            public java.awt.Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                java.awt.Component c = super.prepareRenderer(renderer, row, col);
                if (col == 0) {
                    c.setEnabled(!((Component) (getModel()).getValueAt(row, 1)).isRequired());
                }
                return c;
            }
        };
        
        table.setTableHeader(null);
        table.setShowGrid(false);
        table.setRowHeight(table.getRowHeight() + 2);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setModel(new ComponentTableModel(manifest.getComponents()));
        table.getColumnModel().getColumn(0).setMaxWidth(30);
        JScrollPane tableScroll = new JScrollPane(table);
        container.add(tableScroll, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout(0, 8));
        container.add(bottomPanel, BorderLayout.SOUTH);
        
        JLabel descLabel = new JLabel("<html><b>Description:</b>");
        bottomPanel.add(descLabel, BorderLayout.NORTH);
        
        final JTextArea descArea = new JTextArea("Select an option to see its description.") {
            private static final long serialVersionUID = 4339941505774335040L;

            @Override
            public Dimension getPreferredSize() {
                return new Dimension((int) super.getPreferredSize().getWidth(), 100);
            }
        };
        descArea.setFont(descLabel.getFont());
        descArea.setBackground(descLabel.getBackground());
        descArea.setEditable(false);
        descArea.setWrapStyleWord(true);
        descArea.setLineWrap(true);
        JScrollPane descScroll = new JScrollPane(descArea, 
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        bottomPanel.add(descScroll, BorderLayout.CENTER);

        add(container, BorderLayout.CENTER);
        
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Component component = (Component) (table.getModel()).getValueAt(table.getSelectedRow(), 1);
                descArea.setText(component.getDescription());
            }
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 5));
        JButton installButton = new JButton("Continue...");
        buttonsPanel.add(installButton);
        bottomPanel.add(buttonsPanel, BorderLayout.SOUTH);

        installButton.addActionListener(ActionListeners.dipose(this));
    }

}
