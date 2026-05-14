package com.astreonix.watcher.util;

import com.astreonix.watcher.record.RecorderManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeybindHandler {
    private KeybindHandler() {
    }

    public static void register(RecorderManager recorder) {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("watcher", "watcher"));
        KeyBinding toggleRecording = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.watcher.toggle_recording",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                category
        ));

        KeyBinding pauseResume = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.watcher.pause_resume",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F10,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleRecording.wasPressed()) {
                recorder.toggleRecording();
            }
            while (pauseResume.wasPressed()) {
                recorder.togglePause();
            }
        });
    }
}
