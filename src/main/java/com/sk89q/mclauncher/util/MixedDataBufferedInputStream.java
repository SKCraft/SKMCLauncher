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
