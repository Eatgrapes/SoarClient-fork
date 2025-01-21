package net.caffeinemc.mods.sodium.api.vertex.serializer;

import net.caffeinemc.mods.sodium.api.internal.DependencyInjection;
import net.minecraft.client.renderer.vertex.VertexFormat;

public interface VertexSerializerRegistry {
    VertexSerializerRegistry INSTANCE = DependencyInjection.load(VertexSerializerRegistry.class,
            "net.caffeinemc.mods.sodium.client.render.vertex.serializers.VertexSerializerRegistryImpl");

    static VertexSerializerRegistry instance() {
        return INSTANCE;
    }

    VertexSerializer get(VertexFormat srcFormat, VertexFormat dstFormat);

    void registerSerializer(VertexFormat srcFormat, VertexFormat dstFormat, VertexSerializer serializer);
}
