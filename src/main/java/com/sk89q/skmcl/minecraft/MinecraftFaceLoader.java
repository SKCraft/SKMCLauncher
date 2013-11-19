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

package com.sk89q.skmcl.minecraft;

import com.sk89q.skmcl.util.HttpRequest;
import lombok.NonNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;

import static com.sk89q.skmcl.util.LauncherUtils.checkInterrupted;

public class MinecraftFaceLoader implements Callable<BufferedImage> {

    private static final String SKINS_URL =
            "http://skins.minecraft.net/MinecraftSkins/%s.png";

    private final String username;
    private final int size;

    public MinecraftFaceLoader(String username, int size) {
        this.username = username;
        this.size = size;
    }

    @Override
    public BufferedImage call() throws Exception {
        return createFaceImage(username, size);
    }

    private static BufferedImage readSkin(String username) throws IOException {
        URL url = HttpRequest.url(String.format(SKINS_URL, username));
        HttpRequest request = HttpRequest.get(url);

        try {
            request.execute();
            return ImageIO.read(request.getInputStream());
        } finally {
            try {
                request.close();
            } catch (IOException e) {
            }
        }
    }

    private static BufferedImage createFaceImage(@NonNull String username, int size)
            throws IOException, InterruptedException {
        BufferedImage skin = readSkin(username);
        checkInterrupted();

        BufferedImage out = new BufferedImage(size, size,
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = out.createGraphics();

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);

        int headInset = (int) (0.05 * size);
        g.drawImage(skin,
                headInset, headInset, size - headInset, size - headInset,
                8, 8, 16, 16,
                null);
        g.drawImage(skin, 0, 0, size, size, 40, 8, 48, 16, null);

        return out;
    }

}
