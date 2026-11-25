package com.productivecows.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.productivecows.Config;
import com.productivecows.ProductiveCows;
import com.productivecows.block.cooler.CoolerControllerBlock;
import com.productivecows.block.cooler.CoolerPartBlock;
import com.productivecows.multiblock.CoolerPart;
import com.productivecows.multiblock.CoolerStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Build preview of the Cooler tower, toggled per player by sneak + right-clicking a Controller. Missing blocks are
 * drawn as see-through ghosts of a complete, valid tower; blocks that don't belong are outlined in red. Positions
 * already holding an allowed part are left alone, and the preview turns itself off once the tower forms.
 */
public final class CoolerPreview {
    private static final Set<BlockPos> ACTIVE = new HashSet<>();
    private static final float GHOST_ALPHA = 0.45F;
    private static final double MAX_DISTANCE = 64.0;

    private CoolerPreview() {}

    public static void toggle(BlockPos controller) {
        BlockPos pos = controller.immutable();
        if (!ACTIVE.remove(pos)) {
            ACTIVE.add(pos);
        }
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        MultiBufferSource ghosts = type -> new AlphaVertexConsumer(buffers.getBuffer(Sheets.translucentCullBlockSheet()), GHOST_ALPHA);

        ACTIVE.removeIf(pos -> !(level.getBlockState(pos).getBlock() instanceof CoolerControllerBlock)
                || level.getBlockState(pos).getValue(CoolerPartBlock.FORMED));
        for (BlockPos controller : ACTIVE) {
            if (controller.distToCenterSqr(camera) > MAX_DISTANCE * MAX_DISTANCE) {
                continue;
            }
            Direction facing = level.getBlockState(controller).getValue(CoolerControllerBlock.FACING);
            int maxHeight = Math.max(CoolerStructure.MIN_HEIGHT, Config.coolerMaxHeight);
            CoolerStructure.Frame frame = CoolerStructure.frame(controller, facing, maxHeight,
                    pos -> CoolerPartBlock.partOf(level.getBlockState(pos)));
            BlockPos centre = controller.relative(facing.getOpposite());
            int top = frame.bottom() + frame.height() - 1;
            for (Map.Entry<BlockPos, CoolerPart> entry : CoolerStructure.blueprint(controller, facing, frame).entrySet()) {
                BlockPos pos = entry.getKey();
                boolean cap = pos.getY() == frame.bottom() || pos.getY() == top;
                CoolerPart actual = CoolerPartBlock.partOf(level.getBlockState(pos));
                if (CoolerStructure.allowed(cap, pos.getX() - centre.getX(), pos.getZ() - centre.getZ()).contains(actual)) {
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
                if (actual == CoolerPart.EMPTY) {
                    BlockState ghost = ghostOf(entry.getValue());
                    if (ghost != null) {
                        // Slightly smaller than a block, so it reads as a hologram and never z-fights
                        poseStack.translate(0.02, 0.02, 0.02);
                        poseStack.scale(0.96F, 0.96F, 0.96F);
                        mc.getBlockRenderer().renderSingleBlock(ghost, poseStack, ghosts, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    }
                } else {
                    VertexConsumer lines = buffers.getBuffer(RenderType.lines());
                    LevelRenderer.renderLineBox(poseStack, lines, new AABB(-0.002, -0.002, -0.002, 1.002, 1.002, 1.002), 1.0F, 0.2F, 0.2F, 1.0F);
                }
                poseStack.popPose();
            }
        }
        buffers.endBatch(Sheets.translucentCullBlockSheet());
        buffers.endBatch(RenderType.lines());
    }

    private static BlockState ghostOf(CoolerPart part) {
        return switch (part) {
            case CASING -> ProductiveCows.COOLER_BLOCK.get().defaultBlockState();
            case TANK -> ProductiveCows.COOLER_TANK.get().defaultBlockState();
            case PORT -> ProductiveCows.COOLER_PORT.get().defaultBlockState();
            case STAMP -> ProductiveCows.COOLER_STAMP.get().defaultBlockState();
            default -> null;
        };
    }

    /** Passes vertices through, fading their colour to make the ghost blocks see-through. */
    private record AlphaVertexConsumer(VertexConsumer inner, float alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            inner.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int a) {
            inner.setColor(red, green, blue, (int) (a * alpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            inner.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            inner.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            inner.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            inner.setNormal(x, y, z);
            return this;
        }
    }
}
