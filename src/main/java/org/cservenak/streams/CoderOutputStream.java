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
import java.io.OutputStream;

public class CoderOutputStream extends OutputStream {
    private final CoderThread ct;

    private OutputStream out;

    protected CoderOutputStream(final OutputStream out, final Coder coder)
            throws IOException {
        this.ct = new CoderThread(coder, out);

        this.out = ct.getOutputStreamSink();

        this.ct.start();
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        if ((off | len | (b.length - (len + off)) | (off + len)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            flush();
        } catch (IOException ignored) {
            // why do we swallow exception here?!
        }

        out.close();

        try {
            ct.join();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        ct.checkForException();
    }
}
