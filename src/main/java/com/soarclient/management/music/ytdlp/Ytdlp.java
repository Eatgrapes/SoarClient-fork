package com.soarclient.management.music.ytdlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.soarclient.logger.SoarLogger;
import com.soarclient.utils.file.FileLocation;

public class Ytdlp {

	private String ytdlp;
	private String ffmpeg;

	public void setYtdlpPath(File file) {
		this.ytdlp = file.getAbsolutePath();
	}

	public void setYtdlpCommand(String command) {
		this.ytdlp = command;
	}

	public void setFFmpegPath(File file) {
		this.ffmpeg = file.getAbsolutePath();
	}

	public boolean download(String url) {
		return download(url, "flac");
	}

	public boolean download(String url, String format) {

		List<String> command = new ArrayList<>();

		command.add(ytdlp);
		command.add("-f");
		command.add("bestaudio");
		command.add("--extract-audio");
		command.add("--audio-format");
		command.add(format);
		command.add("--embed-thumbnail");
		command.add("--convert-thumbnails");
		command.add("png");
		command.add("--add-metadata");

		if (ffmpeg != null && !ffmpeg.isBlank() && !ffmpeg.isEmpty()) {
			command.add("--ffmpeg-location");
			command.add(ffmpeg);
		}

		command.add("--output");
		command.add(FileLocation.MUSIC_DIR + File.separator + "%(title)s.%(ext)s");
		command.add(url);

		try {
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			Process process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				SoarLogger.info("YTDLP", line);
			}

			int exitCode = process.waitFor();

			if (exitCode == 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}