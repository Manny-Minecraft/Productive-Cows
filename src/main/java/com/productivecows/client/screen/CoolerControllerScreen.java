package com.productivecows.client.screen;

import com.productivecows.ProductiveCows;
import com.productivecows.menu.CoolerControllerMenu;
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
 * Cooler Controller GUI: one tank holding every fluid as a layer, from the oldest at the bottom, scaled on the
 * shared capacity; an energy bar, the cycle's progress and the output buffer.
 */
public class CoolerControllerScreen extends AbstractContainerScreen<CoolerControllerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ProductiveCows.MODID, "textures/gui/cooler_controller.png");

    // Must match the frames drawn in the texture
    static final int TANK_X = 9;
    static final int TANK_Y = 19;
    static final int TANK_W = 138;
    static final int TANK_H = 46;
    static final int ENERGY_X = 155;
    static final int ENERGY_W = 12;
    // Progress bar, right of the "Output" label; JEI's click area covers it too
    public static final int PROGRESS_X = 48;
    public static final int PROGRESS_Y = 69;
    public static final int PROGRESS_W = 120;
    public static final int PROGRESS_H = 3;

    public CoolerControllerScreen(CoolerControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 192;
        inventoryLabelY = CoolerControllerMenu.INVENTORY_Y - 11;
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
            int top = (int) Math.min(TANK_H, sum * TANK_H / capacity);
            if (amount > 0 && top <= previous) {
                top = Math.min(TANK_H, previous + 1); // every fluid stays visible, at least one pixel
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
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        int[] tops = layerTops();
        int bottom = y + TANK_Y + TANK_H;
        int previous = 0;
        for (int i = 0; i < tops.length; i++) {
            int amount = menu.getFluidAmount(i);
            if (amount > 0 && tops[i] > previous && menu.getFluid(i) != Fluids.EMPTY) {
                FluidGuiRenderer.render(graphics, new FluidStack(menu.getFluid(i), amount),
                        x + TANK_X, bottom - tops[i], TANK_W, tops[i] - previous);
            }
            previous = tops[i];
        }

        graphics.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + PROGRESS_W, y + PROGRESS_Y + PROGRESS_H, 0xFF373737);
        if (menu.getProgress() > 0 && menu.getCycleTicks() > 0) {
            int w = Math.min(PROGRESS_W, menu.getProgress() * PROGRESS_W / menu.getCycleTicks());
            graphics.fillGradient(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + w, y + PROGRESS_Y + PROGRESS_H, 0xFF00F0FF, 0xFF00FF88);
        }

        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        if (energy > 0 && maxEnergy > 0) {
            int h = Math.max(1, Math.min(TANK_H, (int) ((long) energy * TANK_H / maxEnergy)));
            int ex = x + ENERGY_X;
            graphics.fillGradient(ex, y + TANK_Y + TANK_H - h, ex + ENERGY_W, y + TANK_Y + TANK_H, 0xFFE0402A, 0xFF8A1A10);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.getHeight() > 0) {
            Component size = Component.translatable("gui.productivecows.cooler.size", menu.getHeight(), menu.getStamps());
            graphics.drawString(font, size, imageWidth - 8 - font.width(size), titleLabelY, 0x404040, false);
        }
        graphics.drawString(font, Component.translatable("gui.productivecows.cooler.output"), 8, CoolerControllerMenu.OUTPUT_Y - 11, 0x404040, false);
    }

    private void renderGaugeTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            int fromBottom = topPos + TANK_Y + TANK_H - mouseY;
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
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (isHovering(PROGRESS_X, PROGRESS_Y - 2, PROGRESS_W, PROGRESS_H + 4, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            int stamps = menu.getActiveStamps();
            if (stamps == 0) {
                lines.add(Component.translatable("gui.productivecows.cooler.idle").withStyle(ChatFormatting.GRAY));
            } else {
                int cost = stamps * Math.max(0, com.productivecows.Config.coolerEnergyPerStamp);
                lines.add(Component.translatable("tooltip.productivecows.jade.progress", menu.getProgress() * 100 / Math.max(1, menu.getCycleTicks()))
                        .withStyle(ChatFormatting.GREEN));
                lines.add(Component.translatable("gui.productivecows.cooler.cycle", menu.getCycleIngots(), stamps).withStyle(ChatFormatting.GRAY));
                boolean noPower = menu.getEnergy() < cost;
                lines.add(Component.translatable(noPower ? "gui.productivecows.cooler.no_power" : "gui.productivecows.consuming", cost)
                        .withStyle(noPower ? ChatFormatting.RED : ChatFormatting.YELLOW));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (isHovering(ENERGY_X, TANK_Y, ENERGY_W, TANK_H, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.productivecows.energy").withStyle(ChatFormatting.RED));
            lines.add(Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " RF").withStyle(ChatFormatting.GRAY));
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
