package com.productivecows.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Shape check of the Cooler multiblock: a 3x3 tower, 3 to 10 blocks tall.
 * <ul>
 *   <li>Bottom and top layers: corners are Casing, edges Casing, Port or Controller, the centre Casing or Port.</li>
 *   <li>Inner layers: corners are Casing, edges Casing, Tank, Port or Controller, the centre a Stamp or air.</li>
 *   <li>One Controller, at least one Tank (at most {@link #MAX_TANKS}), one Port and one Stamp.</li>
 * </ul>
 * Pure logic on positions, independent of how the world is read, so it ports unchanged between Minecraft versions.
 */
public final class CoolerStructure {
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 10;
    public static final int MAX_TANKS = 8;
    /** Ingots a single Stamp cools at the same time. */
    public static final int INGOTS_PER_STAMP = 2;

    private static final Set<CoolerPart> CAP_CORNER = EnumSet.of(CoolerPart.CASING);
    private static final Set<CoolerPart> CAP_EDGE = EnumSet.of(CoolerPart.CASING, CoolerPart.PORT, CoolerPart.CONTROLLER);
    private static final Set<CoolerPart> CAP_CENTRE = EnumSet.of(CoolerPart.CASING, CoolerPart.PORT);
    private static final Set<CoolerPart> INNER_CORNER = EnumSet.of(CoolerPart.CASING);
    private static final Set<CoolerPart> INNER_EDGE = EnumSet.of(CoolerPart.CASING, CoolerPart.TANK, CoolerPart.PORT, CoolerPart.CONTROLLER);
    private static final Set<CoolerPart> INNER_CENTRE = EnumSet.of(CoolerPart.STAMP, CoolerPart.EMPTY);

    public enum Problem {
        /** The block behind the Controller is not the tower's centre column. */
        CONTROLLER_FACING,
        NO_BOTTOM,
        NO_TOP,
        TOO_TALL,
        WRONG_BLOCK,
        EXTRA_CONTROLLER,
        TOO_MANY_TANKS,
        NO_TANK,
        NO_PORT,
        NO_STAMP
    }

    /**
     * Outcome of a check. When formed, {@code parts} lists every block of the tower (air excluded) and
     * {@code ports} the Port positions; otherwise {@code problem} says what is wrong and, when it applies,
     * where ({@code problemPos}) and what would fit there ({@code expected}).
     */
    public record Result(@Nullable Problem problem, @Nullable BlockPos problemPos, Set<CoolerPart> expected, int count,
                         @Nullable BlockPos bottomCentre, int height, int tanks, int stamps,
                         List<BlockPos> parts, List<BlockPos> ports) {
        public boolean formed() {
            return problem == null;
        }

        static Result fail(Problem problem, @Nullable BlockPos pos, Set<CoolerPart> expected, int count) {
            return new Result(problem, pos, expected, count, null, 0, 0, 0, List.of(), List.of());
        }
    }

    private CoolerStructure() {}

    /**
     * Checks the tower around a Controller whose screen faces {@code facing} (outwards).
     * {@code partAt} reads the world; it is only called on the at most 3x3x10 positions of the tower.
     */
    public static Result check(BlockPos controller, Direction facing, Function<BlockPos, CoolerPart> partAt) {
        // The centre column is right behind the Controller's screen, and the opposite wall right behind the centre:
        // if that wall isn't there, the Controller is turned sideways (or the wall is missing)
        BlockPos centre = controller.relative(facing.getOpposite());
        CoolerPart here = partAt.apply(centre);
        CoolerPart farWall = partAt.apply(centre.relative(facing.getOpposite()));
        if (farWall == CoolerPart.EMPTY || farWall == CoolerPart.OTHER || !(here.isInterior() || here.isCap())) {
            return Result.fail(Problem.CONTROLLER_FACING, centre, EnumSet.noneOf(CoolerPart.class), 0);
        }

        int bottom;
        int top;
        if (here.isInterior()) {
            bottom = findCap(centre, -1, partAt);
            top = findCap(centre, 1, partAt);
        } else {
            // The Controller is on the bottom or the top layer. Air above a top layer also looks like an inner
            // layer, so try the Controller's layer as the bottom first and as the top if no top is found
            bottom = centre.getY();
            top = findCap(centre, 1, partAt);
            if (top == Integer.MIN_VALUE) {
                top = centre.getY();
                bottom = findCap(centre, -1, partAt);
            }
        }
        if (bottom == Integer.MIN_VALUE) {
            return Result.fail(Problem.NO_BOTTOM, null, CAP_CENTRE, MAX_HEIGHT);
        }
        if (top == Integer.MIN_VALUE) {
            return Result.fail(Problem.NO_TOP, null, CAP_CENTRE, MAX_HEIGHT);
        }
        int height = top - bottom + 1;
        if (height > MAX_HEIGHT) {
            return Result.fail(Problem.TOO_TALL, null, EnumSet.noneOf(CoolerPart.class), height);
        }

        List<BlockPos> parts = new ArrayList<>();
        List<BlockPos> ports = new ArrayList<>();
        int tanks = 0;
        int stamps = 0;
        for (int y = bottom; y <= top; y++) {
            boolean cap = y == bottom || y == top;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = new BlockPos(centre.getX() + dx, y, centre.getZ() + dz);
                    boolean corner = dx != 0 && dz != 0;
                    boolean middle = dx == 0 && dz == 0;
                    Set<CoolerPart> allowed = cap
                            ? (corner ? CAP_CORNER : middle ? CAP_CENTRE : CAP_EDGE)
                            : (corner ? INNER_CORNER : middle ? INNER_CENTRE : INNER_EDGE);
                    CoolerPart part = partAt.apply(pos);
                    if (!allowed.contains(part)) {
                        return Result.fail(Problem.WRONG_BLOCK, pos, allowed, 0);
                    }
                    switch (part) {
                        case CONTROLLER -> {
                            if (!pos.equals(controller)) {
                                return Result.fail(Problem.EXTRA_CONTROLLER, pos, EnumSet.noneOf(CoolerPart.class), 0);
                            }
                        }
                        case TANK -> tanks++;
                        case STAMP -> stamps++;
                        case PORT -> ports.add(pos);
                        default -> { }
                    }
                    if (part != CoolerPart.EMPTY) {
                        parts.add(pos);
                    }
                }
            }
        }
        if (tanks > MAX_TANKS) {
            return Result.fail(Problem.TOO_MANY_TANKS, null, EnumSet.noneOf(CoolerPart.class), tanks);
        }
        if (tanks == 0) {
            return Result.fail(Problem.NO_TANK, null, EnumSet.of(CoolerPart.TANK), 0);
        }
        if (ports.isEmpty()) {
            return Result.fail(Problem.NO_PORT, null, EnumSet.of(CoolerPart.PORT), 0);
        }
        if (stamps == 0) {
            return Result.fail(Problem.NO_STAMP, null, EnumSet.of(CoolerPart.STAMP), 0);
        }
        return new Result(null, null, EnumSet.noneOf(CoolerPart.class), 0,
                new BlockPos(centre.getX(), bottom, centre.getZ()), height, tanks, stamps, List.copyOf(parts), List.copyOf(ports));
    }

    /** Walks the centre column from {@code start} in {@code step} direction to the first cap; MIN_VALUE if none within reach. */
    private static int findCap(BlockPos start, int step, Function<BlockPos, CoolerPart> partAt) {
        BlockPos pos = start;
        for (int i = 0; i < MAX_HEIGHT; i++) {
            pos = pos.relative(step > 0 ? Direction.UP : Direction.DOWN);
            CoolerPart part = partAt.apply(pos);
            if (part.isCap()) {
                return pos.getY();
            }
            if (!part.isInterior()) {
                return Integer.MIN_VALUE;
            }
        }
        return Integer.MIN_VALUE;
    }
}
