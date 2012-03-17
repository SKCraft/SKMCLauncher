/*
 *  Copyright (c) 2009 Julien Ponge. All rights reserved.
 *
 *  <julien.ponge@gmail.com>
 *  http://julien.ponge.info/
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
 *
 *  This work is based on the LZMA SDK by Igor Pavlov.
 *  The LZMA SDK is placed under the public domain, and can be obtained from
 *
 *      http://www.7-zip.org/sdk.html
 *
 *  The LzmaInputStream and LzmaOutputStream classes were inspired by the
 *  work of Christopher League, although they are not derivative works.
 *
 *      http://contrapunctus.net/league/haques/lzmajio/
 */

package lzma.sdk.lz;

import java.io.IOException;

public class OutWindow {
    private byte[] _buffer;
    private int _pos;
    private int _windowSize = 0;
    private int _streamPos;
    private java.io.OutputStream _stream;

    public void create(int windowSize) {
        if (_buffer == null || _windowSize != windowSize) {
            _buffer = new byte[windowSize];
        }
        _windowSize = windowSize;
        _pos = 0;
        _streamPos = 0;
    }

    public void setStream(java.io.OutputStream stream) throws IOException {
        releaseStream();
        _stream = stream;
    }

    public void releaseStream() throws IOException {
        flush();
        _stream = null;
    }

    public void init(boolean solid) {
        if (!solid) {
            _streamPos = 0;
            _pos = 0;
        }
    }

    public void flush() throws IOException {
        int size = _pos - _streamPos;
        if (size == 0) {
            return;
        }
        _stream.write(_buffer, _streamPos, size);
        if (_pos >= _windowSize) {
            _pos = 0;
        }
        _streamPos = _pos;
    }

    public void copyBlock(int distance, int len) throws IOException {
        int pos = _pos - distance - 1;
        if (pos < 0) {
            pos += _windowSize;
        }
        for (; len != 0; len--) {
            if (pos >= _windowSize) {
                pos = 0;
            }
            _buffer[_pos++] = _buffer[pos++];
            if (_pos >= _windowSize) {
                flush();
            }
        }
    }

    public void putByte(byte b) throws IOException {
        _buffer[_pos++] = b;
        if (_pos >= _windowSize) {
            flush();
        }
    }

    public byte getByte(int distance) {
        int pos = _pos - distance - 1;
        if (pos < 0) {
            pos += _windowSize;
        }
        return _buffer[pos];
    }
}
