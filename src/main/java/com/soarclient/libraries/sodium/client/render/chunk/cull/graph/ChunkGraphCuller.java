package com.soarclient.libraries.sodium.client.render.chunk.cull.graph;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.soarclient.libraries.sodium.client.render.chunk.cull.ChunkCuller;
import com.soarclient.libraries.sodium.client.util.math.ChunkSectionPos;
import com.soarclient.libraries.sodium.client.util.math.FrustumExtended;
import com.soarclient.libraries.sodium.common.util.DirectionUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class ChunkGraphCuller implements ChunkCuller {
	private final Long2ObjectMap<ChunkGraphNode> nodes = new Long2ObjectOpenHashMap<>();
	private final ChunkGraphIterationQueue visible = new ChunkGraphIterationQueue();
	private final World world;
	private final int renderDistance;
	private FrustumExtended frustum;
	private boolean useOcclusionCulling;
	private int activeFrame = 0;
	private int centerChunkX;
	private int centerChunkY;
	private int centerChunkZ;

	public ChunkGraphCuller(World world, int renderDistance) {
		this.world = world;
		this.renderDistance = renderDistance;
	}

	@Override
	public IntArrayList computeVisible(Vec3 cameraPos, FrustumExtended frustum, int frame, boolean spectator) {
		this.initSearch(cameraPos, frustum, frame, spectator);
		ChunkGraphIterationQueue queue = this.visible;

		for (int i = 0; i < queue.size(); i++) {
			ChunkGraphNode node = queue.getNode(i);
			short cullData = node.computeQueuePop();

			for (EnumFacing dir : DirectionUtil.ALL_DIRECTIONS) {
				if (!this.useOcclusionCulling || (cullData & 1 << dir.ordinal()) != 0) {
					ChunkGraphNode adj = node.getConnectedNode(dir);
					if (adj != null && this.isWithinRenderDistance(adj)) {
						this.bfsEnqueue(node, adj, dir.getOpposite(), cullData);
					}
				}
			}
		}

		return this.visible.getOrderedIdList();
	}

	private boolean isWithinRenderDistance(ChunkGraphNode adj) {
		int x = Math.abs(adj.getChunkX() - this.centerChunkX);
		int z = Math.abs(adj.getChunkZ() - this.centerChunkZ);
		return x <= this.renderDistance && z <= this.renderDistance;
	}

	private void initSearch(Vec3 cameraPos, FrustumExtended frustum, int frame, boolean spectator) {
		this.activeFrame = frame;
		this.frustum = frustum;
		this.useOcclusionCulling = Minecraft.getMinecraft().renderChunksMany;
		this.visible.clear();
		BlockPos origin = new BlockPos(cameraPos.xCoord, cameraPos.yCoord, cameraPos.zCoord);
		int chunkX = origin.getX() >> 4;
		int chunkY = origin.getY() >> 4;
		int chunkZ = origin.getZ() >> 4;
		this.centerChunkX = chunkX;
		this.centerChunkY = chunkY;
		this.centerChunkZ = chunkZ;
		ChunkGraphNode rootNode = this.getNode(chunkX, chunkY, chunkZ);
		if (rootNode != null) {
			rootNode.resetCullingState();
			rootNode.setLastVisibleFrame(frame);
			if (spectator && this.world.getBlockState(origin).getBlock().isOpaqueCube()) {
				this.useOcclusionCulling = false;
			}

			this.visible.add(rootNode);
		} else {
			chunkY = MathHelper.clamp_int(origin.getY() >> 4, 0, 15);
			List<ChunkGraphNode> bestNodes = new ArrayList();

			for (int x = -this.renderDistance; x <= this.renderDistance; x++) {
				for (int z = -this.renderDistance; z <= this.renderDistance; z++) {
					ChunkGraphNode node = this.getNode(chunkX + x, chunkY, chunkZ + z);
					if (node != null && !node.isCulledByFrustum(frustum)) {
						node.resetCullingState();
						node.setLastVisibleFrame(frame);
						bestNodes.add(node);
					}
				}
			}

			bestNodes.sort(Comparator.comparingDouble(nodex -> nodex.getSquaredDistance(origin)));

			for (ChunkGraphNode node : bestNodes) {
				this.visible.add(node);
			}
		}
	}

	private void bfsEnqueue(ChunkGraphNode parent, ChunkGraphNode node, EnumFacing flow, short parentalData) {
		if (node.getLastVisibleFrame() == this.activeFrame) {
			node.updateCullingState(flow, parentalData);
		} else {
			node.setLastVisibleFrame(this.activeFrame);
			if (!node.isCulledByFrustum(this.frustum)) {
				node.setCullingState(parentalData);
				node.updateCullingState(flow, parentalData);
				this.visible.add(node);
			}
		}
	}

	private void connectNeighborNodes(ChunkGraphNode node) {
		for (EnumFacing dir : DirectionUtil.ALL_DIRECTIONS) {
			ChunkGraphNode adj = this.findAdjacentNode(node, dir);
			if (adj != null) {
				adj.setAdjacentNode(dir.getOpposite(), node);
			}

			node.setAdjacentNode(dir, adj);
		}
	}

	private void disconnectNeighborNodes(ChunkGraphNode node) {
		for (EnumFacing dir : DirectionUtil.ALL_DIRECTIONS) {
			ChunkGraphNode adj = node.getConnectedNode(dir);
			if (adj != null) {
				adj.setAdjacentNode(dir.getOpposite(), null);
			}

			node.setAdjacentNode(dir, null);
		}
	}

	private ChunkGraphNode findAdjacentNode(ChunkGraphNode node, EnumFacing dir) {
		return this.getNode(node.getChunkX() + dir.getFrontOffsetX(), node.getChunkY() + dir.getFrontOffsetY(), node.getChunkZ() + dir.getFrontOffsetZ());
	}

	private ChunkGraphNode getNode(int x, int y, int z) {
		return this.nodes.get(ChunkSectionPos.asLong(x, y, z));
	}

	@Override
	public void onSectionStateChanged(int x, int y, int z, SetVisibility occlusionData) {
		ChunkGraphNode node = this.getNode(x, y, z);
		if (node != null) {
			node.setOcclusionData(occlusionData);
		}
	}

	@Override
	public void onSectionLoaded(int x, int y, int z, int id) {
		ChunkGraphNode node = new ChunkGraphNode(x, y, z, id);
		ChunkGraphNode prev;
		if ((prev = this.nodes.put(ChunkSectionPos.asLong(x, y, z), node)) != null) {
			this.disconnectNeighborNodes(prev);
		}

		this.connectNeighborNodes(node);
	}

	@Override
	public void onSectionUnloaded(int x, int y, int z) {
		ChunkGraphNode node = this.nodes.remove(ChunkSectionPos.asLong(x, y, z));
		if (node != null) {
			this.disconnectNeighborNodes(node);
		}
	}

	@Override
	public boolean isSectionVisible(int x, int y, int z) {
		ChunkGraphNode render = this.getNode(x, y, z);
		return render == null ? false : render.getLastVisibleFrame() == this.activeFrame;
	}
}
