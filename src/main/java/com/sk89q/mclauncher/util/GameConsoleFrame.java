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

import com.sk89q.skmcl.swing.SwingHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;

public class GameConsoleFrame extends ConsoleFrame {
    
    private static final long serialVersionUID = -737598535474470253L;
    
    private static final Image trayOkImage;
    private static final Image trayClosedImage;
    
    private JButton killButton;
    private JButton minimizeButton;
    private TrayIcon trayIcon;

    private Process trackProc;
    private boolean killProcess;
    
    static {
        trayOkImage = SwingHelper.readIconImage("/resources/tray_ok.png");
        trayClosedImage = SwingHelper.readIconImage("/resources/tray_closed.png");
    }

    public GameConsoleFrame(
            int numLines, boolean colorEnabled, Process proc, boolean killProcess) {
        super("Messages and Errors", numLines, colorEnabled);

        this.trackProc = proc;
        this.killProcess = killProcess;
        
        if (proc != null) {
            track(proc);
        }
    }
    
    @Override
    protected void tryClose() {
        if (killProcess && trackProc != null) {
            if (!confirmKill()) {
                return;
            }
            
            trackProc.destroy();
            trackProc = null;
        }
        
        if (trackProc == null || trayIcon == null) {
            if (trayIcon != null) {
                SystemTray.getSystemTray().remove(trayIcon);
            }
            
            super.tryClose();
        } else {
            minimize();
        }
    }
    
    /**
     * Confirm a force kill attempt.
     * 
     * @return true to continue force closing
     */
    private boolean confirmKill() {
        boolean confirmKill = true; // @TODO
        
        if (confirmKill && JOptionPane
                .showConfirmDialog(
                        this,
                        "Are you sure? If you're in a single player game, this can " +
                        "make you lose your progress!",
                        "Force Close", JOptionPane.YES_NO_OPTION) != 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Try to just kill.
     */
    private void tryKill() {
        if (!confirmKill()) {
            return;
        }
        
        if (killButton != null) {
            killButton.setEnabled(false);
        }
        
        if (trackProc != null) {
            trackProc.destroy();
            trackProc = null;
        }
        
        messageLog.detachGlobalHandler();
    }
    
    private void minimize() {
        setVisible(false);
    }
    
    private void reshow() {
        self.setVisible(true);
        self.requestFocus();
    }
    
    @Override
    protected void addComponents() {
        super.addComponents();
        
        killButton = new JButton("Force Close");
        minimizeButton = new JButton("Hide Window");
        
        buttonsPanel.add(Box.createHorizontalGlue());
        buttonsPanel.add(killButton);
        buttonsPanel.add(Box.createHorizontalStrut(5));
        buttonsPanel.add(minimizeButton);
        
        killButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tryKill();
            }
        });
        
        minimizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                minimize();
            }
        });
        
        if (!setupTrayIcon()) {
            minimizeButton.setEnabled(true);
        }
    }
    
    private boolean setupTrayIcon() {
        if (!SystemTray.isSupported() || trayOkImage == null || trayClosedImage == null) {
            return false;
        }
        
        trayIcon = new TrayIcon(trayOkImage);
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("Messages and Errors for SKMCLauncher");
        
        trayIcon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reshow();
            }
        });
       
        PopupMenu popup = new PopupMenu();
        MenuItem item;

        popup.add(item = new MenuItem("SKMCLauncher"));
        item.setEnabled(false);

        popup.add(item = new MenuItem("Show messages and errors"));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reshow();
            }
        });

        popup.add(item = new MenuItem("Kill game"));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tryKill();
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

    /**
     * Track a process in a separate daemon thread.
     * 
     * @param process process
     */
    private void track(Process process) {
        final PrintWriter out = new PrintWriter(
                messageLog.getOutputStream(Color.MAGENTA), true);
        
        Thread thread = new Thread(new ProcessTracker(out));
        thread.setName("Console Game Process Tracker");
        thread.setDaemon(true);
        thread.start();
    }
    
    private class ProcessTracker implements Runnable {
        private final PrintWriter out;
        
        public ProcessTracker(PrintWriter out) {
            this.out = out;
        }

        @Override
        public void run() {
            try {
                int code = trackProc.waitFor();
                out.println("Process ended with code " + code);
                out.println("Minecraft is no longer running! "
                        + "Click 'Close Window' to close this window.");
                out.println("Did you know: In 'Options', under 'Environment', you "
                        + "can disable this window from appearing.");
                trackProc = null;
            } catch (InterruptedException e) {
                out.println("Process tracking interrupted!");
            }
            
            if (!running) {
                return;
            }
            
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (killButton != null) {
                        killButton.setEnabled(false);
                    }
                    
                    if (minimizeButton != null) {
                        minimizeButton.setText("Close Window");
                        minimizeButton.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                tryClose();
                            }
                        });
                    }
                    
                    if (trayIcon != null) {
                        trayIcon.setImage(trayClosedImage);
                    }

                    SwingHelper.setIconImage(self, "/resources/tray_closed.png");
                    reshow();
                }
            });
        }
    }

}
