package org.minborg.instantspeed.zgc;

public final class MessageUtil {

    public static int encode(final CharSequence ticker) {
        return ticker.charAt(3) << 24 |
                ticker.charAt(2) << 16 |
                ticker.charAt(1) << 8 |
                ticker.charAt(0);
    }

    public static void decodeInto(int encodedValue, CharSequence decodedTickerOut){
        throw new UnsupportedOperationException();
    }

}