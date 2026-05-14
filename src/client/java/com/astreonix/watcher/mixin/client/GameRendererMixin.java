package com.astreonix.watcher.mixin.client;

import com.astreonix.watcher.WatcherClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void watcher$captureFinalFrame(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null || WatcherClient.recorder() == null) {
            return;
        }

        WatcherClient.recorder().captureRenderedFrame(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight());
    }
}
