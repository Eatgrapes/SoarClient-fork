package com.soarclient.skia.context;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vulkan.VulkanCommandEncoder;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;
import com.soarclient.logger.SoarLogger;
import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.BackendTexture;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.GLTextureInfo;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.RuntimeEffect;
import io.github.humbleui.skija.RuntimeEffectBuilder;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.skija.VkImageInfo;
import io.github.humbleui.skija.VulkanAlloc;
import io.github.humbleui.types.Rect;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.ARBClipControl;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageSubresourceRange;

public final class SkiaContext {

	private static final Queue<Consumer<Canvas>> pendingDraws = new ArrayDeque<>();
	private static final Blur blur = new Blur();
	private static Backend backend;
	private static GpuDevice device;
	private static Canvas canvas;
	private static Image frameSnapshot;
	private static Image frameBackdrop;
	private static boolean frameActive;
	private static boolean unavailable;

	private SkiaContext() {
	}

	public static void queue(Consumer<Canvas> drawingLogic) {
		pendingDraws.add(drawingLogic);
	}

	public static void draw(Consumer<Canvas> drawingLogic) {
		if (frameActive && canvas != null) {
			drawingLogic.accept(canvas);
		} else {
			queue(drawingLogic);
		}
	}

	public static boolean beginFrame() {
		RenderSystem.assertOnRenderThread();
		if (unavailable || frameActive) {
			return false;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gameRenderer == null) {
			return false;
		}

		GpuDevice currentDevice = RenderSystem.getDevice();
		if (device != currentDevice) {
			close();
			device = currentDevice;
			backend = createBackend(currentDevice);
			if (backend == null) {
				unavailable = true;
				return false;
			}
			SoarLogger.info("Skija", "Using " + currentDevice.getDeviceInfo().backendName() + " backend");
		}

		RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
		try {
			canvas = backend.beginFrame(target);
			frameActive = canvas != null;
			if (frameActive) {
				frameSnapshot = backend.snapshot();
				frameBackdrop = backend.backdrop();
			}
			return frameActive;
		} catch (Exception exception) {
			SoarLogger.error("Skija", "Failed to begin frame", exception);
			close();
			unavailable = true;
			return false;
		}
	}

	public static void discardPending() {
		pendingDraws.clear();
	}

	public static void drawPending() {
		if (!frameActive && !beginFrame()) {
			discardPending();
			return;
		}

		Consumer<Canvas> draw;
		while ((draw = pendingDraws.poll()) != null) {
			draw.accept(canvas);
		}
	}

	public static void endFrame() {
		if (!frameActive || backend == null) {
			return;
		}

		try {
			backend.endFrame();
		} catch (Exception exception) {
			SoarLogger.error("Skija", "Failed to end frame", exception);
			close();
			unavailable = true;
		} finally {
			blur.endFrame();
			closeFrameSnapshot();
			canvas = null;
			frameActive = false;
		}
	}

	public static Canvas getCanvas() {
		return canvas;
	}

	public static DirectContext getContext() {
		return backend != null ? backend.context() : null;
	}

	public static Image getBlurredFrame(float requestedStrength) {
		if (!frameActive || frameBackdrop == null || backend == null) {
			return null;
		}
		return blur.get(backend.context(), frameBackdrop, Math.round(requestedStrength));
	}

	public static void invalidate() {
		if (backend != null) {
			backend.invalidate();
		}
		unavailable = false;
	}

	public static void close() {
		pendingDraws.clear();
		blur.close();
		closeFrameSnapshot();
		canvas = null;
		frameActive = false;
		if (backend != null) {
			backend.close();
			backend = null;
		}
		device = null;
		unavailable = false;
	}

	private static void closeFrameSnapshot() {
		frameBackdrop = null;
		if (frameSnapshot != null) {
			frameSnapshot.close();
			frameSnapshot = null;
		}
	}

