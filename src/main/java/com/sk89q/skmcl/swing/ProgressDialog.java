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

import com.sk89q.skmcl.concurrent.WorkerService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Observable;
import java.util.Observer;

import static com.sk89q.skmcl.util.SharedLocale._;

public class ProgressDialog extends JDialog implements Observer {

    private final ProgressDialog self = this;
    private final String defaultTitle = _("progressDialog.title");
    private final String defaultStatus = _("progressDialog.working");
    private final WorkerService workerService;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton cancelButton;

    public ProgressDialog(Window owner, WorkerService workerService) {
        super(owner, _("progressDialog.title"), Dialog.ModalityType.DOCUMENT_MODAL);

        this.workerService = workerService;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                tryCancelling();
            }
        });

        initComponents();
        pack();
        setResizable(false);
        setSize(400, getHeight());
        setLocationRelativeTo(owner);

        workerService.addObserver(this);

        // Initial state
        updateDisplay();
    }

    private void cancel() {
        workerService.cancelAll();
        dispose();
    }

    private void tryCancelling() {
        if (workerService.shouldConfirmInterrupt()) {
            if (SwingHelper.confirmDialog(self,
                    _("progressDialog.cancelPrompt"),
                    _("progressDialog.cancelPromptTitle"))) {
                cancel();
            }
        } else {
            cancel();
        }
    }

    private void initComponents() {
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(1000);
        progressBar.setIndeterminate(true);
        buttonPanel.add(progressBar);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        cancelButton = new JButton(_("button.cancel"));
        buttonPanel.add(cancelButton);
        container.add(buttonPanel);

        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.LINE_AXIS));
        statusLabel = new JLabel(defaultStatus, SwingConstants.LEADING);
        statusPanel.add(statusLabel);
        statusPanel.add(Box.createHorizontalGlue());
        container.add(statusPanel);

        add(container);

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tryCancelling();
            }
        });
    }

    private synchronized void updateDisplay() {
        final String title = workerService.getLocalizedTitle();
        final String status = workerService.getLocalizedStatus();
        final double progress = workerService.getProgress();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setTitle(title != null ? title : defaultTitle);
                statusLabel.setText(status != null ? status : defaultStatus);
                if (progress == -1) {
                    progressBar.setIndeterminate(true);
                } else {
                    double value = progress *
                            (progressBar.getMaximum() - progressBar.getMinimum());
                    progressBar.setValue((int) value);
                    progressBar.setIndeterminate(false);
                }
            }
        });
    }

    @Override
    public synchronized void update(Observable o, Object arg) {
        updateDisplay();
    }
}
