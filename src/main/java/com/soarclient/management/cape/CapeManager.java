package com.soarclient.management.cape;

import com.mojang.blaze3d.platform.NativeImage;
import com.soarclient.skia.Skia;
import com.soarclient.skia.image.ImageHelper;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class CapeManager implements Closeable {
    private static CapeManager instance;

    private final Map<String, Identifier> loadedCapes = Collections.synchronizedMap(new HashMap<>());
    private final Map<Identifier, DynamicTexture> loadedCapeTextures = Collections.synchronizedMap(new HashMap<>());

    private String selectedCapeId = null;
    private volatile boolean closed;

    private final String namespace = "soar-capes";
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public CapeManager() {
        instance = this;
    }

    public static CapeManager getInstance() {
        return instance;
    }

    public void selectCape(String capeId) {
        this.selectedCapeId = capeId;
    }

    public String getSelectedCapeId() {
        return selectedCapeId;
    }

    public Identifier getSelectedCapeTexture() {
        if (selectedCapeId == null) return null;
        return getLoadedCape(selectedCapeId);
    }

    public void clearSelectedCape() {
        this.selectedCapeId = null;
    }

    public void loadCape(String id, byte[] textureData) {
        if (closed || id == null || textureData == null) return;

        executorService.submit(() -> {
            NativeImage pixels;
            try {
                pixels = NativeImage.read(textureData);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }

            if (closed) {
                pixels.close();
                return;
            }

            Minecraft.getInstance().execute(() -> {
                if (closed) {
                    pixels.close();
                    return;
                }
                DynamicTexture nativeImage = new DynamicTexture(() -> "Soar cape " + id, pixels);
                Identifier identifier = Identifier.fromNamespaceAndPath("soar", namespace + "/" + id);
                Skia.getImageHelper().put(identifier, ImageHelper.nativeImageToSkijaImage(pixels));
                Minecraft.getInstance().getTextureManager().register(identifier, nativeImage);
                loadedCapes.put(id, identifier);
                loadedCapeTextures.put(identifier, nativeImage);
            });
        });
    }

    public void unloadCape(String id) {
        if (id == null) return;

        if (id.equals(selectedCapeId)) {
            selectedCapeId = null;
        }

        Identifier cape = loadedCapes.remove(id);
        if (cape != null) {
            loadedCapeTextures.remove(cape);
            Skia.getImageHelper().remove(cape);
            Minecraft.getInstance().getTextureManager().release(cape);
        }
    }

    public Identifier getLoadedCape(String id) {
        return id != null ? loadedCapes.get(id) : null;
    }

    public Set<String> getLoadedCapeIds() {
        return new HashSet<>(loadedCapes.keySet());
    }

    @Override
    public void close() {
        closed = true;
        selectedCapeId = null;
        new HashMap<>(loadedCapes).keySet().forEach(this::unloadCape);
        executorService.shutdown();
    }
}
