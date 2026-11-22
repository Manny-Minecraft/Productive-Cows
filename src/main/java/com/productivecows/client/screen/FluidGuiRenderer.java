package com.productivecows.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/** Draws a fluid in a GUI gauge with its animated still texture, tinted and tiled from the bottom up. */
public final class FluidGuiRenderer {
    private FluidGuiRenderer() {}

    public static void render(GuiGraphics graphics, FluidStack stack, int x, int y, int width, int height) {
        if (stack.isEmpty() || width <= 0 || height <= 0) {
            return;
        }
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(stack.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(stack));
        int color = ext.getTintColor(stack);

        graphics.flush(); // keep the order with what the screen drew before
        Matrix4f matrix = graphics.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int ty = 0; ty < height; ty += 16) {
            int h = Math.min(16, height - ty);
            int y1 = y + height - ty;
            int y0 = y1 - h;
            float v0 = sprite.getV(1f - h / 16f);
            float v1 = sprite.getV1();
            for (int tx = 0; tx < width; tx += 16) {
                int w = Math.min(16, width - tx);
                int x0 = x + tx;
                int x1 = x0 + w;
                float u0 = sprite.getU0();
                float u1 = sprite.getU(w / 16f);
                buffer.addVertex(matrix, x0, y1, 0).setUv(u0, v1).setColor(color);
                buffer.addVertex(matrix, x1, y1, 0).setUv(u1, v1).setColor(color);
                buffer.addVertex(matrix, x1, y0, 0).setUv(u1, v0).setColor(color);
                buffer.addVertex(matrix, x0, y0, 0).setUv(u0, v0).setColor(color);
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }
}
