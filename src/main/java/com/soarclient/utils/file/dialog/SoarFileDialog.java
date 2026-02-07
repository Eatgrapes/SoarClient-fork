package com.soarclient.utils.file.dialog;

import java.io.File;
import java.nio.ByteBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NFDFilterItem;
import org.lwjgl.util.nfd.NativeFileDialog;

import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;

public class SoarFileDialog {

	static {
		NativeFileDialog.NFD_Init();
	}

	public static ObjectObjectImmutablePair<Boolean, File> chooseFile(String name, String... extensions) {

		try (MemoryStack stack = MemoryStack.stackPush()) {

			NFDFilterItem.Buffer filters = NFDFilterItem.malloc(1, stack);

			filters.get(0).name(stack.UTF8(name)).spec(stack.UTF8(String.join(",", extensions)));

			PointerBuffer path = stack.mallocPointer(1);

			int result = NativeFileDialog.NFD_OpenDialog(path, filters, (ByteBuffer) null);

			if (result == NativeFileDialog.NFD_OKAY) {
				String pathString = path.getStringUTF8(0);
				NativeFileDialog.NFD_FreePath(path.get(0));
				return ObjectObjectImmutablePair.of(true, new File(pathString));
			}

			return ObjectObjectImmutablePair.of(false, null);
		}
	}

	public static ObjectObjectImmutablePair<Boolean, File> chooseFolder() {

		try (MemoryStack stack = MemoryStack.stackPush()) {
			PointerBuffer path = stack.mallocPointer(1);

			int result = NativeFileDialog.NFD_PickFolder(path, (ByteBuffer) null);

			if (result == NativeFileDialog.NFD_OKAY) {
				String pathString = path.getStringUTF8(0);
				NativeFileDialog.NFD_FreePath(path.get(0));
				return ObjectObjectImmutablePair.of(true, new File(pathString));
			}

			return ObjectObjectImmutablePair.of(false, null);
		}
	}

	private static boolean isSuccess(int result) {
		return result == NativeFileDialog.NFD_OKAY;
	}
}
