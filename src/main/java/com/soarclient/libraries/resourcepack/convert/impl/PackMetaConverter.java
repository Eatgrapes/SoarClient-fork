package com.soarclient.libraries.resourcepack.convert.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.soarclient.libraries.resourcepack.ResourcePackConverter;
import com.soarclient.libraries.resourcepack.convert.Converter;

public class PackMetaConverter extends Converter {

	public PackMetaConverter() {
		super();
	}

	@Override
	public void convert(File assetsDir) {

		Path file = new File(assetsDir, "pack.mcmeta").toPath();
		Gson gson = new Gson();

		if (!file.toFile().exists()) {
			return;
		}

		try {
			JsonObject json = gson.fromJson(new JsonReader(new FileReader(file.toFile())), JsonObject.class);

			JsonObject packObject = json.getAsJsonObject("pack");

			if (packObject == null)
				packObject = new JsonObject();
			packObject.addProperty("pack_format", ResourcePackConverter.VERSION);
			json.add("pack", packObject);
			json.addProperty("convert", true);

			Files.write(file, Collections.singleton(gson.toJson(json)), Charset.forName("UTF-8"));
		} catch (JsonIOException | JsonSyntaxException | IOException e) {
		}
	}

}
