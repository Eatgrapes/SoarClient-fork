package com.soarclient.mixin.mixins.minecraft.client;


import com.ibm.icu.impl.data.ResourceReader;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourcePack;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(Window.class)
public abstract class MixinWindow {
    @Shadow
    public abstract long getHandle();

    @Inject(method = "setIcon", at = @At(value = "HEAD"), cancellable = true)
    public void onSetIcon(ResourcePack resourcePack, Icons icons, CallbackInfo ci) {

        String path = "assets/soar/logo.dark.png";
        try (InputStream inputStream = ResourceReader.class.getClassLoader().getResourceAsStream(path)) {
            setWindowIcon(getHandle(), inputStream);
            ci.cancel();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 我参考了 <a href="https://github.com/chylex/Minecraft-Window-Title/">Minecraft-Window-Title</a>的代码
     * 感谢作者让我能在高版本改Icon(
     */
    @Unique
    private static void setWindowIcon(long windowHandle, InputStream iconPath) {
        ByteBuffer icon = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            icon = loadIcon(iconPath, w, h, channels);
            if (icon == null) {
                return;
            }

            try (GLFWImage.Buffer icons = GLFWImage.malloc(1)) {
                GLFWImage iconImage = icons.get(0);
                iconImage.set(w.get(0), h.get(0), icon);

                GLFW.glfwSetWindowIcon(windowHandle, icons);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (icon != null) {
                STBImage.stbi_image_free(icon);
            }
        }
    }

    @Unique
    private static ByteBuffer loadIcon(InputStream path, IntBuffer w, IntBuffer h, IntBuffer channels) throws IOException {
        byte[] iconBytes = path.readAllBytes();

        ByteBuffer buffer = ByteBuffer.allocateDirect(iconBytes.length).put(iconBytes).flip();
        ByteBuffer icon = STBImage.stbi_load_from_memory(buffer, w, h, channels, 4);

        if (icon == null) {
            System.out.println("Failed to load image from path: " + "path" + " - " + STBImage.stbi_failure_reason());
        }
        return icon;
    }//感谢来自 Minecraft-Window-Title的代码


}
