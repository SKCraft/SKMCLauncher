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

package lzma.sdk.lzma;

import lzma.sdk.lz.OutWindow;
import lzma.sdk.rangecoder.BitTreeDecoder;

import java.io.IOException;

public class Decoder {
    class LenDecoder {
        final short[] m_Choice = new short[2];
        final BitTreeDecoder[] m_LowCoder = new BitTreeDecoder[Base.kNumPosStatesMax];
        final BitTreeDecoder[] m_MidCoder = new BitTreeDecoder[Base.kNumPosStatesMax];
        final BitTreeDecoder m_HighCoder = new BitTreeDecoder(
                Base.kNumHighLenBits);
        int m_NumPosStates = 0;

        public void create(int numPosStates) {
            for (; m_NumPosStates < numPosStates; m_NumPosStates++) {
                m_LowCoder[m_NumPosStates] = new BitTreeDecoder(
                        Base.kNumLowLenBits);
                m_MidCoder[m_NumPosStates] = new BitTreeDecoder(
                        Base.kNumMidLenBits);
            }
        }

        public void init() {
            lzma.sdk.rangecoder.Decoder.initBitModels(m_Choice);
            for (int posState = 0; posState < m_NumPosStates; posState++) {
                m_LowCoder[posState].init();
                m_MidCoder[posState].init();
            }
            m_HighCoder.init();
        }

        public int decode(lzma.sdk.rangecoder.Decoder rangeDecoder, int posState)
                throws IOException {
            if (rangeDecoder.decodeBit(m_Choice, 0) == 0) {
                return m_LowCoder[posState].decode(rangeDecoder);
            }
            int symbol = Base.kNumLowLenSymbols;
            if (rangeDecoder.decodeBit(m_Choice, 1) == 0) {
                symbol += m_MidCoder[posState].decode(rangeDecoder);
            } else {
                symbol += Base.kNumMidLenSymbols
                        + m_HighCoder.decode(rangeDecoder);
            }
            return symbol;
        }
    }

    class LiteralDecoder {
        class Decoder2 {
            final short[] m_Decoders = new short[0x300];

            public void init() {
                lzma.sdk.rangecoder.Decoder.initBitModels(m_Decoders);
            }

            public byte decodeNormal(lzma.sdk.rangecoder.Decoder rangeDecoder)
                    throws IOException {
                int symbol = 1;
                do {
                    symbol = (symbol << 1)
                            | rangeDecoder.decodeBit(m_Decoders, symbol);
                } while (symbol < 0x100);
                return (byte) symbol;
            }

            public byte decodeWithMatchByte(
                    lzma.sdk.rangecoder.Decoder rangeDecoder, byte matchByte)
                    throws IOException {
                int symbol = 1;
                do {
                    int matchBit = (matchByte >> 7) & 1;
                    matchByte <<= 1;
                    int bit = rangeDecoder.decodeBit(m_Decoders,
                            ((1 + matchBit) << 8) + symbol);
                    symbol = (symbol << 1) | bit;
                    if (matchBit != bit) {
                        while (symbol < 0x100) {
                            symbol = (symbol << 1)
                                    | rangeDecoder
                                            .decodeBit(m_Decoders, symbol);
                        }
                        break;
                    }
                } while (symbol < 0x100);
                return (byte) symbol;
            }
        }

        Decoder2[] m_Coders;
        int m_NumPrevBits;
        int m_NumPosBits;
        int m_PosMask;

        public void create(int numPosBits, int numPrevBits) {
            if (m_Coders != null && m_NumPrevBits == numPrevBits
                    && m_NumPosBits == numPosBits) {
                return;
            }
            m_NumPosBits = numPosBits;
            m_PosMask = (1 << numPosBits) - 1;
            m_NumPrevBits = numPrevBits;
            int numStates = 1 << (m_NumPrevBits + m_NumPosBits);
            m_Coders = new Decoder2[numStates];
            for (int i = 0; i < numStates; i++) {
                m_Coders[i] = new Decoder2();
            }
        }

        public void init() {
            int numStates = 1 << (m_NumPrevBits + m_NumPosBits);
            for (int i = 0; i < numStates; i++) {
                m_Coders[i].init();
            }
        }

