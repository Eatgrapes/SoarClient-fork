package com.soarclient.libs.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;

import java.util.concurrent.TimeUnit;

import com.soarclient.libs.sodium.client.util.math.ChunkSectionPos;

import net.minecraft.world.World;

public class ClonedChunkSectionCache {
	private static final int MAX_CACHE_SIZE = 512;
	private static final long MAX_CACHE_DURATION = TimeUnit.SECONDS.toNanos(5L);
	private final World world;
	private final Long2ReferenceLinkedOpenHashMap<ClonedChunkSection> byPosition = new Long2ReferenceLinkedOpenHashMap<>();
	private long time;

	public ClonedChunkSectionCache(World context) {
		this.world = context;
		this.time = getMonotonicTimeSource();
	}

	public synchronized void cleanup() {
		this.time = getMonotonicTimeSource();
		this.byPosition.values().removeIf(entry -> this.time > entry.getLastUsedTimestamp() + MAX_CACHE_DURATION);
	}

	public synchronized ClonedChunkSection acquire(int x, int y, int z) {
		long key = ChunkSectionPos.asLong(x, y, z);
		ClonedChunkSection section = this.byPosition.get(key);
		if (section == null) {
			while (this.byPosition.size() >= 512) {
				this.byPosition.removeFirst();
			}

			section = this.createSection(x, y, z);
		}

		section.setLastUsedTimestamp(this.time);
		return section;
	}

	private ClonedChunkSection createSection(int x, int y, int z) {
		ClonedChunkSection section = this.allocate();
		ChunkSectionPos pos = ChunkSectionPos.from(x, y, z);
		section.init(pos);
		this.byPosition.putAndMoveToLast(pos.asLong(), section);
		return section;
	}

	public synchronized void invalidate(int x, int y, int z) {
		this.byPosition.remove(ChunkSectionPos.asLong(x, y, z));
	}

	public void release(ClonedChunkSection section) {
	}

	private ClonedChunkSection allocate() {
		return new ClonedChunkSection(this, this.world);
	}

	private static long getMonotonicTimeSource() {
		return System.nanoTime();
	}
}
