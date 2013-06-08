package com.sk89q.mclauncher.util;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.config.Def;

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
        trayOkImage = UIUtil.readIconImage("/resources/tray_ok.png");
        trayClosedImage = UIUtil.readIconImage("/resources/tray_closed.png");
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
    
    /**
     * Confirm a force kill attempt.
     * 
     * @return true to continue force closing
     */
    private boolean confirmKill() {
        boolean confirmKill = 
                Launcher.getInstance().getOptions().getSettings().getBool(
                        Def.CONSOLE_CONFIRM_KILL, true);// Confirm the kill process
        
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
    
    @Override
    protected void tryClose() {
        if (killProcess) {
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
        
        killButton.addActionListener(killProcessListener);
        
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
        
        trayIcon.addActionListener(reshowWindowListener);
       
        PopupMenu popup = new PopupMenu();
        MenuItem item;

        popup.add(item = new MenuItem("SKMCLauncher"));
        item.setEnabled(false);

        popup.add(item = new MenuItem("Show messages and errors"));
        item.addActionListener(reshowWindowListener);

        popup.add(item = new MenuItem("Kill game"));
        item.addActionListener(killProcessListener);
       
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
        Thread thread = new Thread(new ProcessTracker());
        thread.setDaemon(true);
        thread.start();
    }
    
    private ActionListener killProcessListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
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
            
            detachGlobalHandler();
        }
    };
    
    private ActionListener reshowWindowListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            reshow();
        }
    };
    
    private class ProcessTracker implements Runnable {
        @Override
        public void run() {
            final PrintWriter out = new PrintWriter(
                    getOutputStream(Color.MAGENTA), true);
            
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

                    UIUtil.setIconImage(self, "/resources/tray_closed.png");
                    reshowWindowListener.actionPerformed(null);
                }
            });
        }
    }

}
