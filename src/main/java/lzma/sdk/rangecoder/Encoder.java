package lzma.sdk.rangecoder;

import java.io.IOException;

public class Encoder {
    private static final int kTopMask = ~((1 << 24) - 1);

    private static final int kNumBitModelTotalBits = 11;
    private static final int kBitModelTotal = (1 << kNumBitModelTotalBits);
    private static final int kNumMoveBits = 5;

    private java.io.OutputStream Stream;

    private long Low;
    private int Range;
    private int _cacheSize;
    private int _cache;

    private long _position;

    public void setStream(java.io.OutputStream stream) {
        Stream = stream;
    }

    public void releaseStream() {
        Stream = null;
    }

    public void init() {
        _position = 0;
        Low = 0;
        Range = -1;
        _cacheSize = 1;
        _cache = 0;
    }

    public void flushData() throws IOException {
        for (int i = 0; i < 5; i++) {
            shiftLow();
        }
    }

    public void flushStream() throws IOException {
        Stream.flush();
    }

    void shiftLow() throws IOException {
        int LowHi = (int) (Low >>> 32);
        if (LowHi != 0 || Low < 0xFF000000L) {
            _position += _cacheSize;
            int temp = _cache;
            do {
                Stream.write(temp + LowHi);
                temp = 0xFF;
            } while (--_cacheSize != 0);
            _cache = (((int) Low) >>> 24);
        }
        _cacheSize++;
        Low = (Low & 0xFFFFFF) << 8;
    }

    public void encodeDirectBits(int v, int numTotalBits) throws IOException {
        for (int i = numTotalBits - 1; i >= 0; i--) {
            Range >>>= 1;
            if (((v >>> i) & 1) == 1) {
                Low += Range;
            }
            if ((Range & Encoder.kTopMask) == 0) {
                Range <<= 8;
                shiftLow();
            }
        }
    }

    public long getProcessedSizeAdd() {
        return _cacheSize + _position + 4;
    }

    private static final int kNumMoveReducingBits = 2;
    public static final int kNumBitPriceShiftBits = 6;

    public static void initBitModels(short[] probs) {
        for (int i = 0; i < probs.length; i++) {
            probs[i] = (kBitModelTotal >>> 1);
        }
    }

    public void encode(short[] probs, int index, int symbol) throws IOException {
        int prob = probs[index];
        int newBound = (Range >>> kNumBitModelTotalBits) * prob;
        if (symbol == 0) {
            Range = newBound;
            probs[index] = (short) (prob + ((kBitModelTotal - prob) >>> kNumMoveBits));
        } else {
            Low += (newBound & 0xFFFFFFFFL);
            Range -= newBound;
            probs[index] = (short) (prob - ((prob) >>> kNumMoveBits));
        }
        if ((Range & kTopMask) == 0) {
            Range <<= 8;
            shiftLow();
        }
    }

    private static final int[] ProbPrices = new int[kBitModelTotal >>> kNumMoveReducingBits];

    static {
        int kNumBits = (kNumBitModelTotalBits - kNumMoveReducingBits);
        for (int i = kNumBits - 1; i >= 0; i--) {
            int start = 1 << (kNumBits - i - 1);
            int end = 1 << (kNumBits - i);
            for (int j = start; j < end; j++) {
                ProbPrices[j] = (i << kNumBitPriceShiftBits)
                        + (((end - j) << kNumBitPriceShiftBits) >>> (kNumBits
                                - i - 1));
            }
        }
    }

    static public int getPrice(int Prob, int symbol) {
        return ProbPrices[(((Prob - symbol) ^ ((-symbol))) & (kBitModelTotal - 1)) >>> kNumMoveReducingBits];
    }

    static public int getPrice0(int Prob) {
        return ProbPrices[Prob >>> kNumMoveReducingBits];
    }

    static public int getPrice1(int Prob) {
        return ProbPrices[(kBitModelTotal - Prob) >>> kNumMoveReducingBits];
    }
}
