package com.productivecows.block;

import com.productivecows.ProductiveCows;
import com.productivecows.block.entity.CreativeEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Creative-only block that gives unlimited RF to every block next to it. For testing and creative builds. */
public class CreativeEnergyBlock extends Block implements EntityBlock {

    public CreativeEnergyBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeEnergyBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ProductiveCows.CREATIVE_ENERGY_BE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((CreativeEnergyBlockEntity) be).serverTick();
    }
}