	private static Backend createBackend(GpuDevice device) {
		if ("Vulkan".equals(device.getDeviceInfo().backendName())) {
			if (device.backend instanceof VulkanDevice vulkanDevice) {
				return new VulkanBackend(vulkanDevice);
			}
			return null;
		}
		return new GlBackend();
	}

	private interface Backend extends AutoCloseable {

		Canvas beginFrame(RenderTarget target);

		void endFrame();

		DirectContext context();

		Image snapshot();

		Image backdrop();

		void invalidate();

		@Override
		void close();
	}

	private static final class GlBackend implements Backend {

		private DirectContext context;
		private BackendRenderTarget renderTarget;
		private BackendTexture backdropTexture;
		private Image backdropImage;
		private Surface surface;
		private int framebuffer;
		private int textureId;
		private int width;
		private int height;
		private int previousReadFramebuffer;
		private int previousDrawFramebuffer;
		private int previousProgram;
		private int previousVertexArray;
		private int previousArrayBuffer;
		private int previousElementArrayBuffer;
		private int previousUniformBuffer;
		private int previousActiveTexture;
		private final int[] previousViewport = new int[4];
		private final int[] previousScissorBox = new int[4];
		private boolean previousBlendEnabled;
		private boolean previousDepthTestEnabled;
		private boolean previousScissorEnabled;
		private boolean previousStencilEnabled;
		private boolean previousCullEnabled;
		private boolean previousDepthMask;
		private final boolean[][] previousColorMasks = new boolean[8][4];
		private int previousUnpackAlignment;
		private int previousUnpackRowLength;
		private int previousUnpackSkipPixels;
		private int previousUnpackSkipRows;
		private int previousClipOrigin;
		private int previousClipDepthMode;
		private boolean clipControlChanged;
		private final int[] previousTextureBindings = new int[12];
		private final int[] previousSamplerBindings = new int[12];
		private boolean stateCaptured;
		private boolean invalid = true;

