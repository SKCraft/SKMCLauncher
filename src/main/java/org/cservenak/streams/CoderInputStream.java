/*
 *  Copyright (c) 2011 Tamas Cservenak. All rights reserved.
 *
 *  <tamas@cservenak.com>
 *  http://www.cservenak.com/
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.cservenak.streams;

import java.io.IOException;
import java.io.InputStream;

public class CoderInputStream extends InputStream {
    private final CoderThread ct;

    private volatile InputStream in;
    private volatile InputStream baseIn;

    protected CoderInputStream(final InputStream in, final Coder coder)
            throws IOException {
        this.baseIn = in;
        this.ct = new CoderThread(coder, in);

        this.in = ct.getInputStreamSink();

        this.ct.start();
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();

        try {
            baseIn.close();
        } catch (IOException e) {
        }

        try {
            ct.join();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }

        ct.checkForException();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }
}
