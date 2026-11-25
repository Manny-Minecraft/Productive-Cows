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
 * Shape check of the Cooler multiblock: a 3x3 tower, 3 blocks to {@code coolerMaxHeight} tall (10 by default).
 * <ul>
 *   <li>Bottom and top layers: corners are Casing, edges Casing, Port or Controller, the centre Casing or Port.</li>
 *   <li>Inner layers: corners are Casing, edges Casing, Tank, Port or Controller, the centre a Stamp or air.</li>
 *   <li>One Controller, at least one Tank (at most {@link #MAX_TANKS}), one Port and one Stamp.</li>
 * </ul>
 * Pure logic on positions, independent of how the world is read, so it ports unchanged between Minecraft versions.
 */
public final class CoolerStructure {
    public static final int MIN_HEIGHT = 3;
    public static final int DEFAULT_MAX_HEIGHT = 10;
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
     * {@code partAt} reads the world; it is only called on the at most 3x3xmaxHeight positions of the tower.
     */
    public static Result check(BlockPos controller, Direction facing, int maxHeight, Function<BlockPos, CoolerPart> partAt) {
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
            bottom = findCap(centre, -1, partAt, maxHeight);
            top = findCap(centre, 1, partAt, maxHeight);
        } else {
            // The Controller is on the bottom or the top layer. Air above a top layer also looks like an inner
            // layer, so try the Controller's layer as the bottom first and as the top if no top is found
            bottom = centre.getY();
            top = findCap(centre, 1, partAt, maxHeight);
            if (top == Integer.MIN_VALUE) {
                top = centre.getY();
                bottom = findCap(centre, -1, partAt, maxHeight);
            }
        }
        if (bottom == Integer.MIN_VALUE) {
            return Result.fail(Problem.NO_BOTTOM, null, CAP_CENTRE, maxHeight);
        }
        if (top == Integer.MIN_VALUE) {
            return Result.fail(Problem.NO_TOP, null, CAP_CENTRE, maxHeight);
        }
        int height = top - bottom + 1;
        if (height > maxHeight) {
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
                    Set<CoolerPart> allowed = allowed(cap, dx, dz);
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

    /** What may stand at an offset from the centre column, on a bottom/top layer ({@code cap}) or an inner one. */
    public static Set<CoolerPart> allowed(boolean cap, int dx, int dz) {
        boolean corner = dx != 0 && dz != 0;
        boolean middle = dx == 0 && dz == 0;
        return cap
                ? (corner ? CAP_CORNER : middle ? CAP_CENTRE : CAP_EDGE)
                : (corner ? INNER_CORNER : middle ? INNER_CENTRE : INNER_EDGE);
    }

    /** Bottom layer and height of a tower, for the build preview. */
    public record Frame(int bottom, int height) {}

    /**
     * Guesses where the tower around a Controller stands, from what is already built: the caps found in the centre
     * column if any, otherwise the smallest tower with the Controller on its middle layer.
     */
    public static Frame frame(BlockPos controller, Direction facing, int maxHeight, Function<BlockPos, CoolerPart> partAt) {
        BlockPos centre = controller.relative(facing.getOpposite());
        CoolerPart here = partAt.apply(centre);
        int y = controller.getY();
        if (here.isCap()) {
            int top = findCap(centre, 1, partAt, maxHeight);
            if (top != Integer.MIN_VALUE) {
                return new Frame(y, top - y + 1);
            }
            int bottom = findCap(centre, -1, partAt, maxHeight);
            if (bottom != Integer.MIN_VALUE) {
                return new Frame(bottom, y - bottom + 1);
            }
            return new Frame(y, MIN_HEIGHT);
        }
        int bottom = findCap(centre, -1, partAt, maxHeight);
        int top = findCap(centre, 1, partAt, maxHeight);
        if (bottom != Integer.MIN_VALUE && top != Integer.MIN_VALUE && top - bottom + 1 <= maxHeight) {
            return new Frame(bottom, top - bottom + 1);
        }
        if (bottom != Integer.MIN_VALUE) {
            return new Frame(bottom, Math.min(maxHeight, Math.max(MIN_HEIGHT, y - bottom + 2)));
        }
        if (top != Integer.MIN_VALUE) {
            int height = Math.min(maxHeight, Math.max(MIN_HEIGHT, top - y + 2));
            return new Frame(top - height + 1, height);
        }
        return new Frame(y - 1, MIN_HEIGHT);
    }

    /**
     * A complete, valid tower for a frame: Casing everywhere, the Controller where it is, Stamps in the centre of the
     * inner layers, one Tank on the inner wall opposite the Controller and one Port in line with the Controller on
     * the bottom layer (the top one if the Controller is on the bottom).
     */
    public static java.util.Map<BlockPos, CoolerPart> blueprint(BlockPos controller, Direction facing, Frame frame) {
        BlockPos centre = controller.relative(facing.getOpposite());
        int bottom = frame.bottom();
        int top = bottom + frame.height() - 1;
        BlockPos tank = new BlockPos(centre.getX(), bottom + 1, centre.getZ()).relative(facing.getOpposite());
        int portY = controller.getY() == bottom ? top : bottom;
        BlockPos port = new BlockPos(controller.getX(), portY, controller.getZ());
        java.util.Map<BlockPos, CoolerPart> plan = new java.util.LinkedHashMap<>();
        for (int y = bottom; y <= top; y++) {
            boolean cap = y == bottom || y == top;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = new BlockPos(centre.getX() + dx, y, centre.getZ() + dz);
                    CoolerPart part;
                    if (pos.equals(controller)) {
                        part = CoolerPart.CONTROLLER;
                    } else if (!cap && dx == 0 && dz == 0) {
                        part = CoolerPart.STAMP;
                    } else if (pos.equals(tank)) {
                        part = CoolerPart.TANK;
                    } else if (pos.equals(port)) {
                        part = CoolerPart.PORT;
                    } else {
                        part = CoolerPart.CASING;
                    }
                    plan.put(pos, part);
                }
            }
        }
        return plan;
    }

    /** Walks the centre column from {@code start} in {@code step} direction to the first cap; MIN_VALUE if none within reach. */
    private static int findCap(BlockPos start, int step, Function<BlockPos, CoolerPart> partAt, int maxHeight) {
        BlockPos pos = start;
        for (int i = 0; i < maxHeight; i++) {
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
