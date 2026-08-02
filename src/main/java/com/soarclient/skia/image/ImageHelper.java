package com.soarclient.skia.image;

import com.mojang.blaze3d.platform.NativeImage;
import com.soarclient.skia.utils.SkiaUtils;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class ImageHelper implements AutoCloseable {

	private final Map<String, Image> images = new HashMap<>();

	public boolean load(Identifier identifier) {
		String key = identifier.toString();
		if (images.containsKey(key)) {
			return true;
		}

		Minecraft minecraft = Minecraft.getInstance();
		var texture = minecraft.getTextureManager().getTexture(identifier);
		if (texture instanceof DynamicTexture dynamicTexture) {
			NativeImage pixels = dynamicTexture.getPixels();
			if (pixels != null) {
				images.put(key, nativeImageToSkijaImage(pixels));
				return true;
			}
		}

		try {
			var resource = minecraft.getResourceManager().getResource(identifier);
			if (resource.isPresent()) {
				try (InputStream stream = resource.get().open()) {
					images.put(key, Image.makeDeferredFromEncodedBytes(stream.readAllBytes()));
					return true;
				}
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	public boolean load(String filePath) {
		if (!images.containsKey(filePath)) {
			Optional<byte[]> encodedBytes = SkiaUtils.convertToBytes(filePath);
			if (encodedBytes.isEmpty()) {
				return false;
			}
			images.put(filePath, Image.makeDeferredFromEncodedBytes(encodedBytes.get()));
		}
		return true;
	}

	public boolean load(File file) {
		String key = file.getAbsolutePath();
		if (!images.containsKey(key)) {
			try (FileInputStream stream = new FileInputStream(file)) {
				images.put(key, Image.makeDeferredFromEncodedBytes(stream.readAllBytes()));
			} catch (IOException exception) {
				return false;
			}
		}
		return true;
	}

	public Image get(String path) {
		return images.get(path);
	}

	public Image get(Identifier identifier) {
		return images.get(identifier.toString());
	}

	public Image get(File file) {
		return images.get(file.getAbsolutePath());
	}

	public void put(Identifier identifier, Image image) {
		Image previous = images.put(identifier.toString(), image);
		if (previous != null) {
			previous.close();
		}
	}

	public void remove(Identifier identifier) {
		Image image = images.remove(identifier.toString());
		if (image != null) {
			image.close();
		}
	}

	@Override
	public void close() {
		images.values().forEach(Image::close);
		images.clear();
	}

	public static Image nativeImageToSkijaImage(NativeImage nativeImage) {
		int[] pixels = nativeImage.getPixelsABGR();
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(pixels.length * 4).order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.asIntBuffer().put(pixels);
		byte[] byteArray = new byte[pixels.length * 4];
		byteBuffer.position(0);
		byteBuffer.get(byteArray);
		ImageInfo info = new ImageInfo(nativeImage.getWidth(), nativeImage.getHeight(), ColorType.RGBA_8888, ColorAlphaType.UNPREMUL);
		return Image.makeRasterFromBytes(info, byteArray, nativeImage.getWidth() * 4L);
	}
}
