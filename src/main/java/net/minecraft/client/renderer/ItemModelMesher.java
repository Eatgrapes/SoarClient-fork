package net.minecraft.client.renderer;

import java.util.Map;

import com.google.common.collect.Maps;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.src.Config;
import net.optifine.CustomItems;

public class ItemModelMesher {
	private final Int2ObjectOpenHashMap<ModelResourceLocation> simpleShapes = new Int2ObjectOpenHashMap<>();
	private final Int2ObjectOpenHashMap<IBakedModel> simpleShapesCache = new Int2ObjectOpenHashMap<>();
	private final Map<Item, ItemMeshDefinition> shapers = Maps.newHashMap();
	private final ModelManager modelManager;

	public ItemModelMesher(ModelManager modelManager) {
		this.modelManager = modelManager;
	}

	public TextureAtlasSprite getParticleIcon(Item item) {
		return this.getParticleIcon(item, 0);
	}

	public TextureAtlasSprite getParticleIcon(Item item, int meta) {
		return this.getItemModel(new ItemStack(item, 1, meta)).getParticleTexture();
	}

	public IBakedModel getItemModel(ItemStack stack) {
		Item item = stack.getItem();
		IBakedModel ibakedmodel = this.getItemModel(item, this.getMetadata(stack));

		if (ibakedmodel == null) {
			ItemMeshDefinition itemmeshdefinition = this.shapers.get(item);

			if (itemmeshdefinition != null) {
				ibakedmodel = this.modelManager.getModel(itemmeshdefinition.getModelLocation(stack));
			}
		}

		if (ibakedmodel == null) {
			ibakedmodel = this.modelManager.getMissingModel();
		}

		if (Config.isCustomItems()) {
			ibakedmodel = CustomItems.getCustomItemModel(stack, ibakedmodel, null, true);
		}

		return ibakedmodel;
	}

	protected int getMetadata(ItemStack stack) {
		return stack.isItemStackDamageable() ? 0 : stack.getMetadata();
	}

	protected IBakedModel getItemModel(Item item, int meta) {
		return this.simpleShapesCache.get(this.getIndex(item, meta));
	}

	private int getIndex(Item item, int meta) {
		return Item.getIdFromItem(item) << 16 | meta;
	}

	public void register(Item item, int meta, ModelResourceLocation location) {
		this.simpleShapes.put(this.getIndex(item, meta), location);
		this.simpleShapesCache.put(this.getIndex(item, meta), this.modelManager.getModel(location));
	}

	public void register(Item item, ItemMeshDefinition definition) {
		this.shapers.put(item, definition);
	}

	public ModelManager getModelManager() {
		return this.modelManager;
	}

	public void rebuildCache() {
		this.simpleShapesCache.clear();

		for (Int2ObjectMap.Entry<ModelResourceLocation> entry : this.simpleShapes.int2ObjectEntrySet()) {
		    this.simpleShapesCache.put(entry.getIntKey(), this.modelManager.getModel(entry.getValue()));
		}
	}
}
