package com.astreonix.watcher.encode;

import com.astreonix.watcher.WatcherClient;
import com.astreonix.watcher.config.WatcherConfig;
import com.astreonix.watcher.record.RecordingSession;
import com.astreonix.watcher.record.VideoFrame;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public final class FFmpegEncoder {
    private final WatcherConfig config;
    private final Map<UUID, ExportStatus> statuses = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Watcher FFmpeg Encoder");
        thread.setDaemon(true);
        return thread;
    });

    public FFmpegEncoder(WatcherConfig config) {
        this.config = config;
    }

    public void export(RecordingSession session, Path outputFile) {
        statuses.put(session.id(), new ExportStatus(ExportState.QUEUED, "Queued for export", outputFile));
        executor.submit(() -> runExport(session, outputFile));
    }

    public ExportStatus status(RecordingSession session) {
        return statuses.getOrDefault(session.id(), ExportStatus.idle());
    }

    public void persistSession(RecordingSession session) {
        executor.submit(() -> {
            try {
                writeSession(session);
            } catch (IOException exception) {
                WatcherClient.LOGGER.warn("Failed to persist Watcher session {}", session.id(), exception);
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void runExport(RecordingSession session, Path outputFile) {
        try {
            statuses.put(session.id(), new ExportStatus(ExportState.SAVING_SESSION, "Saving frames", outputFile));
            writeSession(session);
            Files.createDirectories(outputFile.toAbsolutePath().getParent());

            if (session.frames().isEmpty()) {
                statuses.put(session.id(), new ExportStatus(ExportState.FAILED, "No frames captured", outputFile));
                return;
            }

            statuses.put(session.id(), new ExportStatus(ExportState.ENCODING, "Encoding MP4", outputFile));
            runFfmpeg(session, outputFile);
            statuses.put(session.id(), new ExportStatus(ExportState.COMPLETE, "Exported " + outputFile.getFileName(), outputFile));
            WatcherClient.LOGGER.info("Exported Watcher recording {} to {}", session.id(), outputFile);
        } catch (IOException exception) {
            statuses.put(session.id(), new ExportStatus(ExportState.FAILED, exception.getMessage(), outputFile));
            WatcherClient.LOGGER.warn("Failed to export Watcher recording {}", session.id(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            statuses.put(session.id(), new ExportStatus(ExportState.FAILED, "Export interrupted", outputFile));
            WatcherClient.LOGGER.warn("Interrupted Watcher export {}", session.id(), exception);
        }
    }

    private void runFfmpeg(RecordingSession session, Path outputFile) throws IOException, InterruptedException {
        Path framesDirectory = session.directory().resolve("frames");
        Path framePattern = framesDirectory.resolve("frame-%06d.png");
        Path ffmpegLog = session.directory().resolve("ffmpeg-export.log");
        List<String> command = new ArrayList<>();
        command.add(resolveFfmpegPath());
        command.add("-y");
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("warning");
        command.add("-framerate");
        command.add(Integer.toString(session.settings().frameRate()));
        command.add("-i");
        command.add(framePattern.toString());
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add(config.ffmpegPreset);
        command.add("-crf");
        command.add(Integer.toString(config.crf));
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add(outputFile.toString());

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.to(ffmpegLog.toFile()))
                .start();
        boolean finished = process.waitFor(30, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("FFmpeg export timed out");
        }
        if (process.exitValue() != 0) {
            String output = Files.exists(ffmpegLog) ? Files.readString(ffmpegLog) : "No FFmpeg log was written";
            throw new IOException("FFmpeg failed: " + output.trim());
        }
    }

    private String resolveFfmpegPath() {
        String configured = config.ffmpegPath == null || config.ffmpegPath.isBlank() ? "ffmpeg" : config.ffmpegPath;
        if (!"ffmpeg".equalsIgnoreCase(configured)) {
            return configured;
        }

        String executable = System.getProperty("os.name", "").toLowerCase().contains("win") ? "ffmpeg.exe" : "ffmpeg";
        String path = System.getenv("PATH");
        if (path != null) {
            for (String entry : path.split(java.io.File.pathSeparator)) {
                Path candidate = Path.of(entry, executable);
                if (Files.isRegularFile(candidate)) {
                    return candidate.toString();
                }
            }
        }

        Path userHome = Path.of(System.getProperty("user.home", "."));
        try (var stream = Files.newDirectoryStream(userHome, "ffmpeg*")) {
            for (Path directory : stream) {
                Path candidate = directory.resolve("bin").resolve(executable);
                if (Files.isRegularFile(candidate)) {
                    return candidate.toString();
                }
            }
        } catch (IOException ignored) {
        }

        Path commonWindows = Path.of("C:\\", "ffmpeg", "bin", executable);
        if (Files.isRegularFile(commonWindows)) {
            return commonWindows.toString();
        }
        return configured;
    }

    private void writeSession(RecordingSession session) throws IOException {
        Path sessionDirectory = session.directory();
        Path framesDirectory = sessionDirectory.resolve("frames");
        Files.createDirectories(framesDirectory);

        for (VideoFrame frame : session.frames()) {
            Path frameFile = framesDirectory.resolve("frame-" + String.format("%06d", frame.index()) + ".png");
            if (!Files.exists(frameFile)) {
                ImageIO.write(toImage(frame), "png", frameFile.toFile());
            }
        }

        Files.write(sessionDirectory.resolve("metadata.json"), List.of(
                "{",
                "  \"id\": \"" + session.id() + "\",",
                "  \"createdAt\": \"" + DateTimeFormatter.ISO_INSTANT.format(session.createdAt()) + "\",",
                "  \"durationNanos\": " + session.durationNanos() + ",",
                "  \"frameCount\": " + session.frames().size() + ",",
                "  \"audioChunkCount\": " + session.audioChunks().size() + ",",
                "  \"frameRate\": " + session.settings().frameRate() + ",",
                "  \"crf\": " + session.settings().crf() + ",",
                "  \"voiceEffect\": \"" + session.settings().voiceEffect() + "\"",
                "}"
        ));
        WatcherClient.LOGGER.info("Persisted Watcher session {} to {}", session.id(), sessionDirectory);
    }

    private BufferedImage toImage(VideoFrame frame) {
        BufferedImage image = new BufferedImage(frame.width(), frame.height(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < frame.height(); y++) {
            for (int x = 0; x < frame.width(); x++) {
                image.setRGB(x, y, frame.argbPixels()[y * frame.width() + x]);
            }
        }
        return image;
    }
}
