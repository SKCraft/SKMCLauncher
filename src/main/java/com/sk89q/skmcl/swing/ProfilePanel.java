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

import com.sk89q.skmcl.profile.Profile;
import com.sk89q.skmcl.util.ResourceUserAgent;
import lombok.Getter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xhtmlrenderer.simple.XHTMLPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

import static com.sk89q.skmcl.util.SharedLocale._;

public class ProfilePanel extends JPanel implements ListSelectionListener {

    private final JList list;
    private final JLabel titleLabel = new JLabel();
    @Getter
    private JButton launchButton;
    @Getter
    private JButton editProfileButton;

    public ProfilePanel(JList list) {
        this.list = list;
        list.getSelectionModel().addListSelectionListener(this);

        initComponents();
    }

    private void initComponents() {
        setLayout(new ProfileDisplayLayout());

        LinedBoxPanel buttonsPanel = new LinedBoxPanel(true).fullyPadded();
        launchButton = new JButton(_("launcher.launch"));
        editProfileButton = new JButton(_("launcher.editProfile"));
        XHTMLPanel view = new XHTMLPanel(new ResourceUserAgent());
        view.getSharedContext().getTextRenderer().setSmoothingThreshold(6f);

        Font font = launchButton.getFont().deriveFont(13f);
        launchButton.setFont(font);
        editProfileButton.setFont(font);

        buttonsPanel.setBorder(BorderFactory.createEmptyBorder());
        SwingHelper.removeOpaqueness(buttonsPanel);
        titleLabel.setFont(titleLabel.getFont().deriveFont(35f));

        buttonsPanel.addElement(launchButton);
        buttonsPanel.addElement(editProfileButton);

        add(titleLabel, ProfileDisplayLayout.TITLE);
        add(buttonsPanel, ProfileDisplayLayout.BUTTONS);
        add(view, BorderLayout.CENTER);

        view.setDocument("/resources/default.html");
        setDocumentStyle(view);
    }

    private static void setDocumentStyle(XHTMLPanel view) {
        JLabel label = new JLabel();
        Font font = label.getFont();

        String css =
                ".launcher-font-style { " +
                    "font-family: '" + font.getFontName() + "';" +
                    "font-size: " + font.getSize2D() + "pt;" +
                "}";

        Document doc = view.getDocument();
        Element style = doc.createElement("style");
        style.appendChild(doc.createTextNode(css));
        doc.getElementsByTagName("head").item(0).appendChild(style);
        view.setDocument(doc);
    }

    public void setProfile(Profile profile) {
        titleLabel.setText(profile.getName());
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            setProfile((Profile) list.getSelectedValue());
        }
    }

    private static class ProfileDisplayLayout extends BorderLayout {

        public static final int PADDING = 20;
        public static final String TITLE = "title";
        public static final String BUTTONS = "buttons";

        private Component title;
        private Component buttons;

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
            if ((constraints == null) || (constraints instanceof String)) {
                addLayoutComponent((String)constraints, comp);
            } else {
                throw new IllegalArgumentException("Invalid constraint");
            }
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
            if (TITLE.equalsIgnoreCase(name)) {
                title = comp;
            } else if (BUTTONS.equalsIgnoreCase(name)) {
                buttons = comp;
            } else {
                super.addLayoutComponent(name, comp);
            }
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            super.removeLayoutComponent(comp);

            if (title == comp) {
                title = null;
            } else if (buttons == comp) {
                buttons = null;
            }
        }

        public void layoutContainer(Container target) {
            Component comp;
            Insets insets = target.getInsets();
            int top = insets.top;
            int bottom = target.getHeight() - insets.bottom;
            int left = insets.left;
            int right = target.getWidth() - insets.right;
            int buttonsYOffset = 0;

            if ((comp = title) != null) {
                Dimension d = comp.getPreferredSize();
                comp.setBounds(left + PADDING, top + PADDING, right - left, d.height);
                buttonsYOffset += d.height + PADDING;
            }

            if ((comp = buttons) != null) {
                Dimension d = comp.getPreferredSize();
                comp.setBounds(left + PADDING, top + buttonsYOffset + 10, right - left, d.height);
            }

            super.layoutContainer(target);
        }
    }

}
