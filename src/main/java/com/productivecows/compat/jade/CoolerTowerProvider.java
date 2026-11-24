package com.productivecows.compat.jade;

import com.productivecows.ProductiveCows;
import com.productivecows.block.cooler.CoolerPortBlock;
import com.productivecows.block.entity.CoolerControllerBlockEntity;
import com.productivecows.block.entity.CoolerPortBlockEntity;
import com.productivecows.multiblock.CoolerStructure;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Jade lines for the Cooler tower, shown on its Controller and on its Ports. */
public class CoolerTowerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final CoolerTowerProvider INSTANCE = new CoolerTowerProvider();
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ProductiveCows.MODID, "cooler_tower");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        String key = "tooltip.productivecows.jade.cooler.";
        if (data.contains("Mode")) {
            tooltip.add(Component.translatable(key + "port_" + data.getString("Mode")).withStyle(ChatFormatting.GRAY));
        }
        if (!data.getBoolean("Formed")) {
            tooltip.add(Component.translatable(key + "not_formed").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.translatable(key + "size", data.getInt("Height"), data.getInt("Stamps"), data.getInt("Tanks")));
        tooltip.add(Component.translatable(key + "fluids", data.getInt("FluidTypes"), data.getInt("Tanks"),
                data.getInt("FluidTotal"), data.getInt("FluidCapacity")).withStyle(ChatFormatting.AQUA));
        int total = data.getInt("CycleTicks");
        if (data.getInt("Progress") > 0 && total > 0) {
            tooltip.add(Component.translatable("tooltip.productivecows.jade.progress", data.getInt("Progress") * 100 / total)
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        CoolerControllerBlockEntity controller = null;
        if (accessor.getBlockEntity() instanceof CoolerControllerBlockEntity c) {
            controller = c;
        } else if (accessor.getBlockEntity() instanceof CoolerPortBlockEntity port) {
            data.putString("Mode", accessor.getBlockState().getValue(CoolerPortBlock.MODE).getSerializedName());
            controller = port.boundController();
        }
        CoolerStructure.Result structure = controller == null ? null : controller.structure();
        boolean formed = structure != null && structure.formed();
        data.putBoolean("Formed", formed);
        if (!formed) {
            return;
        }
        data.putInt("Height", structure.height());
        data.putInt("Stamps", structure.stamps());
        data.putInt("Tanks", structure.tanks());
        data.putInt("FluidTypes", controller.fluidContents().size());
        data.putInt("FluidTotal", controller.fluidContents().values().stream().mapToInt(Integer::intValue).sum());
        data.putInt("FluidCapacity", controller.fluidCapacity());
        data.putInt("Progress", controller.progress());
        data.putInt("CycleTicks", controller.cycleTicks());
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