        Decoder2 getDecoder(int pos, byte prevByte) {
            return m_Coders[((pos & m_PosMask) << m_NumPrevBits)
                    + ((prevByte & 0xFF) >>> (8 - m_NumPrevBits))];
        }
    }

    private final OutWindow m_OutWindow = new OutWindow();
    private final lzma.sdk.rangecoder.Decoder m_RangeDecoder = new lzma.sdk.rangecoder.Decoder();

    private final short[] m_IsMatchDecoders = new short[Base.kNumStates << Base.kNumPosStatesBitsMax];
    private final short[] m_IsRepDecoders = new short[Base.kNumStates];
    private final short[] m_IsRepG0Decoders = new short[Base.kNumStates];
    private final short[] m_IsRepG1Decoders = new short[Base.kNumStates];
    private final short[] m_IsRepG2Decoders = new short[Base.kNumStates];
    private final short[] m_IsRep0LongDecoders = new short[Base.kNumStates << Base.kNumPosStatesBitsMax];

    private final BitTreeDecoder[] m_PosSlotDecoder = new BitTreeDecoder[Base.kNumLenToPosStates];
    private final short[] m_PosDecoders = new short[Base.kNumFullDistances
            - Base.kEndPosModelIndex];

    private final BitTreeDecoder m_PosAlignDecoder = new BitTreeDecoder(
            Base.kNumAlignBits);

    private final LenDecoder m_LenDecoder = new LenDecoder();
    private final LenDecoder m_RepLenDecoder = new LenDecoder();

    private final LiteralDecoder m_LiteralDecoder = new LiteralDecoder();

    private int m_DictionarySize = -1;
    private int m_DictionarySizeCheck = -1;

    private int m_PosStateMask;

    public Decoder() {
        for (int i = 0; i < Base.kNumLenToPosStates; i++) {
            m_PosSlotDecoder[i] = new BitTreeDecoder(Base.kNumPosSlotBits);
        }
    }

    boolean setDictionarySize(int dictionarySize) {
        if (dictionarySize < 0) {
            return false;
        }
        if (m_DictionarySize != dictionarySize) {
            m_DictionarySize = dictionarySize;
            m_DictionarySizeCheck = Math.max(m_DictionarySize, 1);
            m_OutWindow.create(Math.max(m_DictionarySizeCheck, (1 << 12)));
        }
        return true;
    }

    boolean setLcLpPb(int lc, int lp, int pb) {
        if (lc > Base.kNumLitContextBitsMax || lp > 4
                || pb > Base.kNumPosStatesBitsMax) {
            return false;
        }
        m_LiteralDecoder.create(lp, lc);
        int numPosStates = 1 << pb;
        m_LenDecoder.create(numPosStates);
        m_RepLenDecoder.create(numPosStates);
        m_PosStateMask = numPosStates - 1;
        return true;
    }

    void init() throws IOException {
        m_OutWindow.init(false);

        lzma.sdk.rangecoder.Decoder.initBitModels(m_IsMatchDecoders);
        lzma.sdk.rangecoder.Decoder.initBitModels(m_IsRep0LongDecoders);
        lzma.sdk.rangecoder.Decoder.initBitModels(m_IsRepDecoders);
        lzma.sdk.rangecoder.Decoder.initBitModels(m_IsRepG0Decoders);
        lzma.sdk.rangecoder.Decoder.initBitModels(m_IsRepG1Decoders);
        lzma.sdk.rangecoder.Decoder.initBitModels(m_IsRepG2Decoders);
        lzma.sdk.rangecoder.Decoder.initBitModels(m_PosDecoders);

        m_LiteralDecoder.init();
        int i;
        for (i = 0; i < Base.kNumLenToPosStates; i++) {
            m_PosSlotDecoder[i].init();
        }
        m_LenDecoder.init();
        m_RepLenDecoder.init();
        m_PosAlignDecoder.init();
        m_RangeDecoder.init();
    }

