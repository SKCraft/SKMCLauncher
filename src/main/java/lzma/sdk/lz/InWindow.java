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

public class InWindow {
    byte[] _bufferBase; // pointer to buffer with data
    private java.io.InputStream _stream;
    private int _posLimit; // offset (from _buffer) of first byte when new block
                           // reading must be done
    private boolean _streamEndWasReached; // if (true) then _streamPos shows
                                          // real end of stream

    private int _pointerToLastSafePosition;

    int _bufferOffset;

    private int _blockSize; // Size of Allocated memory block
    int _pos; // offset (from _buffer) of curent byte
    private int _keepSizeBefore; // how many BYTEs must be kept in buffer before
                                 // _pos
    private int _keepSizeAfter; // how many BYTEs must be kept buffer after _pos
    int _streamPos; // offset (from _buffer) of first not read byte from Stream

    void moveBlock() {
        int offset = _bufferOffset + _pos - _keepSizeBefore;
        // we need one additional byte, since movePos moves on 1 byte.
        if (offset > 0) {
            offset--;
        }

        int numBytes = _bufferOffset + _streamPos - offset;

        // check negative offset ????
        System.arraycopy(_bufferBase, offset, _bufferBase, 0, numBytes);
        _bufferOffset -= offset;
    }

    void readBlock() throws IOException {
        if (_streamEndWasReached) {
            return;
        }
        while (true) {
            int size = (0 - _bufferOffset) + _blockSize - _streamPos;
            if (size == 0) {
                return;
            }
            int numReadBytes = _stream.read(_bufferBase, _bufferOffset
                    + _streamPos, size);
            if (numReadBytes == -1) {
                _posLimit = _streamPos;
                int pointerToPostion = _bufferOffset + _posLimit;
                if (pointerToPostion > _pointerToLastSafePosition) {
                    _posLimit = _pointerToLastSafePosition - _bufferOffset;
                }

                _streamEndWasReached = true;
                return;
            }
            _streamPos += numReadBytes;
            if (_streamPos >= _pos + _keepSizeAfter) {
                _posLimit = _streamPos - _keepSizeAfter;
            }
        }
    }

    void free() {
        _bufferBase = null;
    }

    void create(int keepSizeBefore, int keepSizeAfter, int keepSizeReserv) {
        _keepSizeBefore = keepSizeBefore;
        _keepSizeAfter = keepSizeAfter;
        int blockSize = keepSizeBefore + keepSizeAfter + keepSizeReserv;
        if (_bufferBase == null || _blockSize != blockSize) {
            free();
            _blockSize = blockSize;
            _bufferBase = new byte[_blockSize];
        }
        _pointerToLastSafePosition = _blockSize - keepSizeAfter;
    }

    public void setStream(java.io.InputStream stream) {
        _stream = stream;
    }

    public void releaseStream() {
        _stream = null;
    }

    void init() throws IOException {
        _bufferOffset = 0;
        _pos = 0;
        _streamPos = 0;
        _streamEndWasReached = false;
        readBlock();
    }

    void movePos() throws IOException {
        _pos++;
        if (_pos > _posLimit) {
            int pointerToPostion = _bufferOffset + _pos;
            if (pointerToPostion > _pointerToLastSafePosition) {
                moveBlock();
            }
            readBlock();
        }
    }

    public byte getIndexByte(int index) {
        return _bufferBase[_bufferOffset + _pos + index];
    }

    // index + limit have not to exceed _keepSizeAfter;
    public int getMatchLen(int index, int distance, int limit) {
        if (_streamEndWasReached) {
            if ((_pos + index) + limit > _streamPos) {
                limit = _streamPos - (_pos + index);
            }
        }
        distance++;
        // Byte *pby = _buffer + (size_t)_pos + index;
        int pby = _bufferOffset + _pos + index;

        int i;
        for (i = 0; i < limit
                && _bufferBase[pby + i] == _bufferBase[pby + i - distance]; i++) {
        }
        return i;
    }

    public int getNumAvailableBytes() {
        return _streamPos - _pos;
    }

    void reduceOffsets(int subValue) {
        _bufferOffset += subValue;
        _posLimit -= subValue;
        _pos -= subValue;
        _streamPos -= subValue;
    }
}
