package net.skds.physex.registry;

import net.minecraft.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.skds.physex.client.models.AdvancedFallingBlockRenderer;
import net.skds.physex.entity.AdvancedFallingBlockEntity;

@OnlyIn(Dist.CLIENT)
public class RenderRegistry {

	@SuppressWarnings("unchecked")
	public static void register() {
        RenderingRegistry.registerEntityRenderingHandler((EntityType<AdvancedFallingBlockEntity>) Entities.ADVANCED_FALLING_BLOCK.get(), AdvancedFallingBlockRenderer::new);
	}	
}