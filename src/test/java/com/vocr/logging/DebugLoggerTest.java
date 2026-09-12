package com.vocr.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated("mutates process streams and the DebugLogger singleton")
class DebugLoggerTest {
    private final DebugLogger logger = DebugLogger.getInstance();
    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private PrintStream originalErr;

    @TempDir
    Path tempDir;

    @BeforeEach
    void redirectProcessStreams() {
        originalOut = System.out;
        originalErr = System.err;
        logger.setEnabled(false);
        logger.stopFileLogging();
        logger.setMinConsoleLevel(DebugLogger.LogLevel.DEBUG);
        logger.setMinFileLevel(DebugLogger.LogLevel.INFO);
        logger.setEnabled(true);
        System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(stderr, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreProcessStreams() {
        try {
            logger.setEnabled(false);
            logger.stopFileLogging();
            logger.setMinConsoleLevel(DebugLogger.LogLevel.DEBUG);
            logger.setMinFileLevel(DebugLogger.LogLevel.INFO);
            logger.setEnabled(true);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    void errorWithThrowableWritesCompleteRecordToStderrAndFile() throws IOException {
        logger.setMinConsoleLevel(DebugLogger.LogLevel.ERROR);
        logger.setMinFileLevel(DebugLogger.LogLevel.ERROR);
        Path logFile = tempDir.resolve("中文日志").resolve("error.log");
        logger.startFileLogging(logFile.toString());

        IllegalStateException failure = new IllegalStateException(
                "outer failure", new IOException("root cause"));
        DebugLogger.error("operation failed", failure);
        logger.stopFileLogging();

        String consoleRecord = stderr.toString(StandardCharsets.UTF_8);
        String fileRecord = Files.readString(logFile);
        assertAll(
                () -> assertEquals("", stdout.toString(StandardCharsets.UTF_8)),
                () -> assertCompleteRecord(consoleRecord, "ERROR", "operation failed"),
                () -> assertCompleteRecord(fileRecord, "ERROR", "operation failed"),
                () -> assertTrue(consoleRecord.contains("Caused by: java.io.IOException: root cause")),
                () -> assertTrue(fileRecord.contains("Caused by: java.io.IOException: root cause")));
    }

    @Test
    void warnWithThrowableUsesStdoutInsteadOfStderr() {
        IOException failure = new IOException("cleanup failure");

        DebugLogger.warn("cleanup failed", failure);

        assertAll(
                () -> assertCompleteRecord(
                        stdout.toString(StandardCharsets.UTF_8), "WARN", "cleanup failed"),
                () -> assertEquals("", stderr.toString(StandardCharsets.UTF_8)));
    }

    @Test
    void levelThresholdSuppressesBothHeaderAndThrowable() throws IOException {
        logger.setMinConsoleLevel(DebugLogger.LogLevel.ERROR);
        logger.setMinFileLevel(DebugLogger.LogLevel.ERROR);
        Path logFile = tempDir.resolve("filtered.log");
        logger.startFileLogging(logFile.toString());

        DebugLogger.warn("hidden warning", new IOException("hidden cause"));
        logger.stopFileLogging();

        assertAll(
                () -> assertEquals("", stdout.toString(StandardCharsets.UTF_8)),
                () -> assertEquals("", stderr.toString(StandardCharsets.UTF_8)),
                () -> assertEquals("", Files.readString(logFile)));
    }

    @Test
    void disabledLoggerSuppressesBothHeaderAndThrowable() throws IOException {
        logger.setMinConsoleLevel(DebugLogger.LogLevel.ERROR);
        logger.setMinFileLevel(DebugLogger.LogLevel.ERROR);
        Path logFile = tempDir.resolve("disabled.log");
        logger.startFileLogging(logFile.toString());
        logger.setEnabled(false);

        DebugLogger.error("hidden error", new IOException("hidden cause"));
        logger.stopFileLogging();

        assertAll(
                () -> assertEquals("", stdout.toString(StandardCharsets.UTF_8)),
                () -> assertEquals("", stderr.toString(StandardCharsets.UTF_8)),
                () -> assertEquals("", Files.readString(logFile)));
    }

    private static void assertCompleteRecord(String record, String level, String message) {
        String firstLine = record.lines().findFirst().orElse("");
        assertAll(
                () -> assertTrue(firstLine.matches(
                        "\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}] \\[" + level + "] " + message)),
                () -> assertTrue(record.contains("java.")),
                () -> assertTrue(record.contains("failure")),
                () -> assertTrue(record.contains("\tat ")));
    }
}
