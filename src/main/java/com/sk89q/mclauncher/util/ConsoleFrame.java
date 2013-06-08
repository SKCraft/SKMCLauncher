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

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import com.sk89q.mclauncher.util.PastebinPoster.PasteCallback;

/**
 * Console dialog for showing console messages.
 */
public class ConsoleFrame extends JFrame implements PasteCallback {

    private static final long serialVersionUID = -3266712569265372777L;
    private static final Logger rootLogger = Logger.getLogger("");
    private static final Image trayOkImage;
    private static final Image trayClosedImage;

    private final ConsoleFrame self = this;
    private boolean running = true;
    
    private Process trackProc;
    private boolean killProcess;
    private Handler loggerHandler;
    private JTextComponent textComponent;
    private JButton killButton;
    private JButton minimizeButton;
    private Document document;
    private int numLines;
    private boolean colorEnabled = false;
    private TrayIcon trayIcon;
    private final SimpleAttributeSet defaultAttributes = new SimpleAttributeSet();
    private final SimpleAttributeSet highlightedAttributes;
    private final SimpleAttributeSet errorAttributes;
    private final SimpleAttributeSet infoAttributes;
    private final SimpleAttributeSet debugAttributes;
    
    static {
        trayOkImage = UIUtil.readIconImage("/resources/tray_ok.png");
        trayClosedImage = UIUtil.readIconImage("/resources/tray_closed.png");
    }
    
    /**
     * Construct the frame.
     * 
     * @param numLines number of lines to show at a time
     * @param colorEnabled true to enable a colored console
     */
    public ConsoleFrame(int numLines, boolean colorEnabled) {
        this(numLines, colorEnabled, null, false);
    }

