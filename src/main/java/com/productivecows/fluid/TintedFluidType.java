package com.productivecows.fluid;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Consumer;

public class TintedFluidType extends FluidType {
    
    private static final ResourceLocation WATER_OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");
    private static final ResourceLocation UNDERWATER = ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");

    private final int tintColor;
    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;

    public TintedFluidType(Properties properties, int tintColor, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        super(properties);
        this.tintColor = tintColor;
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return stillTexture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowingTexture;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return WATER_OVERLAY;
            }

            @Override
            public ResourceLocation getRenderOverlayTexture(net.minecraft.client.Minecraft mc) {
                return UNDERWATER;
            }

            @Override
            public int getTintColor() {
                // Opaque tint: the transparency comes from the water texture itself
                return 0xFF000000 | tintColor;
            }

            @Override
            public org.joml.Vector3f modifyFogColor(net.minecraft.client.Camera camera, float partialTick, net.minecraft.client.multiplayer.ClientLevel level,
                                                    int renderDistance, float darkenWorldAmount, org.joml.Vector3f fluidFogColor) {
                // Seen from inside, the fluid tints the fog with its own colour, like water does
                return new org.joml.Vector3f(((tintColor >> 16) & 0xFF) / 255f, ((tintColor >> 8) & 0xFF) / 255f, (tintColor & 0xFF) / 255f);
            }
        });
    }
}
