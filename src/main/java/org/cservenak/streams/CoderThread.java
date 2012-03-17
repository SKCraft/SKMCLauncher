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

import java.io.*;

public class CoderThread extends Thread {
    private final PipedInputStream inSink;

    private final PipedOutputStream outSink;

    private final Runnable workhorse;

    private Throwable throwable;

    public CoderThread(final Coder coder, final InputStream in)
            throws IOException {
        this.inSink = new PipedInputStream();
        this.outSink = new PipedOutputStream(inSink);
        this.workhorse = new Runnable() {
            @Override
            public void run() {
                try {
                    coder.code(in, outSink);

                    flush(outSink);
                } catch (Throwable e) {
                    throwable = e;
                } finally {
                    close(outSink);
                }
            }
        };
    }

    public CoderThread(final Coder coder, final OutputStream out)
            throws IOException {
        this.outSink = new PipedOutputStream();
        this.inSink = new PipedInputStream(outSink);
        this.workhorse = new Runnable() {
            @Override
            public void run() {
                try {
                    coder.code(inSink, out);

                    flush(out);
                } catch (Throwable e) {
                    throwable = e;
                } finally {
                    close(inSink);
                }
            }
        };
    }

    @Override
    public void run() {
        workhorse.run();
    }

    // ==

    public Throwable getThrowable() {
        return throwable;
    }

    public void checkForException() throws IOException {
        if (null != throwable) {
            if (throwable instanceof IOException) {
                throw (IOException) throwable;
            } else {
                throw new IOException(throwable);
            }
        }
    }

    public PipedInputStream getInputStreamSink() {
        return inSink;
    }

    public PipedOutputStream getOutputStreamSink() {
        return outSink;
    }

    // ==

    protected boolean flush(Flushable flushable) {
        if (flushable != null) {
            try {
                flushable.flush();

                return true;
            } catch (IOException e) {
                // mute
            }
        }

        return false;
    }

    protected boolean close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();

                return true;
            } catch (IOException e) {
                // mute
            }
        }

        return false;
    }
}
