package com.uncraftbar.sandsoftime.client;

import com.mojang.math.Axis;
import com.uncraftbar.sandsoftime.entities.TimeAcceleratorEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Renders acceleration info on the four side faces of BLOCK-mode TimeAcceleratorEntities.
 * Each face shows the speed multiplier (green) and remaining time (yellow), like a sign.
 * ENTITY-mode accelerators are invisible; their info is shown via {@link AccelerationHudOverlay}.
 */
public class TimeAcceleratorRenderer extends EntityRenderer<TimeAcceleratorEntity> {

    private final Font font;

    /**
     * Face render configs: { translateX, translateZ, rotationDegreesAroundY }.
     * Entity origin is block center-bottom (x+0.5, y, z+0.5).
     * Offset 0.501 prevents z-fighting with the block face.
     *
     * Text default normal is -Z (faces toward negative Z).
     * Rotations align the text normal with each face's outward normal:
     *   North (-Z): 0°    South (+Z): 180°    East (+X): -90°    West (-X): 90°
     */
    private static final float[][] FACE_CONFIGS = {
            {  0.0F, -0.501F,    0F },  // North
            {  0.0F,  0.501F,  180F },  // South
            {  0.501F, 0.0F,  -90F },  // East
            { -0.501F, 0.0F,   90F },  // West
    };

    private static final float SCALE = 0.02F;
    private static final int LINE_GAP = 2; // pixels between lines

    public TimeAcceleratorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.font = context.getFont();
    }

    @Override
    public ResourceLocation getTextureLocation(TimeAcceleratorEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }

    @Override
    public void render(TimeAcceleratorEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        if (entity.getMode() != TimeAcceleratorEntity.Mode.BLOCK) return;

        int speed = entity.getSpeedMultiplier();
        int remainingSec = Math.max(0, entity.getRemainingTicks() / 20);

        String line1 = speed + "x";
        String line2 = remainingSec + "s";

        int w1 = font.width(line1);
        int w2 = font.width(line2);
        int fontH = font.lineHeight; // 9 pixels
        int totalH = fontH * 2 + LINE_GAP;

        int fullBright = LightTexture.FULL_BRIGHT;

        for (float[] face : FACE_CONFIGS) {
            poseStack.pushPose();

            // Translate to face center (Y=0.5 is block center height)
            poseStack.translate(face[0], 0.5, face[1]);

            // Rotate so text faces outward from this face
            poseStack.mulPose(Axis.YP.rotationDegrees(face[2]));

            // Scale: negative X un-mirrors text after Y rotation, negative Y flips font-down to world-up
            poseStack.scale(-SCALE, -SCALE, SCALE);

            // Vertical centering: top of line 1
            float baseY = -totalH / 2.0F;

            // Line 1: speed (green)
            float x1 = -w1 / 2.0F;
            font.drawInBatch(line1, x1, baseY, 0x55FF55, false,
                    poseStack.last().pose(), buffer,
                    Font.DisplayMode.SEE_THROUGH, 0x80000000, fullBright);
            font.drawInBatch(line1, x1, baseY, 0x55FF55, false,
                    poseStack.last().pose(), buffer,
                    Font.DisplayMode.NORMAL, 0, fullBright);

            // Line 2: time remaining (yellow)
            float y2 = baseY + fontH + LINE_GAP;
            float x2 = -w2 / 2.0F;
            font.drawInBatch(line2, x2, y2, 0xFFFF55, false,
                    poseStack.last().pose(), buffer,
                    Font.DisplayMode.SEE_THROUGH, 0x80000000, fullBright);
            font.drawInBatch(line2, x2, y2, 0xFFFF55, false,
                    poseStack.last().pose(), buffer,
                    Font.DisplayMode.NORMAL, 0, fullBright);

            poseStack.popPose();
        }
    }
}
