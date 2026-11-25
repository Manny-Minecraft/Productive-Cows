package com.productivecows.block.cooler;

import com.productivecows.ProductiveCows;
import com.productivecows.block.entity.CoolerControllerBlockEntity;
import com.productivecows.multiblock.CoolerPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Brain of the Cooler multiblock. Its screen must face out of the tower; right-click reports the tower's state. */
public class CoolerControllerBlock extends CoolerPartBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CoolerControllerBlock(Properties properties) {
        super(CoolerPart.CONTROLLER, properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoolerControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ProductiveCows.COOLER_CONTROLLER_BE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((CoolerControllerBlockEntity) be).serverTick();
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                                Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        // A bucket (or any fluid container) with fluid in it is poured into the tower instead of opening the GUI
        if (net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(stack).isEmpty()) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CoolerControllerBlockEntity controller
                && !controller.pourFrom(player, hand)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.productivecows.cooler.fluid_refused"), true);
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide && player.isShiftKeyDown()) {
            // Sneak-click also switches this player's build preview of the tower (client side only)
            com.productivecows.client.CoolerPreview.toggle(pos);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CoolerControllerBlockEntity controller) {
            // A formed tower opens its GUI; sneaking, or an incomplete tower, reports the tower's state instead
            var result = controller.recheck();
            if (result.formed() && !player.isShiftKeyDown()) {
                player.openMenu(controller, pos);
            } else {
                player.displayClientMessage(controller.describe(result), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CoolerControllerBlockEntity controller) {
            controller.unform();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
