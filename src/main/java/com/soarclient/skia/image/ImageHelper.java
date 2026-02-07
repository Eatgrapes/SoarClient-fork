package com.soarclient.skia.image;

import com.soarclient.skia.context.SkiaContext;
import com.soarclient.skia.utils.SkiaUtils;
import com.soarclient.utils.ImageUtils;
import io.github.humbleui.skija.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.DynamicTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ImageHelper {

    private final Map<String, Image> images = new HashMap<>();
    private final Map<Integer, Image> textures = new HashMap<>();

    public boolean load(int texture, float width, float height, SurfaceOrigin origin) {

        if (!textures.containsKey(texture)) {
            Image image = Image.adoptGLTextureFrom(SkiaContext.getContext(), texture, GL11.GL_TEXTURE_2D, (int) width,
                (int) height, GL11.GL_RGBA8, origin, ColorType.RGBA_8888);
            textures.put(texture, image);
        }

        return true;
    }

    public boolean load(Identifier identifier) {
        if (images.containsKey(identifier.getPath())) return true;

        var mc = MinecraftClient.getInstance();
        var texture = mc.getTextureManager().getTexture(identifier);

        if (texture instanceof NativeImageBackedTexture nbt) {
            var ni = nbt.getImage();
            if (ni != null) {
                images.put(identifier.getPath(), nativeImageToSkijaImage(ni));
                return true;
            }
        } else {
            try {
                var resource = mc.getResourceManager().getResource(identifier);
                if (resource.isPresent()) {
                    try (InputStream is = resource.get().getInputStream()) {
                        images.put(identifier.getPath(), Image.makeDeferredFromEncodedBytes(is.readAllBytes()));
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    public boolean load(String filePath) {
        if (!images.containsKey(filePath)) {
            Optional<byte[]> encodedBytes = SkiaUtils.convertToBytes(filePath);
            if (encodedBytes.isPresent()) {
                Image image = Image.makeDeferredFromEncodedBytes(encodedBytes.get());
                images.put(filePath, image);
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean load(File file) {

        if (!images.containsKey(file.getName())) {

            try {
                byte[] encoded = org.apache.commons.io.IOUtils.toByteArray(new FileInputStream(file));
                Image image = Image.makeDeferredFromEncodedBytes(encoded);
                images.put(file.getName(), image);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    public Image get(String path) {

        if (images.containsKey(path)) {
            return images.get(path);
        }

        return null;
    }

    public Image get(int texture) {

        if (textures.containsKey(texture)) {
            return textures.get(texture);
        }

        return null;
    }

    public static Image nativeImageToSkijaImage(NativeImage nativeImage) {
        int[] pixels = nativeImage.copyPixelsAbgr();

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(pixels.length * 4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.asIntBuffer().put(pixels);

        byte[] byteArray = new byte[byteBuffer.remaining()];
        byteBuffer.get(byteArray);

        ImageInfo info = new ImageInfo(
            nativeImage.getWidth(),
            nativeImage.getHeight(),
            ColorType.RGBA_8888,
            ColorAlphaType.PREMUL
        );

        return Image.makeRasterFromBytes(info, byteArray, nativeImage.getWidth() * 4L);
    }
}
