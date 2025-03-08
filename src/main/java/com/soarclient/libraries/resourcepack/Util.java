package com.soarclient.libraries.resourcepack;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Util {

    private Util() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static <K, V, M extends Map<K, V>> M createMap(Supplier<M> mapSupplier, Consumer<M> initializer) {
        M map = mapSupplier.get();
        initializer.accept(map);
        return map;
    }

    public static void copyDir(Path src, Path dest) throws IOException {
        Files.walk(src).forEach(path -> {
            try {
                Files.copy(path, dest.resolve(src.relativize(path)));
            } catch (Throwable e) {
                throw Util.propagate(e);
            }
        });
    }

    public static void deleteDirectoryAndContents(Path dirPath) throws IOException {
        if (!dirPath.toFile().exists()) return;

        //noinspection ResultOfMethodCallIgnored
        Files.walk(dirPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public static boolean fileExistsCorrectCasing(Path path) throws IOException {
        if (!path.toFile().exists()) return false;
        return path.toString().equals(path.toFile().getCanonicalPath());
    }

    public static JsonObject readJsonResource(Gson gson, String path) {
        try (InputStream stream = PackConverter.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            try (InputStreamReader streamReader = new InputStreamReader(stream)) {
                return gson.fromJson(streamReader, JsonObject.class);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage readImageResource(String path) {
        try (InputStream stream = PackConverter.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            return ImageIO.read(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonObject readJson(Gson gson, Path path) throws IOException {
        return Util.readJson(gson, path, JsonObject.class);
    }

    public static <T> T readJson(Gson gson, Path path, Class<T> clazz) throws IOException {
        // TODO Improvement: this will fail if there is a BOM in the file
        return gson.fromJson(new JsonReader(new FileReader(path.toFile())), clazz);
    }

    public static void writeJson(Gson gson, Path path, JsonElement json, boolean dontEscapeUnicode) {
        try (FileWriter fileWriter = new FileWriter(path.toFile())) {
            String jsonString = gson.toJson(json);
            if (dontEscapeUnicode) {
                jsonString = jsonString.replace("\\\\u", "\\u");
            }

            fileWriter.write(jsonString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeJson(Gson gson, Path path, JsonElement json) {
        writeJson(gson, path, json, true);
    }

    /**
     * @return null if file doesn't exist, {@code true} if successfully renamed, {@code false} if failed
     */
    public static Boolean renameFile(Path file, String newName) {
        if (!file.toFile().exists()) return null;
        return file.toFile().renameTo(new File(file.getParent() + "/" + newName));
    }

    public static RuntimeException propagate(Throwable t) {
        throw new RuntimeException(t);
    }

}
