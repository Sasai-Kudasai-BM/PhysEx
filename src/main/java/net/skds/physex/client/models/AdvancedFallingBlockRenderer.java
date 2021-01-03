package net.skds.physex.client.models;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.Random;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.skds.physex.entity.AdvancedFallingBlockEntity;

@OnlyIn(Dist.CLIENT)
public class AdvancedFallingBlockRenderer extends EntityRenderer<AdvancedFallingBlockEntity> {
	public AdvancedFallingBlockRenderer(EntityRendererManager renderManagerIn) {
		super(renderManagerIn);
		this.shadowSize = 0.5F;
	}

	@Override
	public boolean shouldRender(AdvancedFallingBlockEntity livingEntityIn, ClippingHelper camera, double camX,
			double camY, double camZ) {
		double d0 = this.renderManager.squareDistanceTo(livingEntityIn);
		final int d = 32;
		if (d0 > d * d) {
			return false;
		}
		if (!livingEntityIn.isInRangeToRender3d(camX, camY, camZ)) {
			return false;
		} else if (livingEntityIn.ignoreFrustumCheck) {
			return true;
		} else {
			AxisAlignedBB axisalignedbb = livingEntityIn.getRenderBoundingBox().grow(0.5D);
			if (axisalignedbb.hasNaN() || axisalignedbb.getAverageEdgeLength() == 0.0D) {
				axisalignedbb = new AxisAlignedBB(livingEntityIn.getPosX() - 2.0D, livingEntityIn.getPosY() - 2.0D,
						livingEntityIn.getPosZ() - 2.0D, livingEntityIn.getPosX() + 2.0D,
						livingEntityIn.getPosY() + 2.0D, livingEntityIn.getPosZ() + 2.0D);
			}

			return camera.isBoundingBoxInFrustum(axisalignedbb);
		}
	}

	@SuppressWarnings("deprecation")
	public void render(AdvancedFallingBlockEntity entityIn, float entityYaw, float partialTicks,
			MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {

		BlockState blockstate = entityIn.getBlockState();
		if (blockstate.getRenderType() == BlockRenderType.MODEL) {
			World world = entityIn.getWorldObj();
			if (blockstate != world.getBlockState(entityIn.func_233580_cy_())
					&& blockstate.getRenderType() != BlockRenderType.INVISIBLE) {
				matrixStackIn.push();
				BlockPos blockpos = new BlockPos(entityIn.getPosX(), entityIn.getBoundingBox().maxY,
						entityIn.getPosZ());
				matrixStackIn.translate(-0.5D, 0.0D, -0.5D);
				BlockRendererDispatcher blockrendererdispatcher = Minecraft.getInstance().getBlockRendererDispatcher();
				for (net.minecraft.client.renderer.RenderType type : net.minecraft.client.renderer.RenderType
						.getBlockRenderTypes()) {
					if (RenderTypeLookup.canRenderInLayer(blockstate, type)) {
						net.minecraftforge.client.ForgeHooksClient.setRenderLayer(type);
						blockrendererdispatcher.getBlockModelRenderer().renderModel(world,
								blockrendererdispatcher.getModelForState(blockstate), blockstate, blockpos,
								matrixStackIn, bufferIn.getBuffer(type), false, new Random(),
								blockstate.getPositionRandom(entityIn.getOrigin()), OverlayTexture.NO_OVERLAY);
					}
				}
				net.minecraftforge.client.ForgeHooksClient.setRenderLayer(null);
				matrixStackIn.pop();
				super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public ResourceLocation getEntityTexture(AdvancedFallingBlockEntity entity) {
		return AtlasTexture.LOCATION_BLOCKS_TEXTURE;
	}
}
