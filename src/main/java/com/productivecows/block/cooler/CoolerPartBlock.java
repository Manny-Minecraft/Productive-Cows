package com.productivecows.block.cooler;

import com.productivecows.multiblock.CoolerPart;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** A block of the Cooler tower. {@link #FORMED} is set by the Controller while the tower is complete. */
public class CoolerPartBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private final CoolerPart part;

    public CoolerPartBlock(CoolerPart part, Properties properties) {
        super(properties);
        this.part = part;
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    public CoolerPart part() {
        return part;
    }

    /** What a block state means for the structure check. */
    public static CoolerPart partOf(BlockState state) {
        if (state.getBlock() instanceof CoolerPartBlock block) {
            return block.part;
        }
        return state.isAir() ? CoolerPart.EMPTY : CoolerPart.OTHER;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }
}
