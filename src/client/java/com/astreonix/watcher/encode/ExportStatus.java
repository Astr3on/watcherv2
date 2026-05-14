package com.astreonix.watcher.encode;

import java.nio.file.Path;

public record ExportStatus(ExportState state, String detail, Path outputFile) {
    public static ExportStatus idle() {
        return new ExportStatus(ExportState.IDLE, "Ready", null);
    }
}
