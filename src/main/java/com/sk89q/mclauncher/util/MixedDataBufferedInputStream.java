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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class MixedDataBufferedInputStream extends BufferedInputStream {
    
    private int lineBufferSize = 8192;

    public MixedDataBufferedInputStream(InputStream in) {
        super(in);
    }

    public MixedDataBufferedInputStream(InputStream in, int lineBufferSize) {
        super(in);
        this.lineBufferSize = lineBufferSize;
    }
    
    public String readLine() throws IOException {
        byte[] buf = new byte[lineBufferSize];
        int pos = 0;
        boolean foundCR = false;
        while (true) {
            int ch = read();
            if (ch == '\r') {
                buf[pos++] = (byte) ch;
                foundCR = true;
                in.mark(1);
                continue;
            }
            if (ch == '\n' || ch < 0) {
                buf[pos++] = (byte) ch;
                break;
            } else if (foundCR) {
                in.reset();
                break;
            }
            buf[pos++] = (byte) ch;
            if (pos == buf.length) {
                break;
            }
        }
        return new String(Arrays.copyOf(buf, pos), "UTF-8");
    }

}
