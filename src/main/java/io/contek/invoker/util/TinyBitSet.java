package io.contek.invoker.util;

public final class TinyBitSet {

    public static long set(long store, int idx) {
        store |= 1L<<idx;

        return store;
    }

    public static long clear(long store, int idx) {
        store &= ~(1L<<idx);

        return store;
    }

    public static boolean isSet(long store, int idx) {
        return (store & (1L << idx)) != 0;
    }
}
