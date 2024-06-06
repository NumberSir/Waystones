package net.blay09.mods.waystones.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.blay09.mods.waystones.block.SharestoneBlock;
import net.blay09.mods.waystones.block.entity.SharestoneBlockEntity;
import net.blay09.mods.waystones.config.WaystonesConfig;
import net.blay09.mods.waystones.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class SharestoneRenderer implements BlockEntityRenderer<SharestoneBlockEntity> {

    private static final Material MATERIAL = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.fromNamespaceAndPath("minecraft", "waystone_overlays/sharestone_color"));

    private static ItemStack warpStoneItem;

    public SharestoneRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SharestoneBlockEntity tileEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int combinedLightIn, int combinedOverlayIn) {
        Level level = tileEntity.getLevel();
        BlockState state = tileEntity.getBlockState();
        if (level == null || state.getValue(SharestoneBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }

        long gameTime = level.getGameTime();

        DyeColor color = ((SharestoneBlock) state.getBlock()).getColor();
        if (color != null) {
            poseStack.pushPose();
            poseStack.translate(0.5f, 0f, 0.5f);
            poseStack.mulPose(Axis.XN.rotationDegrees(180f));
            poseStack.translate(0f, -2f, 0f);
            float scale = 1.01f;
            poseStack.scale(0.5f, 0.5f, 0.5f);
            poseStack.scale(scale, scale, scale);
            VertexConsumer vertexBuilder = MATERIAL.buffer(buffer, RenderType::entityCutout);
            int light = WaystonesConfig.getActive().client.disableTextGlow ? combinedLightIn : 15728880;
            int overlay = WaystonesConfig.getActive().client.disableTextGlow ? combinedOverlayIn : OverlayTexture.NO_OVERLAY;
            int textureDiffuseColor = color.getTextureDiffuseColor();
            float r = ((textureDiffuseColor & 16711680) >> 16) / 255f;
            float g = ((textureDiffuseColor & 65280) >> 8) / 255f;
            float b = ((textureDiffuseColor & 255)) / 255f;
            // TODO 1.21 model.renderToBuffer(poseStack, vertexBuilder, light, overlay, r, g, b, 1f);
            poseStack.popPose();
        }

        if (warpStoneItem == null) {
            warpStoneItem = new ItemStack(ModItems.warpStone);
            level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.UNBREAKING).ifPresent(it -> warpStoneItem.enchant(it, 1));
        }

        float angle = gameTime / 2f % 360;
        float offsetY = (float) Math.sin(gameTime / 8f) * 0.025f;
        poseStack.pushPose();
        poseStack.translate(0.5f, 1f + offsetY, 0.5f);
        poseStack.mulPose(Axis.YN.rotationDegrees(angle));
        poseStack.scale(0.5f, 0.5f, 0.5f);
        Minecraft.getInstance().getItemRenderer().renderStatic(warpStoneItem, ItemDisplayContext.FIXED, combinedLightIn, combinedOverlayIn, poseStack, buffer, level, 0);
        poseStack.popPose();
    }
}
