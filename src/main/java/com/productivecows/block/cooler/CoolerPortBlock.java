package com.productivecows.block.cooler;

import com.productivecows.block.entity.CoolerPortBlockEntity;
import com.productivecows.multiblock.CoolerPart;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Cooler Port: the tower's connection to pipes and cables. Right-click switches between INPUT (accepts fluids and
 * energy) and OUTPUT (gives the cooled items).
 */
public class CoolerPortBlock extends CoolerPartBlock implements EntityBlock {
    public enum Mode implements StringRepresentable {
        INPUT, OUTPUT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public CoolerPortBlock(Properties properties) {
        super(CoolerPart.PORT, properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(MODE, Mode.INPUT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoolerPortBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            Mode next = state.getValue(MODE) == Mode.INPUT ? Mode.OUTPUT : Mode.INPUT;
            level.setBlock(pos, state.setValue(MODE, next), Block.UPDATE_ALL);
            // Pipes and cables cache what a block offers: tell them the port changed
            level.invalidateCapabilities(pos);
            player.displayClientMessage(Component.translatable("message.productivecows.cooler.port_" + next.getSerializedName()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
