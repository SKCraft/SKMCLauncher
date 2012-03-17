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

package lzma.streams;

import lzma.sdk.lzma.Encoder;
import org.cservenak.streams.Coder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static lzma.sdk.lzma.Encoder.EMatchFinderTypeBT2;
import static lzma.sdk.lzma.Encoder.EMatchFinderTypeBT4;

public class LzmaEncoderWrapper implements Coder {
    private final static byte[] MINUS_ONE = new byte[8];
    static {
        for (int i = 0; i < MINUS_ONE.length; ++i) {
            MINUS_ONE[i] = (byte) -1;
        }
    }

    private final Encoder encoder;

    public LzmaEncoderWrapper(final Encoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public void code(final InputStream in, final OutputStream out)
            throws IOException {
        encoder.writeCoderProperties(out);
        // write -1 as "unknown" for file size
        out.write(MINUS_ONE);
        encoder.code(in, out, -1, -1, null);
    }

    /**
     * A convenient builder that makes it easier to configure the LZMA encoder.
     * Default values:
     * <ul>
     * <li>dictionary size: 23 (almost max, so is memory hungry)</li>
     * <li>end marker mode: true</li>
     * <li>match finder: BT4</li>
     * <li>number of fast bytes: 0x20</li>
     * </ul>
     */
    public static class Builder {
        private int dictionnarySize = 1 << 23;

        private boolean endMarkerMode = true;

        private int matchFinder = EMatchFinderTypeBT4;

        private int numFastBytes = 0x20;

        public Builder useMaximalDictionarySize() {
            dictionnarySize = 1 << 28;
            return this;
        }

        public Builder useMediumDictionarySize() {
            dictionnarySize = 1 << 15;
            return this;
        }

        public Builder useMinimalDictionarySize() {
            dictionnarySize = 1;
            return this;
        }

        public Builder useEndMarkerMode(boolean endMarkerMode) {
            this.endMarkerMode = endMarkerMode;
            return this;
        }

        public Builder useBT4MatchFinder() {
            matchFinder = EMatchFinderTypeBT4;
            return this;
        }

        public Builder useBT2MatchFinder() {
            matchFinder = EMatchFinderTypeBT2;
            return this;
        }

        public Builder useMinimalFastBytes() {
            numFastBytes = 5;
            return this;
        }

        public Builder useMediumFastBytes() {
            numFastBytes = 0x20;
            return this;
        }

        public Builder useMaximalFastBytes() {
            numFastBytes = 273;
            return this;
        }

        public LzmaEncoderWrapper build() {
            Encoder encoder = new Encoder();

            encoder.setDictionarySize(dictionnarySize);
            encoder.setEndMarkerMode(endMarkerMode);
            encoder.setMatchFinder(matchFinder);
            encoder.setNumFastBytes(numFastBytes);

            return new LzmaEncoderWrapper(encoder);
        }
    }
}
