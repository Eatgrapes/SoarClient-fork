package com.soarclient.management.cape;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CapeManager implements Closeable {
    private static CapeManager instance;

    private final Map<String, Identifier> loadedCapes = Collections.synchronizedMap(new HashMap<>());
    private final Map<Identifier, NativeImageBackedTexture> loadedCapeTextures = Collections.synchronizedMap(new HashMap<>());

    private String selectedCapeId = null;

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
        if (id == null || textureData == null) return;

        executorService.submit(() -> {
            RenderSystem.recordRenderCall(() -> {
                NativeImageBackedTexture nativeImage = createNativeTexture(textureData);
                if (nativeImage != null) {
                    Identifier identifier = Identifier.of("soar", namespace + "/" + id);
                    MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, nativeImage);
                    loadedCapes.put(id, identifier);
                    loadedCapeTextures.put(identifier, nativeImage);
                }
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
            NativeImageBackedTexture texture = loadedCapeTextures.remove(cape);
            if (texture != null) {
                texture.close();
            }
            MinecraftClient.getInstance().getTextureManager().destroyTexture(cape);
        }
    }

    public Identifier getLoadedCape(String id) {
        return id != null ? loadedCapes.get(id) : null;
    }

    public Set<String> getLoadedCapeIds() {
        return new HashSet<>(loadedCapes.keySet());
    }

    private static NativeImageBackedTexture createNativeTexture(byte[] bytes) {
        if (bytes == null) return null;
        try {
            return new NativeImageBackedTexture(NativeImage.read(bytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        selectedCapeId = null;
        new HashMap<>(loadedCapes).keySet().forEach(this::unloadCape);
        executorService.shutdown();
    }
}
