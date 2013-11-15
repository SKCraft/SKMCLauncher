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

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TextFieldPopupMenu extends JPopupMenu implements ActionListener {

    public static final TextFieldPopupMenu INSTANCE = new TextFieldPopupMenu();

    private final JMenuItem cutItem;
    private final JMenuItem copyItem;
    private final JMenuItem pasteItem;
    private final JMenuItem deleteItem;
    private final JMenuItem selectAllItem;

    private TextFieldPopupMenu() {
        cutItem = addMenuItem(new JMenuItem("Cut", 'T'));
        copyItem = addMenuItem(new JMenuItem("Copy", 'C'));
        pasteItem = addMenuItem(new JMenuItem("Paste", 'P'));
        deleteItem = addMenuItem(new JMenuItem("Delete", 'D'));
        addSeparator();
        selectAllItem = addMenuItem(new JMenuItem("Select All", 'A'));
    }

    private JMenuItem addMenuItem(JMenuItem item) {
        item.addActionListener(this);
        return add(item);
    }

    @Override
    public void show(Component invoker, int x, int y) {
        JTextComponent textComponent = (JTextComponent) invoker;
        boolean editable = textComponent.isEditable() && textComponent.isEnabled();
        cutItem.setVisible(editable);
        pasteItem.setVisible(editable);
        deleteItem.setVisible(editable);
        super.show(invoker, x, y);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JTextComponent textComponent = (JTextComponent) getInvoker();
        textComponent.requestFocus();

        boolean haveSelection =
                textComponent.getSelectionStart() != textComponent.getSelectionEnd();

        if (e.getSource() == cutItem) {
            if (!haveSelection) textComponent.selectAll();
            textComponent.cut();
        } else if (e.getSource() == copyItem) {
            if (!haveSelection) textComponent.selectAll();
            textComponent.copy();
        } else if (e.getSource() == pasteItem) {
            textComponent.paste();
        } else if (e.getSource() == deleteItem) {
            if (!haveSelection) textComponent.selectAll();
            textComponent.replaceSelection("");
        } else if (e.getSource() == selectAllItem) {
            textComponent.selectAll();
        }
    }
}