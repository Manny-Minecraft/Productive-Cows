package com.productivecows.multiblock;

/** What a position of the Cooler tower holds, as far as the structure check is concerned. */
public enum CoolerPart {
    CASING,
    TANK,
    PORT,
    CONTROLLER,
    STAMP,
    /** Air: allowed only in the centre column of the inner layers. */
    EMPTY,
    /** Any other block. */
    OTHER;

    /** Blocks that close the bottom and the top of the tower in their centre. */
    boolean isCap() {
        return this == CASING || this == PORT;
    }

    /** What the centre column of an inner layer may hold. */
    boolean isInterior() {
        return this == STAMP || this == EMPTY;
    }
}
