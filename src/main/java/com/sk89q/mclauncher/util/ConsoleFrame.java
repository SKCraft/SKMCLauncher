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

package com.sk89q.mclauncher.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.sk89q.mclauncher.util.PastebinPoster.PasteCallback;

public class ConsoleFrame extends JFrame implements PasteCallback {

    private static final long serialVersionUID = -3266712569265372777L;

    protected final ConsoleFrame self = this;

    protected boolean running = true;
    protected final MessageLog messageLog;
    protected Box buttonsPanel;

    /**
     * Construct the frame.
     * 
     * @param numLines number of lines to show at a time
     * @param colorEnabled true to enable a colored console
     */
    public ConsoleFrame(int numLines, boolean colorEnabled) {
        this("Console", numLines, colorEnabled);
    }

    /**
     * Construct the frame.
     * 
     * @param title the title of the window
     * @param numLines number of lines to show at a time
     * @param colorEnabled true to enable a colored console
     */
    public ConsoleFrame(String title, int numLines, boolean colorEnabled) {
        super(title);
        
        UIUtil.setIconImage(this, "/resources/tray_ok.png");
        
        messageLog = new MessageLog(numLines, colorEnabled);
        
        setSize(new Dimension(650, 400));
        addComponents();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                tryClose();
            }
        });
    }

    /**
     * Return whether the console is still active.
     * 
     * @return true if still active
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Get the message log.
     * 
     * @return the message log
     */
    public MessageLog getMessageLog() {
        return messageLog;
    }
    
    /**
     * Try to close.
     */
    protected void tryClose() {
        synchronized (self) {
            if (!running) {
                return; // Already closed
            }
            
            running = false;

            // Tell threads waiting on us that we're done
            self.notifyAll();
        }
        
        messageLog.detachGlobalHandler();
        dispose();
    }
    
    /**
     * Build the interface.
     */
    protected void addComponents() {
        buttonsPanel = Box.createHorizontalBox();
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JButton pastebinButton = new JButton("Upload Log...");
        buttonsPanel.add(pastebinButton);
        buttonsPanel.add(Box.createHorizontalStrut(5));
        add(buttonsPanel, BorderLayout.NORTH);
        
        pastebinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = messageLog.getPastableText();
                messageLog.log("Uploading paste (" + text.length() + " bytes)...\n",
                        messageLog.asHighlighted());
                PastebinPoster.paste(text, self);
            }
        });
        
        add(messageLog, BorderLayout.CENTER);
    }

    @Override
    public void handleSuccess(String url) {
        messageLog.log("Paste uploaded to: " + url + "\n", messageLog.asHighlighted());
        UIUtil.openURL(url, this);
    }

    @Override
    public void handleError(String err) {
        messageLog.log("Failed to upload paste: " + err + "\n", messageLog.asError());
    }

    /**
     * Log a message.
     * 
     * @param line line
     */
    public void log(String line) {
        messageLog.log(line, null);
    }

    /**
     * Wait until the console is closed.
     */
    public void waitFor() {
        while (running) {
            try {
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

}
