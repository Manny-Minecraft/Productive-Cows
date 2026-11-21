package com.productivecows.menu;

/**
 * Menu data slots reach the client as 16-bit shorts, so values that can exceed 32767 (energy) are sent
 * as two slots, the low and the high half, and joined again by the menu.
 */
public final class SplitInt {
    private SplitInt() {}

    public static int low(int value) {
        return value & 0xFFFF;
    }

    public static int high(int value) {
        return value >>> 16;
    }

    public static int join(int low, int high) {
        return (low & 0xFFFF) | ((high & 0xFFFF) << 16);
    }
}
