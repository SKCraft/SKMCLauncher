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

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;

import static com.sk89q.skmcl.util.SharedLocale._;

/**
 * A version of the console window that can manage a process.
 */
public class ProcessConsoleFrame extends ConsoleFrame {
    
    private JButton killButton;
    private JButton minimizeButton;
    private TrayIcon trayIcon;

    @Getter
    private Process process;
    @Getter @Setter
    private boolean killOnClose;

    private PrintWriter processOut;

    /**
     * Create a new instance of the frame.
     *
     * @param numLines the number of log lines
     * @param colorEnabled whether color is enabled in the log
     */
    public ProcessConsoleFrame(int numLines, boolean colorEnabled) {
        super(_("processConsole.title"), numLines, colorEnabled);
        processOut = new PrintWriter(
                getMessageLog().getOutputStream(new Color(166, 252, 219)), true);
        initComponents();
        updateComponents();
    }

    public synchronized void setProcess(Process process) {
        try {
            Process lastProcess = this.process;
            if (lastProcess != null) {
                processOut.println(
                        _("processConsole.endedWithCode", lastProcess.exitValue()));
            }
        } catch (IllegalThreadStateException e) {
        }

        if (process != null) {
            processOut.println(_("processConsole.attachedTo", process.toString()));
        }

        this.process = process;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateComponents();
            }
        });
    }

    private synchronized boolean hasProcess() {
        return process != null;
    }

    @Override
    protected void performClose() {
        if (hasProcess()) {
            if (killOnClose) {
                performKill();
            }
        }

        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }

        super.performClose();
    }

    private void performKill() {
        if (!confirmKill()) {
            return;
        }

        synchronized (this) {
            if (hasProcess()) {
                process.destroy();
                setProcess(null);
            }
        }

        updateComponents();
    }

    protected void initComponents() {
        killButton = new JButton(_("processConsole.killApplication"));
        minimizeButton = new JButton(); // Text set later

        LinedBoxPanel buttonsPanel = getButtonsPanel();
        buttonsPanel.addGlue();
        buttonsPanel.addElement(killButton);
        buttonsPanel.addElement(minimizeButton);
        
        killButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performKill();
            }
        });

        minimizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                contextualClose();
            }
        });
        
        if (!setupTrayIcon()) {
            minimizeButton.setEnabled(true);
        }
    }

    private boolean setupTrayIcon() {
        if (!SystemTray.isSupported() ||
                TRAY_OK_IMAGE == null ||
                TRAY_CLOSED_IMAGE == null) {
            return false;
        }

        trayIcon = new TrayIcon(TRAY_OK_IMAGE);
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip(_("processConsole.tray.toolTip"));

        trayIcon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reshow();
            }
        });
       
        PopupMenu popup = new PopupMenu();
        MenuItem item;

        popup.add(item = new MenuItem(_("launcher.title")));
        item.setEnabled(false);

        popup.add(item = new MenuItem(_("processConsole.tray.showWindow")));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reshow();
            }
        });

        popup.add(item = new MenuItem(_("processConsole.tray.killApplication")));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performKill();
            }
        });
       
        trayIcon.setPopupMenu(popup);
       
        try {
            SystemTray tray = SystemTray.getSystemTray();
            tray.add(trayIcon);
            return true;
        } catch (AWTException e) {
        }
        
        return false;
    }

    private synchronized void updateComponents() {
        Image icon = hasProcess() ? TRAY_OK_IMAGE : TRAY_CLOSED_IMAGE;

        killButton.setEnabled(hasProcess());

        if (!hasProcess() || trayIcon == null) {
            minimizeButton.setText(_("processConsole.closeWindow"));
        } else {
            minimizeButton.setText(_("processConsole.hideWindow"));
        }

        if (trayIcon != null) {
            trayIcon.setImage(icon);
        }

        setIconImage(icon);
    }

    private synchronized void contextualClose() {
        if (!hasProcess() || trayIcon == null) {
            performClose();
        } else {
            minimize();
        }

        updateComponents();
    }

    private boolean confirmKill() {
        return SwingHelper.confirmDialog(
                this,
                _("processConsole.killConfirm.message"),
                _("processConsole.killConfirm.title"));
    }

    private void minimize() {
        setVisible(false);
    }

    private void reshow() {
        setVisible(true);
        requestFocus();
    }

}
