package com.soarclient.utils.mouse;

import com.soarclient.animation.SimpleAnimation;

public class ScrollHelper {

	private SimpleAnimation animation = new SimpleAnimation();
	private float scroll, maxScroll, minScroll;

	public ScrollHelper() {
		maxScroll = Float.MAX_VALUE;
		minScroll = 0;
	}

	public void onScroll(double amount) {
		scroll += amount * 60;
	}

	public void onUpdate() {
		animation.onTick(scroll, 16);
		scroll = Math.max(Math.min(minScroll, scroll), -maxScroll);
	}

	public float getValue() {
		return animation.getValue();
	}

	public void setMaxScroll(float itemHeight, float itemSpace, int itemSize, int row, float height) {

		int totalItems = itemSize;
		int itemsPerRow = row;
		int rows = (int) Math.ceil((double) totalItems / itemsPerRow);
		float totalHeight = rows * (itemHeight + itemSpace);
		float viewportHeight = height;
		float maxScroll = Math.max(0, totalHeight - viewportHeight);

		this.maxScroll = maxScroll;
	}

	public void setMaxScroll(float totalHeight, float height) {

		float maxScroll = Math.max(0, totalHeight - height);

		this.maxScroll = maxScroll;
	}

	public void reset() {
		this.scroll = 0;
	}
}
