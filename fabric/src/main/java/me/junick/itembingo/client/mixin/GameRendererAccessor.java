package me.junick.itembingo.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code GuiRenderer.draw()} hard-reads {@code GameRenderer.mainRenderTarget}
 * as its output. Swapping the field for the duration of one render is what
 * lets the board-image export draw into an offscreen target the player never
 * sees. The previous value is always restored in a finally block.
 */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("mainRenderTarget")
    @Mutable
    void itembingo$setMainRenderTarget(RenderTarget target);
}
