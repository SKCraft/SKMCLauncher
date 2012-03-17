package lzma.sdk.rangecoder;

import java.io.IOException;

public class BitTreeEncoder {
    private final short[] Models;
    private final int NumBitLevels;

    public BitTreeEncoder(int numBitLevels) {
        NumBitLevels = numBitLevels;
        Models = new short[1 << numBitLevels];
    }

    public void init() {
        Decoder.initBitModels(Models);
    }

    public void encode(Encoder rangeEncoder, int symbol) throws IOException {
        int m = 1;
        for (int bitIndex = NumBitLevels; bitIndex != 0;) {
            bitIndex--;
            int bit = (symbol >>> bitIndex) & 1;
            rangeEncoder.encode(Models, m, bit);
            m = (m << 1) | bit;
        }
    }

    public void reverseEncode(Encoder rangeEncoder, int symbol)
            throws IOException {
        int m = 1;
        for (int i = 0; i < NumBitLevels; i++) {
            int bit = symbol & 1;
            rangeEncoder.encode(Models, m, bit);
            m = (m << 1) | bit;
            symbol >>= 1;
        }
    }

    public int getPrice(int symbol) {
        int price = 0;
        int m = 1;
        for (int bitIndex = NumBitLevels; bitIndex != 0;) {
            bitIndex--;
            int bit = (symbol >>> bitIndex) & 1;
            price += Encoder.getPrice(Models[m], bit);
            m = (m << 1) + bit;
        }
        return price;
    }

    public int reverseGetPrice(int symbol) {
        int price = 0;
        int m = 1;
        for (int i = NumBitLevels; i != 0; i--) {
            int bit = symbol & 1;
            symbol >>>= 1;
            price += Encoder.getPrice(Models[m], bit);
            m = (m << 1) | bit;
        }
        return price;
    }

    public static int reverseGetPrice(short[] Models, int startIndex,
            int NumBitLevels, int symbol) {
        int price = 0;
        int m = 1;
        for (int i = NumBitLevels; i != 0; i--) {
            int bit = symbol & 1;
            symbol >>>= 1;
            price += Encoder.getPrice(Models[startIndex + m], bit);
            m = (m << 1) | bit;
        }
        return price;
    }

    public static void reverseEncode(short[] Models, int startIndex,
            Encoder rangeEncoder, int NumBitLevels, int symbol)
            throws IOException {
        int m = 1;
        for (int i = 0; i < NumBitLevels; i++) {
            int bit = symbol & 1;
            rangeEncoder.encode(Models, startIndex + m, bit);
            m = (m << 1) | bit;
            symbol >>= 1;
        }
    }
}
