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

package lzma.sdk;

public class CRC {
    static public int[] Table = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int r = i;
            for (int j = 0; j < 8; j++) {
                if ((r & 1) != 0) {
                    r = (r >>> 1) ^ 0xEDB88320;
                } else {
                    r >>>= 1;
                }
            }
            Table[i] = r;
        }
    }

    int _value = -1;

    public void init() {
        _value = -1;
    }

    public void update(byte[] data, int offset, int size) {
        for (int i = 0; i < size; i++) {
            _value = Table[(_value ^ data[offset + i]) & 0xFF] ^ (_value >>> 8);
        }
    }

    public void update(byte[] data) {
        int size = data.length;
        for (int i = 0; i < size; i++) {
            _value = Table[(_value ^ data[i]) & 0xFF] ^ (_value >>> 8);
        }
    }

    public void updateByte(int b) {
        _value = Table[(_value ^ b) & 0xFF] ^ (_value >>> 8);
    }

    public int getDigest() {
        return ~_value;
    }
}