    public boolean code(java.io.InputStream inStream,
            java.io.OutputStream outStream, long outSize) throws IOException {
        m_RangeDecoder.setStream(inStream);
        m_OutWindow.setStream(outStream);
        init();

        int state = Base.stateInit();
        int rep0 = 0, rep1 = 0, rep2 = 0, rep3 = 0;

        long nowPos64 = 0;
        byte prevByte = 0;
        while (outSize < 0 || nowPos64 < outSize) {
            int posState = (int) nowPos64 & m_PosStateMask;
            if (m_RangeDecoder.decodeBit(m_IsMatchDecoders,
                    (state << Base.kNumPosStatesBitsMax) + posState) == 0) {
                LiteralDecoder.Decoder2 decoder2 = m_LiteralDecoder.getDecoder(
                        (int) nowPos64, prevByte);
                if (!Base.stateIsCharState(state)) {
                    prevByte = decoder2.decodeWithMatchByte(m_RangeDecoder,
                            m_OutWindow.getByte(rep0));
                } else {
                    prevByte = decoder2.decodeNormal(m_RangeDecoder);
                }
                m_OutWindow.putByte(prevByte);
                state = Base.stateUpdateChar(state);
                nowPos64++;
            } else {
                int len;
                if (m_RangeDecoder.decodeBit(m_IsRepDecoders, state) == 1) {
                    len = 0;
                    if (m_RangeDecoder.decodeBit(m_IsRepG0Decoders, state) == 0) {
                        if (m_RangeDecoder
                                .decodeBit(m_IsRep0LongDecoders,
                                        (state << Base.kNumPosStatesBitsMax)
                                                + posState) == 0) {
                            state = Base.stateUpdateShortRep(state);
                            len = 1;
                        }
                    } else {
                        int distance;
                        if (m_RangeDecoder.decodeBit(m_IsRepG1Decoders, state) == 0) {
                            distance = rep1;
                        } else {
                            if (m_RangeDecoder.decodeBit(m_IsRepG2Decoders,
                                    state) == 0) {
                                distance = rep2;
                            } else {
                                distance = rep3;
                                rep3 = rep2;
                            }
                            rep2 = rep1;
                        }
                        rep1 = rep0;
                        rep0 = distance;
                    }
                    if (len == 0) {
                        len = m_RepLenDecoder.decode(m_RangeDecoder, posState)
                                + Base.kMatchMinLen;
                        state = Base.stateUpdateRep(state);
                    }
                } else {
                    rep3 = rep2;
                    rep2 = rep1;
                    rep1 = rep0;
                    len = Base.kMatchMinLen
                            + m_LenDecoder.decode(m_RangeDecoder, posState);
                    state = Base.stateUpdateMatch(state);
                    int posSlot = m_PosSlotDecoder[Base.getLenToPosState(len)]
                            .decode(m_RangeDecoder);
                    if (posSlot >= Base.kStartPosModelIndex) {
                        int numDirectBits = (posSlot >> 1) - 1;
                        rep0 = ((2 | (posSlot & 1)) << numDirectBits);
                        if (posSlot < Base.kEndPosModelIndex) {
                            rep0 += BitTreeDecoder.reverseDecode(m_PosDecoders,
                                    rep0 - posSlot - 1, m_RangeDecoder,
                                    numDirectBits);
                        } else {
                            rep0 += (m_RangeDecoder
                                    .decodeDirectBits(numDirectBits
                                            - Base.kNumAlignBits) << Base.kNumAlignBits);
                            rep0 += m_PosAlignDecoder
                                    .reverseDecode(m_RangeDecoder);
                            if (rep0 < 0) {
                                if (rep0 == -1) {
                                    break;
                                }
                                return false;
                            }
                        }
                    } else {
                        rep0 = posSlot;
                    }
                }
                if (rep0 >= nowPos64 || rep0 >= m_DictionarySizeCheck) {
                    // m_OutWindow.flush();
                    return false;
                }
                m_OutWindow.copyBlock(rep0, len);
                nowPos64 += len;
                prevByte = m_OutWindow.getByte(0);
            }
        }
        m_OutWindow.flush();
        m_OutWindow.releaseStream();
        m_RangeDecoder.releaseStream();
        return true;
    }

    public boolean setDecoderProperties(byte[] properties) {
        if (properties.length < 5) {
            return false;
        }
        int val = properties[0] & 0xFF;
        int lc = val % 9;
        int remainder = val / 9;
        int lp = remainder % 5;
        int pb = remainder / 5;
        int dictionarySize = 0;
        for (int i = 0; i < 4; i++) {
            dictionarySize += ((int) (properties[1 + i]) & 0xFF) << (i * 8);
        }
        return setLcLpPb(lc, lp, pb) && setDictionarySize(dictionarySize);
    }
}
