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

import com.sk89q.skmcl.minecraft.MinecraftFaceLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.sk89q.skmcl.util.LauncherUtils.checkInterrupted;

public class MinecraftFacePanel extends JButton {

    private static final int PANEL_SIZE = 46;
    private static final int FACE_SIZE = 40;
    private static final int FACE_PADDING = 3;
    private static final BufferedImage BACKGROUND;
    private static final BufferedImage GENERIC_AVATAR;

    static {
        BACKGROUND = SwingHelper.readIconImage("/resources/face_bg.png");
        GENERIC_AVATAR = SwingHelper.readIconImage("/resources/face_generic.png");
    }

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Future<BufferedImage> future;
    private String lastUsername;
    private BufferedImage face;

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(BACKGROUND, 0, 0, null);
        g.drawImage(getDrawnFace(), FACE_PADDING, FACE_PADDING, null);
    }

    private BufferedImage getDrawnFace() {
        BufferedImage face = this.face;
        return face != null ? face : GENERIC_AVATAR;
    }

    public synchronized void setUsername(String username) {
        if (future != null) {
            future.cancel(true);
        }

        // If there wasn't a change, then don't change the face
        if (lastUsername != null && username != null &&
                lastUsername.equalsIgnoreCase(username)) {
            return;
        }

        lastUsername = username;
        setToolTipText(username);
        setFace(null);
        if (username != null) {
            future = executor.submit(new FaceLoader(username));
        }
    }

    private void setFace(BufferedImage image) {
        face = image;

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                repaint();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PANEL_SIZE, PANEL_SIZE);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    private class FaceLoader extends MinecraftFaceLoader {

        private FaceLoader(String username) {
            super(username, FACE_SIZE);
        }

        public BufferedImage call() throws Exception {
            BufferedImage image = super.call();
            checkInterrupted();
            setFace(image);
            invalidate();
            return image;
        }

    }

}
