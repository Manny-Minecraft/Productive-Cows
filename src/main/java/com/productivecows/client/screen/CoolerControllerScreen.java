package com.productivecows.client.screen;

import com.productivecows.Config;
import com.productivecows.ProductiveCows;
import com.productivecows.menu.CoolerControllerMenu;
import com.productivecows.multiblock.CoolerStructure;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Cooling Tower GUI, in the style of the other machines: energy bar, one tank holding every fluid as a layer
 * (oldest at the bottom, scaled on the shared capacity), the cycle's progress line and a 3x3 output. The "i"
 * button lists the tower's size, Stamps, ingots per cycle and energy use.
 */
public class CoolerControllerScreen extends AbstractContainerScreen<CoolerControllerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ProductiveCows.MODID, "textures/gui/cooler_controller.png");

    // Inner areas of the frames drawn in the texture
    static final int ENERGY_X = 9;
    static final int TANK_X = 29;
    static final int GAUGE_Y = 16;
    static final int TANK_W = 42;
    static final int ENERGY_W = 14;
    static final int GAUGE_H = 52;
    public static final int PROGRESS_X = 78;
    public static final int PROGRESS_Y = 42;
    public static final int PROGRESS_W = 30;
    public static final int PROGRESS_H = 2;
    static final int INFO_X = 158;
    static final int INFO_Y = 4;
    static final int INFO_SIZE = 9;

    public CoolerControllerScreen(CoolerControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 72;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderGaugeTooltips(graphics, mouseX, mouseY);
    }

    /** Top edge (in pixels from the tank's bottom) of each layer; rounding on the running total keeps them exact. */
    private int[] layerTops() {
        int capacity = Math.max(1, Math.max(menu.getCapacity(), totalFluid()));
        int[] tops = new int[CoolerControllerMenu.FLUIDS_SHOWN];
        long sum = 0;
        int previous = 0;
        for (int i = 0; i < tops.length; i++) {
            int amount = menu.getFluidAmount(i);
            sum += amount;
            int top = (int) Math.min(GAUGE_H, sum * GAUGE_H / capacity);
            if (amount > 0 && top <= previous) {
                top = Math.min(GAUGE_H, previous + 1); // every fluid stays visible, at least one pixel
            }
            tops[i] = amount > 0 ? top : previous;
            previous = tops[i];
        }
        return tops;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 220, 166);

        // Fluid layers, oldest at the bottom
        int[] tops = layerTops();
        int bottom = y + GAUGE_Y + GAUGE_H;
        int previous = 0;
        for (int i = 0; i < tops.length; i++) {
            int amount = menu.getFluidAmount(i);
            if (amount > 0 && tops[i] > previous && menu.getFluid(i) != Fluids.EMPTY) {
                FluidGuiRenderer.render(graphics, new FluidStack(menu.getFluid(i), amount),
                        x + TANK_X, bottom - tops[i], TANK_W, tops[i] - previous);
            }
            previous = tops[i];
        }

        // Energy, from the bar sprite stored in the texture (like the other machines)
        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        if (energy > 0 && maxEnergy > 0) {
            int h = Math.max(1, Math.min(GAUGE_H, (int) ((long) energy * GAUGE_H / maxEnergy)));
            graphics.blit(TEXTURE, x + ENERGY_X, y + GAUGE_Y + GAUGE_H - h, 204, 1 + (GAUGE_H - h), ENERGY_W, h, 220, 166);
        }

        // Cycle progress, with the coolers' colours
        if (menu.getProgress() > 0 && menu.getCycleTicks() > 0) {
            int w = Math.min(PROGRESS_W, menu.getProgress() * PROGRESS_W / menu.getCycleTicks());
            graphics.fillGradient(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + w, y + PROGRESS_Y + PROGRESS_H, 0xFF00F0FF, 0xFF00FF88);
        }
    }

    private void renderGaugeTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        if (isHovering(TANK_X, GAUGE_Y, TANK_W, GAUGE_H, mouseX, mouseY)) {
            int fromBottom = topPos + GAUGE_Y + GAUGE_H - mouseY;
            int[] tops = layerTops();
            int previous = 0;
            for (int i = 0; i < tops.length; i++) {
                if (menu.getFluidAmount(i) > 0 && fromBottom > previous && fromBottom <= tops[i]) {
                    lines.add(new FluidStack(menu.getFluid(i), 1).getHoverName().copy().withStyle(ChatFormatting.AQUA));
                    lines.add(Component.literal(menu.getFluidAmount(i) + " mB").withStyle(ChatFormatting.GRAY));
                }
                previous = tops[i];
            }
            if (lines.isEmpty()) {
                lines.add(Component.translatable("gui.productivecows.cooler.tank_free").withStyle(ChatFormatting.GRAY));
            }
            lines.add(Component.translatable("gui.productivecows.cooler.shared", totalFluid(), menu.getCapacity()).withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(PROGRESS_X, PROGRESS_Y - 4, PROGRESS_W, PROGRESS_H + 8, mouseX, mouseY)) {
            int stamps = menu.getActiveStamps();
            if (stamps == 0) {
                lines.add(Component.translatable("gui.productivecows.cooler.idle").withStyle(ChatFormatting.GRAY));
            } else {
                int cost = stamps * Math.max(0, Config.coolerEnergyPerStamp);
                lines.add(Component.translatable("tooltip.productivecows.jade.progress", menu.getProgress() * 100 / Math.max(1, menu.getCycleTicks()))
                        .withStyle(ChatFormatting.GREEN));
                lines.add(Component.translatable("gui.productivecows.cooler.cycle", menu.getCycleIngots(), stamps).withStyle(ChatFormatting.GRAY));
                boolean noPower = menu.getEnergy() < cost;
                lines.add(Component.translatable(noPower ? "gui.productivecows.cooler.no_power" : "gui.productivecows.consuming", cost)
                        .withStyle(noPower ? ChatFormatting.RED : ChatFormatting.YELLOW));
            }
        } else if (isHovering(ENERGY_X, GAUGE_Y, ENERGY_W, GAUGE_H, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.productivecows.energy").withStyle(ChatFormatting.RED));
            lines.add(Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " RF").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(INFO_X, INFO_Y, INFO_SIZE, INFO_SIZE, mouseX, mouseY)) {
            int stamps = menu.getStamps();
            int ingots = stamps * CoolerStructure.INGOTS_PER_STAMP;
            lines.add(Component.translatable("gui.productivecows.cooler.info_size", menu.getHeight()).withStyle(ChatFormatting.WHITE));
            lines.add(Component.translatable("gui.productivecows.cooler.info_stamps", stamps, ingots).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.productivecows.cooler.info_tanks", menu.getTanks(), menu.getCapacity()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.productivecows.cooler.info_cycle", String.format(java.util.Locale.ROOT, "%.1f", menu.getCycleTicks() / 20f))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.productivecows.cooler.info_energy", stamps * Math.max(0, Config.coolerEnergyPerStamp))
                    .withStyle(ChatFormatting.YELLOW));
        }
        if (!lines.isEmpty()) {
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private int totalFluid() {
        int total = 0;
        for (int i = 0; i < CoolerControllerMenu.FLUIDS_SHOWN; i++) {
            total += menu.getFluidAmount(i);
        }
        return total;
    }
}
