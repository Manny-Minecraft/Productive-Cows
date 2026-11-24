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
 * Cooler Controller GUI. One gauge per possible fluid (as many as the tower has Tanks, up to 8; the others are
 * locked), all filled against the shared capacity, an energy bar and the output buffer.
 */
public class CoolerControllerScreen extends AbstractContainerScreen<CoolerControllerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ProductiveCows.MODID, "textures/gui/cooler_controller.png");

    // Must match the gauge frames drawn in the texture
    static final int GAUGE_X = 9;
    static final int GAUGE_Y = 19;
    static final int GAUGE_W = 12;
    static final int GAUGE_H = 46;
    static final int GAUGE_STEP = 15;
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

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        int capacity = menu.getCapacity();
        int tanks = menu.getTanks();
        for (int i = 0; i < CoolerControllerMenu.FLUIDS_SHOWN; i++) {
            int gx = x + GAUGE_X + i * GAUGE_STEP;
            int gy = y + GAUGE_Y;
            if (i >= tanks) {
                // Locked: the tower needs one more Tank for this fluid
                graphics.fill(gx, gy, gx + GAUGE_W, gy + GAUGE_H, 0xFF4A4A4A);
                continue;
            }
            int amount = menu.getFluidAmount(i);
            if (amount > 0 && capacity > 0 && menu.getFluid(i) != Fluids.EMPTY) {
                int h = Math.max(1, Math.min(GAUGE_H, (int) ((long) amount * GAUGE_H / capacity)));
                FluidGuiRenderer.render(graphics, new FluidStack(menu.getFluid(i), amount), gx, gy + GAUGE_H - h, GAUGE_W, h);
            }
        }

        graphics.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + PROGRESS_W, y + PROGRESS_Y + PROGRESS_H, 0xFF373737);
        if (menu.getProgress() > 0 && menu.getCycleTicks() > 0) {
            int w = Math.min(PROGRESS_W, menu.getProgress() * PROGRESS_W / menu.getCycleTicks());
            graphics.fillGradient(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + w, y + PROGRESS_Y + PROGRESS_H, 0xFF00F0FF, 0xFF00FF88);
        }

        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        if (energy > 0 && maxEnergy > 0) {
            int h = Math.max(1, Math.min(GAUGE_H, (int) ((long) energy * GAUGE_H / maxEnergy)));
            int ex = x + ENERGY_X;
            int ey = y + GAUGE_Y + GAUGE_H - h;
            graphics.fillGradient(ex, ey, ex + ENERGY_W, y + GAUGE_Y + GAUGE_H, 0xFFE0402A, 0xFF8A1A10);
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
        int capacity = menu.getCapacity();
        for (int i = 0; i < CoolerControllerMenu.FLUIDS_SHOWN; i++) {
            int gx = leftPos + GAUGE_X + i * GAUGE_STEP;
            int gy = topPos + GAUGE_Y;
            if (!isHovering(gx - leftPos, gy - topPos, GAUGE_W, GAUGE_H, mouseX, mouseY)) {
                continue;
            }
            List<Component> lines = new ArrayList<>();
            if (i >= menu.getTanks()) {
                lines.add(Component.translatable("gui.productivecows.cooler.fluid_locked").withStyle(ChatFormatting.GRAY));
            } else if (menu.getFluidAmount(i) > 0) {
                lines.add(new FluidStack(menu.getFluid(i), 1).getHoverName().copy().withStyle(ChatFormatting.AQUA));
                lines.add(Component.literal(menu.getFluidAmount(i) + " mB").withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.translatable("gui.productivecows.cooler.fluid_empty").withStyle(ChatFormatting.GRAY));
            }
            lines.add(Component.translatable("gui.productivecows.cooler.shared", totalFluid(), capacity).withStyle(ChatFormatting.DARK_GRAY));
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
                lines.add(Component.translatable(menu.getEnergy() < cost ? "gui.productivecows.cooler.no_power" : "gui.productivecows.consuming", cost)
                        .withStyle(menu.getEnergy() < cost ? ChatFormatting.RED : ChatFormatting.YELLOW));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (isHovering(ENERGY_X, GAUGE_Y, ENERGY_W, GAUGE_H, mouseX, mouseY)) {
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