		@Override
		public Canvas beginFrame(RenderTarget target) {
			GpuTexture colorTexture = target.getColorTexture();
			if (!(colorTexture instanceof GlTexture texture) || target.width <= 0 || target.height <= 0) {
				return null;
			}

			if (this.context == null) {
				this.context = DirectContext.makeGL();
			}

			if (this.invalid || this.surface == null || this.textureId != texture.glId()
					|| this.width != target.width || this.height != target.height) {
				this.recreate(texture.glId(), target.width, target.height);
			}

			this.captureState();
			try {
				if (GL.getCapabilities().GL_ARB_clip_control) {
					ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_NEGATIVE_ONE_TO_ONE);
					this.clipControlChanged = true;
				}
				GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebuffer);
				this.context.resetGLAll();
				return this.surface.getCanvas();
			} catch (RuntimeException | Error throwable) {
				this.restoreState();
				throw throwable;
			}
		}

		@Override
		public void endFrame() {
			if (this.context == null || this.surface == null) {
				return;
			}

			try {
				this.context.flushAndSubmit(this.surface, false);
			} finally {
				this.restoreState();
			}
		}

		@Override
		public DirectContext context() {
			return this.context;
		}

		@Override
		public Image snapshot() {
			return this.surface != null ? this.surface.makeImageSnapshot() : null;
		}

		@Override
		public Image backdrop() {
			return this.backdropImage;
		}

		@Override
		public void invalidate() {
			this.invalid = true;
		}

		@Override
		public void close() {
			this.restoreState();
			this.closeTarget();
			if (this.context != null) {
				this.context.close();
				this.context = null;
			}
		}

		private void captureState() {
			this.previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
			this.previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
			this.previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
			this.previousVertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
			this.previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
			this.previousElementArrayBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
			this.previousUniformBuffer = GL11.glGetInteger(GL31.GL_UNIFORM_BUFFER_BINDING);
			this.previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
			GL11.glGetIntegerv(GL11.GL_VIEWPORT, this.previousViewport);
			GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, this.previousScissorBox);
			this.previousBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
			this.previousDepthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
			this.previousScissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
			this.previousStencilEnabled = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
			this.previousCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
			this.previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
			ByteBuffer colorMask = ByteBuffer.allocateDirect(4);
			for (int i = 0; i < this.previousColorMasks.length; i++) {
				colorMask.clear();
				GL30.glGetBooleani_v(GL11.GL_COLOR_WRITEMASK, i, colorMask);
				for (int channel = 0; channel < 4; channel++) {
					this.previousColorMasks[i][channel] = colorMask.get(channel) != 0;
				}
			}
			this.previousUnpackAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
			this.previousUnpackRowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
			this.previousUnpackSkipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
			this.previousUnpackSkipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
			if (GL.getCapabilities().GL_ARB_clip_control) {
				this.previousClipOrigin = GL11.glGetInteger(ARBClipControl.GL_CLIP_ORIGIN);
				this.previousClipDepthMode = GL11.glGetInteger(ARBClipControl.GL_CLIP_DEPTH_MODE);
			}
			for (int i = 0; i < this.previousTextureBindings.length; i++) {
				GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
				this.previousTextureBindings[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
				this.previousSamplerBindings[i] = GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, i);
			}
			GL13.glActiveTexture(this.previousActiveTexture);
			this.stateCaptured = true;
		}

		private void restoreState() {
			if (!this.stateCaptured) {
				return;
			}
			for (int i = 0; i < this.previousTextureBindings.length; i++) {
				GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.previousTextureBindings[i]);
				GL33.glBindSampler(i, this.previousSamplerBindings[i]);
			}
			GL13.glActiveTexture(this.previousActiveTexture);
			GL20.glUseProgram(this.previousProgram);
			GL30.glBindVertexArray(this.previousVertexArray);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.previousArrayBuffer);
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.previousElementArrayBuffer);
			GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, this.previousUniformBuffer);
			GL11.glViewport(this.previousViewport[0], this.previousViewport[1], this.previousViewport[2], this.previousViewport[3]);
			GL11.glScissor(this.previousScissorBox[0], this.previousScissorBox[1], this.previousScissorBox[2], this.previousScissorBox[3]);
			this.setEnabled(GL11.GL_BLEND, this.previousBlendEnabled);
			this.setEnabled(GL11.GL_DEPTH_TEST, this.previousDepthTestEnabled);
			this.setEnabled(GL11.GL_SCISSOR_TEST, this.previousScissorEnabled);
			this.setEnabled(GL11.GL_STENCIL_TEST, this.previousStencilEnabled);
			this.setEnabled(GL11.GL_CULL_FACE, this.previousCullEnabled);
			GL11.glDepthMask(this.previousDepthMask);
			for (int i = 0; i < this.previousColorMasks.length; i++) {
				boolean[] mask = this.previousColorMasks[i];
				GL30.glColorMaski(i, mask[0], mask[1], mask[2], mask[3]);
			}
			GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, this.previousUnpackAlignment);
			GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, this.previousUnpackRowLength);
			GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, this.previousUnpackSkipPixels);
			GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, this.previousUnpackSkipRows);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.previousReadFramebuffer);
			GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.previousDrawFramebuffer);
			if (this.clipControlChanged) {
				ARBClipControl.glClipControl(this.previousClipOrigin, this.previousClipDepthMode);
				this.clipControlChanged = false;
			}
			this.stateCaptured = false;
		}

		private void setEnabled(int capability, boolean enabled) {
			if (enabled) {
				GL11.glEnable(capability);
			} else {
				GL11.glDisable(capability);
			}
		}

		private void recreate(int textureId, int width, int height) {
			this.closeTarget();
			this.framebuffer = GL30.glGenFramebuffers();
			GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebuffer);
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textureId, 0);
			int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
			if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
				this.closeTarget();
				throw new IllegalStateException("Incomplete Skija framebuffer: 0x" + Integer.toHexString(status));
			}

			this.renderTarget = BackendRenderTarget.makeGL(width, height, 1, 0, this.framebuffer, GL11.GL_RGBA8);
			this.surface = Surface.wrapBackendRenderTarget(
					this.context, this.renderTarget, SurfaceOrigin.BOTTOM_LEFT, ColorType.RGBA_8888, ColorSpace.getSRGB());
			this.backdropTexture = BackendTexture.makeGL(width, height, false,
					new GLTextureInfo(GL11.GL_TEXTURE_2D, textureId, GL11.GL_RGBA8));
			this.backdropImage = Image.borrowTextureFrom(this.context, this.backdropTexture, SurfaceOrigin.BOTTOM_LEFT,
					ColorType.RGBA_8888, ColorAlphaType.OPAQUE, ColorSpace.getSRGB(), null);
			this.textureId = textureId;
			this.width = width;
			this.height = height;
			this.invalid = false;
		}

		private void closeTarget() {
			if (this.backdropImage != null) {
				this.backdropImage.close();
				this.backdropImage = null;
			}
			if (this.backdropTexture != null) {
				this.backdropTexture.close();
				this.backdropTexture = null;
			}
			if (this.surface != null) {
				this.surface.close();
				this.surface = null;
			}
			if (this.renderTarget != null) {
				this.renderTarget.close();
				this.renderTarget = null;
			}
			if (this.framebuffer != 0) {
				GL30.glDeleteFramebuffers(this.framebuffer);
				this.framebuffer = 0;
			}
			this.textureId = 0;
		}
	}

	private static final class VulkanBackend implements Backend {

		private static final int MINECRAFT_LAYOUT = VK12.VK_IMAGE_LAYOUT_GENERAL;
		private static final int SKIJA_LAYOUT = VK12.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
		private static final int BRIDGE_STAGE = VK12.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
		private static final int BRIDGE_ACCESS = VK12.VK_ACCESS_MEMORY_READ_BIT | VK12.VK_ACCESS_MEMORY_WRITE_BIT;

		private final VulkanDevice device;
		private DirectContext context;
		private BackendRenderTarget renderTarget;
		private BackendTexture backdropTexture;
		private Image backdropImage;
		private Surface surface;
		private VulkanGpuTexture currentTexture;
		private long image;
		private int width;
		private int height;
		private int format;
		private int usage;
		private int mipLevels;
		private boolean inSkijaLayout;
		private boolean invalid = true;

		private VulkanBackend(VulkanDevice device) {
			this.device = device;
		}

		@Override
		public Canvas beginFrame(RenderTarget target) {
			if (!(target.getColorTexture() instanceof VulkanGpuTexture texture) || target.width <= 0 || target.height <= 0) {
				return null;
			}

			if (this.context == null) {
				this.context = DirectContext.makeVulkan(
						this.device.instance().vkInstance().address(),
						this.device.vkDevice().getPhysicalDevice().address(),
						this.device.vkDevice().address(),
						this.device.graphicsQueue().vkQueue().address(),
						this.device.graphicsQueue().queueFamilyIndex(),
						VK.getFunctionProvider().getFunctionAddress("vkGetInstanceProcAddr"),
						this.device.vkDevice().getCapabilities().vkGetDeviceProcAddr,
						VK12.VK_API_VERSION_1_2);
			}

			int vkFormat = VulkanConst.toVk(texture.getFormat());
			int vkUsage = VulkanConst.textureUsageToVk(texture.usage(), texture.getFormat());
			if (this.invalid || this.surface == null || this.image != texture.vkImage()
					|| this.width != target.width || this.height != target.height || this.format != vkFormat
					|| this.usage != vkUsage || this.mipLevels != texture.getMipLevels()) {
				this.recreate(texture, target.width, target.height, vkFormat, vkUsage);
			}

			this.currentTexture = texture;
			this.device.createCommandEncoder().submit();
			this.transition(texture, MINECRAFT_LAYOUT, SKIJA_LAYOUT,
					BRIDGE_STAGE, BRIDGE_ACCESS, BRIDGE_STAGE, BRIDGE_ACCESS);
			this.inSkijaLayout = true;
			return this.surface.getCanvas();
		}

		@Override
		public void endFrame() {
			if (this.context == null || this.surface == null || this.image == 0L) {
				return;
			}

			try {
				this.context.flushAndSubmit(this.surface, false);
			} finally {
				this.restoreMinecraftLayout();
			}
		}

		@Override
		public DirectContext context() {
			return this.context;
		}

		@Override
		public Image snapshot() {
			return this.surface != null ? this.surface.makeImageSnapshot() : null;
		}

		@Override
		public Image backdrop() {
			return this.backdropImage;
		}

		@Override
		public void invalidate() {
			this.invalid = true;
		}

		@Override
		public void close() {
			this.restoreMinecraftLayout();
			if (this.context != null) {
				this.context.flushAndSubmit(true);
			}
			this.device.graphicsQueue().waitIdle();
			this.closeTarget();
			if (this.context != null) {
				this.context.close();
				this.context = null;
			}
		}

		private void recreate(VulkanGpuTexture texture, int width, int height, int format, int usage) {
			this.device.graphicsQueue().waitIdle();
			this.closeTarget();
			this.renderTarget = BackendRenderTarget.makeVulkan(
					width, height, texture.vkImage(), VK12.VK_IMAGE_TILING_OPTIMAL,
					SKIJA_LAYOUT, format, usage, 1, texture.getMipLevels());
			this.surface = Surface.wrapBackendRenderTarget(
					this.context, this.renderTarget, SurfaceOrigin.BOTTOM_LEFT, ColorType.RGBA_8888, null);
			VkImageInfo imageInfo = new VkImageInfo(texture.vkImage(), new VulkanAlloc(), VK12.VK_IMAGE_TILING_OPTIMAL,
					SKIJA_LAYOUT, format, usage, 1, texture.getMipLevels(), this.device.graphicsQueue().queueFamilyIndex(), false, 0);
			this.backdropTexture = BackendTexture.makeVulkan(width, height, imageInfo);
			this.backdropImage = Image.borrowTextureFrom(this.context, this.backdropTexture, SurfaceOrigin.BOTTOM_LEFT,
					ColorType.RGBA_8888, ColorAlphaType.OPAQUE, null, null);
			this.image = texture.vkImage();
			this.width = width;
			this.height = height;
			this.format = format;
			this.usage = usage;
			this.mipLevels = texture.getMipLevels();
			this.invalid = false;
		}

		private void restoreMinecraftLayout() {
			if (!this.inSkijaLayout || this.currentTexture == null || this.currentTexture.vkImage() != this.image) {
				return;
			}
			try {
				this.transition(this.currentTexture, SKIJA_LAYOUT, MINECRAFT_LAYOUT,
						BRIDGE_STAGE, BRIDGE_ACCESS, BRIDGE_STAGE, BRIDGE_ACCESS);
			} finally {
				this.inSkijaLayout = false;
			}
		}

		private void transition(VulkanGpuTexture texture, int oldLayout, int newLayout,
				int srcStage, int srcAccess, int dstStage, int dstAccess) {
			VulkanCommandEncoder encoder = this.device.createCommandEncoder();
			VkCommandBuffer commandBuffer = encoder.allocateAndBeginTransientCommandBuffer();
			try (MemoryStack stack = MemoryStack.stackPush()) {
				VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack).sType$Default();
				barrier.oldLayout(oldLayout);
				barrier.newLayout(newLayout);
				barrier.srcAccessMask(srcAccess);
				barrier.dstAccessMask(dstAccess);
				barrier.srcQueueFamilyIndex(VK12.VK_QUEUE_FAMILY_IGNORED);
				barrier.dstQueueFamilyIndex(VK12.VK_QUEUE_FAMILY_IGNORED);
				barrier.image(texture.vkImage());
				VkImageSubresourceRange range = barrier.subresourceRange();
				range.aspectMask(VK12.VK_IMAGE_ASPECT_COLOR_BIT);
				range.baseMipLevel(0);
				range.levelCount(texture.getMipLevels());
				range.baseArrayLayer(0);
				range.layerCount(1);
				VK12.vkCmdPipelineBarrier(commandBuffer, srcStage, dstStage, 0, null, null, barrier);
			}
			int result = VK12.vkEndCommandBuffer(commandBuffer);
			if (result != VK12.VK_SUCCESS) {
				throw new IllegalStateException("Failed to end Skija layout transition command buffer: " + result);
			}
			encoder.execute(commandBuffer);
			encoder.submit();
		}

		private void closeTarget() {
			if (this.backdropImage != null) {
				this.backdropImage.close();
				this.backdropImage = null;
			}
			if (this.backdropTexture != null) {
				this.backdropTexture.close();
				this.backdropTexture = null;
			}
			if (this.surface != null) {
				this.surface.close();
				this.surface = null;
			}
			if (this.renderTarget != null) {
				this.renderTarget.close();
				this.renderTarget = null;
			}
			this.image = 0L;
			this.currentTexture = null;
			this.inSkijaLayout = false;
		}
	}

	private static final class Blur implements AutoCloseable {

		private static final int[][] STRENGTHS = {
				{1, 125}, {1, 225}, {2, 200}, {2, 300}, {2, 425},
				{3, 250}, {3, 325}, {3, 425}, {3, 550}, {4, 325},
				{4, 400}, {4, 500}, {4, 600}, {4, 725}, {4, 825},
				{5, 450}, {5, 525}, {5, 625}, {5, 725}, {5, 850}
		};

		private static final String DOWNSAMPLE_SHADER = """
				uniform shader image;
				uniform float2 sourceScale;
				uniform float2 halfTexelSize;
				uniform float offset;

				half4 main(float2 coord) {
					float2 sourceCoord = coord * sourceScale;
					half4 color = (
							image.eval(sourceCoord + float2(-halfTexelSize.x * 2.0, 0.0) * offset) +
							image.eval(sourceCoord + float2(-halfTexelSize.x, halfTexelSize.y) * offset) * 2.0 +
							image.eval(sourceCoord + float2(0.0, halfTexelSize.y * 2.0) * offset) +
							image.eval(sourceCoord + halfTexelSize * offset) * 2.0 +
							image.eval(sourceCoord + float2(halfTexelSize.x * 2.0, 0.0) * offset) +
							image.eval(sourceCoord + float2(halfTexelSize.x, -halfTexelSize.y) * offset) * 2.0 +
							image.eval(sourceCoord + float2(0.0, -halfTexelSize.y * 2.0) * offset) +
							image.eval(sourceCoord - halfTexelSize * offset) * 2.0
					) / 12.0;
					return half4(color.rgb, 1.0);
				}
				""";

		private static final String UPSAMPLE_SHADER = """
				uniform shader image;
				uniform float2 sourceScale;
				uniform float2 halfTexelSize;
				uniform float offset;

				half4 main(float2 coord) {
					float2 sourceCoord = coord * sourceScale;
					half4 color = (
							image.eval(sourceCoord) * 4.0 +
							image.eval(sourceCoord - halfTexelSize * offset) +
							image.eval(sourceCoord + halfTexelSize * offset) +
							image.eval(sourceCoord + float2(halfTexelSize.x, -halfTexelSize.y) * offset) +
							image.eval(sourceCoord - float2(halfTexelSize.x, -halfTexelSize.y) * offset)
					) / 8.0;
					return half4(color.rgb, 1.0);
				}
				""";

		private RuntimeEffect downsampleEffect;
		private RuntimeEffect upsampleEffect;
		private Surface[] surfaces;
		private int width;
		private int height;
		private int strength;
		private Image blurredImage;

		private Image get(DirectContext context, Image source, int requestedStrength) {
			int clampedStrength = Math.max(1, Math.min(STRENGTHS.length, requestedStrength));
			if (this.blurredImage != null && this.width == source.getWidth() && this.height == source.getHeight()
					&& this.strength == clampedStrength) {
				return this.blurredImage;
			}

			this.closeFrameImage();
			this.ensureEffects();
			this.ensureSurfaces(context, source.getWidth(), source.getHeight());

			int iterations = STRENGTHS[clampedStrength - 1][0];
			float offset = STRENGTHS[clampedStrength - 1][1] / 100F;
			Image current = source;
			Image owned = null;
			try {
				for (int level = 0; level <= iterations; level++) {
					Surface target = this.surfaces[level];
					this.renderPass(context, target, current, this.downsampleEffect, offset);
					if (owned != null) {
						owned.close();
					}
					owned = target.makeImageSnapshot();
					current = owned;
				}

				for (int level = iterations - 1; level >= 0; level--) {
					Surface target = this.surfaces[level];
					this.renderPass(context, target, current, this.upsampleEffect, offset);
					owned.close();
					owned = target.makeImageSnapshot();
					current = owned;
				}

				this.blurredImage = owned;
				owned = null;
				this.strength = clampedStrength;
				return this.blurredImage;
			} finally {
				if (owned != null) {
					owned.close();
				}
			}
		}

		private void endFrame() {
			this.closeFrameImage();
		}

		@Override
		public void close() {
			this.closeFrameImage();
			this.closeSurfaces();
			if (this.downsampleEffect != null) {
				this.downsampleEffect.close();
				this.downsampleEffect = null;
			}
			if (this.upsampleEffect != null) {
				this.upsampleEffect.close();
				this.upsampleEffect = null;
			}
		}

		private void renderPass(DirectContext context, Surface target, Image source, RuntimeEffect effect, float offset) {
			Canvas canvas = target.getCanvas();
			canvas.clear(0xFF000000);
			float scaleX = source.getWidth() / (float) target.getWidth();
			float scaleY = source.getHeight() / (float) target.getHeight();
			try (
					Shader imageShader = source.makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, SamplingMode.LINEAR, null);
					RuntimeEffectBuilder builder = new RuntimeEffectBuilder(effect);
					Shader shader = builder.setChild("image", imageShader)
							.setUniform("sourceScale", scaleX, scaleY)
							.setUniform("halfTexelSize", 0.5F, 0.5F)
							.setUniform("offset", offset)
							.makeShader();
					Paint paint = new Paint().setShader(shader)
			) {
				canvas.drawRect(Rect.makeWH(target.getWidth(), target.getHeight()), paint);
			}
			context.flushAndSubmit(target, false);
		}

		private void ensureEffects() {
			if (this.downsampleEffect == null) {
				this.downsampleEffect = RuntimeEffect.makeForShader(DOWNSAMPLE_SHADER);
				this.upsampleEffect = RuntimeEffect.makeForShader(UPSAMPLE_SHADER);
			}
		}

		private void ensureSurfaces(DirectContext context, int width, int height) {
			if (this.surfaces != null && this.width == width && this.height == height) {
				return;
			}
			this.closeSurfaces();
			this.width = width;
			this.height = height;
			this.surfaces = new Surface[6];
			for (int level = 0; level < this.surfaces.length; level++) {
				int surfaceWidth = Math.max(1, width >> level);
				int surfaceHeight = Math.max(1, height >> level);
				ImageInfo info = new ImageInfo(surfaceWidth, surfaceHeight, ColorType.RGBA_8888,
						ColorAlphaType.PREMUL, ColorSpace.getSRGB());
				this.surfaces[level] = Surface.makeRenderTarget(context, false, info, 1, SurfaceOrigin.BOTTOM_LEFT, null);
				if (this.surfaces[level] == null) {
					throw new IllegalStateException("Failed to create Kawase blur surface " + surfaceWidth + "x" + surfaceHeight);
				}
			}
		}

		private void closeFrameImage() {
			if (this.blurredImage != null) {
				this.blurredImage.close();
				this.blurredImage = null;
			}
		}

		private void closeSurfaces() {
			if (this.surfaces != null) {
				for (Surface surface : this.surfaces) {
					if (surface != null) {
						surface.close();
					}
				}
				this.surfaces = null;
			}
			this.width = 0;
			this.height = 0;
		}
	}
}