    /**
     * Construct the frame.
     * 
     * @param numLines number of lines to show at a time
     * @param colorEnabled true to enable a colored console
     * @param proc process to track
     * @param killProcess true to kill the process on console close
     */
    public ConsoleFrame(int numLines, boolean colorEnabled,
            final Process proc, final boolean killProcess) {
        super("Messages and Errors");

        UIUtil.setIconImage(this, "/resources/tray_ok.png");
        
        this.numLines = numLines;
        this.colorEnabled = colorEnabled;
        this.trackProc = proc;
        this.killProcess = killProcess;
        
        this.highlightedAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(highlightedAttributes, Color.BLACK);
        StyleConstants.setBackground(highlightedAttributes, Color.YELLOW);
        
        this.errorAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(errorAttributes, new Color(200, 0, 0));
        this.infoAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(infoAttributes, new Color(200, 0, 0));
        this.debugAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(debugAttributes, Color.DARK_GRAY);
        
        setSize(new Dimension(650, 400));
        addComponents();
        
        if (proc != null) {
            track(proc);
        }

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                close();
            }
        });
    }
    
    /**
     * Try to close.
     */
    private void close() {
        if (trackProc != null && killProcess) {
            trackProc.destroy();
            trackProc = null;
        }
        if (trackProc == null) {
            running = false;
            
            // Tell threads waiting on us that we're done
            synchronized (self) {
                self.notifyAll();
            }
            
            if (trayIcon != null) {
                SystemTray.getSystemTray().remove(trayIcon);
            }
            
            if (loggerHandler != null) {
                rootLogger.removeHandler(loggerHandler);
            }
            
            dispose();
        } else {
            self.setVisible(false);
        }
    }
    
    /**
     * Build the interface.
     */
    private void addComponents() {
        if (colorEnabled) {
            JTextPane text = new JTextPane() {
                private static final long serialVersionUID = 6814733823000144811L;

                @Override
                public boolean getScrollableTracksViewportWidth()
                {
                    return true;
                }
            };
            this.textComponent = text;
        } else {
            JTextArea text = new JTextArea();
            this.textComponent = text;
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
        }
        
        textComponent.setFont(new JLabel().getFont());
        textComponent.setEditable(false);
        DefaultCaret caret = (DefaultCaret) textComponent.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        document = textComponent.getDocument();
        document.addDocumentListener(new LimitLinesDocumentListener(numLines, true));
        
        JScrollPane scrollText = new JScrollPane(textComponent);
        scrollText.setBorder(null);
        scrollText.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollText.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        Box buttonsPanel = Box.createHorizontalBox();
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JButton pastebinButton = new JButton("Upload Log...");
        buttonsPanel.add(pastebinButton);
        buttonsPanel.add(Box.createHorizontalStrut(5));
        add(buttonsPanel, BorderLayout.NORTH);
        
        pastebinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = getPastableText();
                log("Uploading paste (" + text.length() + " bytes)...\n", 
                        highlightedAttributes);
                PastebinPoster.paste(text, self);
            }
        });
        
        if (trackProc != null) {
            killButton = new JButton("Kill Process");
            minimizeButton = new JButton("Hide Log");
            UIUtil.equalWidth(killButton, minimizeButton);
            
            buttonsPanel.add(Box.createHorizontalGlue());
            buttonsPanel.add(killButton);
            buttonsPanel.add(Box.createHorizontalStrut(5));
            buttonsPanel.add(minimizeButton);
            
            killButton.addActionListener(killProcessListener);
            
            minimizeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    self.setVisible(false);
                }
            });
            
            if (!setupTrayIcon()) {
                minimizeButton.setEnabled(true);
            }
        }
        
        add(scrollText, BorderLayout.CENTER);
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
    
    public String getPastableText() {
        String text = textComponent.getText().replaceAll("[\r\n]+", "\n");
        text = text.replaceAll("Session ID is [A-Fa-f0-9]+", "Session ID is [redacted]");
        return text;
    }

    @Override
    public void handleSuccess(String url) {
        log("Paste uploaded to: " + url + "\n", highlightedAttributes);
        UIUtil.openURL(url, this);
    }

    @Override
    public void handleError(String err) {
        log("Failed to upload paste: " + err + "\n", errorAttributes);
    }

    /**
     * Log a message.
     * 
     * @param line line
     */
    public void log(String line) {
        log(line, null);
    }
    
    /**
     * Log a message given the {@link AttributeSet}.
     * 
     * @param line line
     * @param attributes attribute set, or null for none
     */
    public void log(String line, AttributeSet attributes) {
        if (colorEnabled) {
            if (line.startsWith("(!!)")) {
                attributes = highlightedAttributes;
            }
        }
        
        try {
            int offset = document.getLength();
            document.insertString(offset, line,
                    (attributes != null && colorEnabled) ? attributes : defaultAttributes);
            textComponent.setCaretPosition(document.getLength());
        } catch (BadLocationException ble) {
        
        }
    }
    
    /**
     * Get an output stream that can be written to.
     * 
     * @return output stream
     */
    public ConsoleOutputStream getOutputStream() {
        return getOutputStream((AttributeSet) null);
    }
    
    /**
     * Get an output stream with the given attribute set.
     * 
     * @param attributes attributes
     * @return output stream
     */
    public ConsoleOutputStream getOutputStream(AttributeSet attributes) {
        return new ConsoleOutputStream(attributes);
    }

    /**
     * Get an output stream using the give color.
     * 
     * @param color color to use
     * @return output stream
     */
    public ConsoleOutputStream getOutputStream(Color color) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        return getOutputStream(attributes);
    }
    
    /**
     * Consume an input stream and print it to the dialog. The consumer
     * will be in a separate daemon thread.
     * 
     * @param from stream to read
     */
    public void consume(InputStream from) {
        consume(from, getOutputStream());
    }

    /**
     * Consume an input stream and print it to the dialog. The consumer
     * will be in a separate daemon thread.
     * 
     * @param from stream to read
     * @param color color to use
     */
    public void consume(InputStream from, Color color) {
        consume(from, getOutputStream(color));
    }

    /**
     * Consume an input stream and print it to the dialog. The consumer
     * will be in a separate daemon thread.
     * 
     * @param from stream to read
     * @param attributes attributes
     */
    public void consume(InputStream from, AttributeSet attributes) {
        consume(from, getOutputStream(attributes));
    }
    
    /**
     * Internal method to consume a stream.
     * 
     * @param from stream to consume
     * @param outputStream console stream to write to
     */
    private void consume(InputStream from, ConsoleOutputStream outputStream) {
        final InputStream in = from;
        final PrintWriter out = new PrintWriter(outputStream, true);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                try {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        String s = new String(buffer, 0, len);
                        System.out.print(s);
                        out.append(s);
                        out.flush();
                    }
                } catch (IOException e) {
                } finally {
                    Util.close(in);
                    Util.close(out);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Track a process in a separate daemon thread.
     * 
     * @param process process
     */
    private void track(Process process) {
        final PrintWriter out = new PrintWriter(getOutputStream(Color.MAGENTA), true);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int code = trackProc.waitFor();
                    out.println("Process ended with code " + code);
                    trackProc = null;
                } catch (InterruptedException e) {
                    out.println("Process tracking interrupted!");
                }
                
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (killButton != null) {
                            killButton.setEnabled(false);
                        }
                        if (minimizeButton != null) {
                            minimizeButton.setText("Close Log");
                            minimizeButton.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    close();
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
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Register a global logger listener.
     */
    public void registerLoggerHandler() {
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        
        loggerHandler = new ConsoleLoggerHandler();
        rootLogger.addHandler(loggerHandler);
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
    
    private ActionListener killProcessListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (killButton != null) {
                killButton.setEnabled(false);
            }
            
            if (trackProc != null) {
                trackProc.destroy();
                trackProc = null;
            }
            
            if (loggerHandler != null) {
                rootLogger.removeHandler(loggerHandler);
                loggerHandler = null;
            }
        }
    };
    
    private ActionListener reshowWindowListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            self.setVisible(true);
            self.requestFocus();
        }
    };
    
    /**
     * Used to send console messages to the console.
     */
    public class ConsoleOutputStream extends ByteArrayOutputStream {
        private AttributeSet attributes;
        
        private ConsoleOutputStream(AttributeSet attributes) {
            this.attributes = attributes;
        }
        
        @Override
        public void flush() {
            String data = toString();
            if (data.length() == 0) return;
            log(data, attributes);
            reset();
        }
    }

    /**
     * Used to send logger messages to the console.
     */
    private class ConsoleLoggerHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            Level level = record.getLevel();
            Throwable t = record.getThrown();
            AttributeSet attributes = defaultAttributes;

            if (level.intValue() >= Level.WARNING.intValue()) {
                attributes = errorAttributes;
            } else if (level.intValue() < Level.INFO.intValue()) {
                attributes = debugAttributes;
            }

            log(record.getMessage() + "\n", attributes);
            if (t != null) {
                log(Util.getStackTrace(t) + "\n", attributes);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

}
