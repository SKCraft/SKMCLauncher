package lzma.sdk.rangecoder;

public class BitTreeDecoder {
    private final short[] Models;
    private final int NumBitLevels;

    public BitTreeDecoder(int numBitLevels) {
        NumBitLevels = numBitLevels;
        Models = new short[1 << numBitLevels];
    }

    public void init() {
        Decoder.initBitModels(Models);
    }

    public int decode(Decoder rangeDecoder) throws java.io.IOException {
        int m = 1;
        for (int bitIndex = NumBitLevels; bitIndex != 0; bitIndex--) {
            m = (m << 1) + rangeDecoder.decodeBit(Models, m);
        }
        return m - (1 << NumBitLevels);
    }

    public int reverseDecode(Decoder rangeDecoder) throws java.io.IOException {
        int m = 1;
        int symbol = 0;
        for (int bitIndex = 0; bitIndex < NumBitLevels; bitIndex++) {
            int bit = rangeDecoder.decodeBit(Models, m);
            m <<= 1;
            m += bit;
            symbol |= (bit << bitIndex);
        }
        return symbol;
    }

    public static int reverseDecode(short[] Models, int startIndex,
            Decoder rangeDecoder, int NumBitLevels) throws java.io.IOException {
        int m = 1;
        int symbol = 0;
        for (int bitIndex = 0; bitIndex < NumBitLevels; bitIndex++) {
            int bit = rangeDecoder.decodeBit(Models, startIndex + m);
            m <<= 1;
            m += bit;
            symbol |= (bit << bitIndex);
        }
        return symbol;
    }
}
