/*
 *  Copyright (c) 2010-2011 Julien Ponge. All rights reserved.
 *
 *  Portions Copyright (c) 2011 Tamas Cservenak.
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
import org.cservenak.streams.CoderOutputStream;

import java.io.IOException;
import java.io.OutputStream;

import static lzma.sdk.lzma.Encoder.EMatchFinderTypeBT2;
import static lzma.sdk.lzma.Encoder.EMatchFinderTypeBT4;

/**
 * An output stream that uses LZMA compression.
 * 
 * @author Julien Ponge
 * @author Tamas Cservenak
 */
public class LzmaOutputStream extends CoderOutputStream {
    public LzmaOutputStream(final OutputStream out,
            final LzmaEncoderWrapper wrapper) throws IOException {
        super(out, wrapper);
    }

    public LzmaOutputStream(final OutputStream out, final Encoder lzmaEncoder)
            throws IOException {
        this(out, new LzmaEncoderWrapper(lzmaEncoder));
    }

    /**
     * A convenient builder that makes it easier to configure the LZMA encoder.
     * Default values:
     * <ul>
     * <li>dictionnary size: max</li>
     * <li>end marker mode: true</li>
     * <li>match finder: BT4</li>
     * <li>number of fast bytes: 0x20</li>
     * </ul>
     */
    public static class Builder {
        private final OutputStream out;

        private int dictionnarySize = 1 << 23;

        private boolean endMarkerMode = true;

        private int matchFinder = EMatchFinderTypeBT4;

        private int numFastBytes = 0x20;

        public Builder(OutputStream out) {
            this.out = out;
        }

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

        public LzmaOutputStream build() throws IOException {
            Encoder encoder = new Encoder();

            encoder.setDictionarySize(dictionnarySize);
            encoder.setEndMarkerMode(endMarkerMode);
            encoder.setMatchFinder(matchFinder);
            encoder.setNumFastBytes(numFastBytes);

            return new LzmaOutputStream(out, encoder);
        }
    }
}
