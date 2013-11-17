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

import com.sk89q.mclauncher.util.PastebinPoster;
import com.sk89q.mclauncher.util.PastebinPoster.PasteCallback;
import lombok.Getter;
import lombok.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static com.sk89q.skmcl.util.SharedLocale._;

/**
 * A frame capable of showing messages.
 */
public class ConsoleFrame extends JFrame {

    protected static final Image TRAY_OK_IMAGE;
    protected static final Image TRAY_CLOSED_IMAGE;

    static {
        TRAY_OK_IMAGE = SwingHelper.readIconImage("/resources/tray_ok.png");
        TRAY_CLOSED_IMAGE = SwingHelper.readIconImage("/resources/tray_closed.png");
    }

    @Getter
    private final MessageLog messageLog;
    @Getter
    private LinedBoxPanel buttonsPanel;

    /**
     * Construct the frame.
     *
     * @param numLines number of lines to show at a time
     * @param colorEnabled true to enable a colored console
     */
    public ConsoleFrame(int numLines, boolean colorEnabled) {
        this(_("console.title"), numLines, colorEnabled);
    }

    /**
     * Construct the frame.
     * 
     * @param title the title of the window
     * @param numLines number of lines to show at a time
     * @param colorEnabled true to enable a colored console
     */
    public ConsoleFrame(@NonNull String title, int numLines, boolean colorEnabled) {
        messageLog = new MessageLog(numLines, colorEnabled);

        setTitle(title);
        if (TRAY_OK_IMAGE != null) {
            setIconImage(TRAY_OK_IMAGE);
        }
        setSize(new Dimension(650, 400));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initComponents();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                performClose();
            }
        });
    }

    /**
     * Add components to the frame.
     */
    private void initComponents() {
        JButton pastebinButton = new JButton(_("console.uploadLog"));
        buttonsPanel = new LinedBoxPanel(true);

        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        buttonsPanel.addElement(pastebinButton);

        add(buttonsPanel, BorderLayout.NORTH);
        add(messageLog, BorderLayout.CENTER);

        pastebinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pastebinLog();
            }
        });
    }

    /**
     * Attempt to perform window close.
     */
    protected void performClose() {
        messageLog.detachGlobalHandler();
        dispose();
    }

    /**
     * Send the contents of the message log to a pastebin.
     */
    private void pastebinLog() {
        String text = messageLog.getPastableText();
        messageLog.log(_("console.paste.uploading", text.length()) + "\n",
                messageLog.asHighlighted());

        PastebinPoster.paste(text, new PasteCallback() {
            @Override
            public void handleSuccess(String url) {
                messageLog.log(_("console.paste.uploaded", url) +
                        "\n", messageLog.asHighlighted());
                SwingHelper.openURL(url, messageLog);
            }

            @Override
            public void handleError(String err) {
                messageLog.log(_("console.paste.uploadFailed", err) +
                        "\n", messageLog.asError());
            }
        });
    }

}
