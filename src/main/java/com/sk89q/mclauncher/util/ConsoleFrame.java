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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Console dialog for showing console messages.
 * 
 * @author sk89q
 */
public class ConsoleFrame extends JFrame {

    private static final long serialVersionUID = -3266712569265372777L;
    
    private Process trackProc;
    private JTextComponent textComponent;
    private Document document;
    private int numLines;
    private boolean colorEnabled = false;
    private SimpleAttributeSet defaultAttributes = new SimpleAttributeSet();
    private SimpleAttributeSet highlightedAttributes;
    
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
     * @param trackProc process to track
     * @param killProcess true to kill the process on console close
     */
    public ConsoleFrame(int numLines, boolean colorEnabled,
            final Process trackProc, final boolean killProcess) {
        super("Console");
        this.numLines = numLines;
        this.colorEnabled = colorEnabled;
        this.trackProc = trackProc;
        
        this.highlightedAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(highlightedAttributes, Color.BLACK);
        StyleConstants.setBackground(highlightedAttributes, Color.YELLOW);
        
        setSize(new Dimension(650, 400));
        buildUI();
        
        if (trackProc != null) {
            track(trackProc);
        }

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (trackProc != null && killProcess) {
                    trackProc.destroy();
                }
                event.getWindow().dispose();
            }
        });
    }
    
    /**
     * Build the interface.
     */
    private void buildUI() {
        if (colorEnabled) {
            JTextPane text = new JTextPane();
            this.textComponent = text;
        } else {
            JTextArea text = new JTextArea();
            this.textComponent = text;
            text.setLineWrap(true);
        }
        
        textComponent.setFont(UIUtil.getMonospaceFont());
        textComponent.setEditable(false);
        DefaultCaret caret = (DefaultCaret) textComponent.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        document = textComponent.getDocument();
        document.addDocumentListener(new LimitLinesDocumentListener(numLines, true));
        
        JScrollPane scrollText = new JScrollPane(textComponent);
        scrollText.setBorder(null);
        scrollText.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        
        add(scrollText, BorderLayout.CENTER);
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
                } catch (InterruptedException e) {
                    out.println("Process tracking interrupted!");
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
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

}
