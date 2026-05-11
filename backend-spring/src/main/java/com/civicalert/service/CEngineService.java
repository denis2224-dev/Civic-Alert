package com.civicalert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.civicalert.dto.DetectionResult;
import com.civicalert.entity.RumorPattern;
import com.civicalert.enums.RiskLevel;
import com.civicalert.repository.RumorPatternRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CEngineService {

    private static final Logger log = LoggerFactory.getLogger(CEngineService.class);
    private static final long ENGINE_TIMEOUT_SECONDS = 6;
    private static final long BUILD_TIMEOUT_SECONDS = 40;
    private static final long STREAM_WAIT_SECONDS = 2;

    private final ObjectMapper objectMapper;
    private final RumorPatternRepository rumorPatternRepository;
    private final Path engineDirectory = Paths.get("../c-engine").toAbsolutePath().normalize();
    private final Path engineBinary = engineDirectory.resolve("civic_alert_engine");
    private volatile boolean compilationAttempted;
    private volatile String compilationError;

    public CEngineService(
            ObjectMapper objectMapper,
            RumorPatternRepository rumorPatternRepository
    ) {
        this.objectMapper = objectMapper;
        this.rumorPatternRepository = rumorPatternRepository;
    }

    @PostConstruct
    public void prepareEngine() {
        if (!Files.exists(engineBinary)) {
            ensureEngineReady();
        }
    }

    public DetectionResult analyzeClaim(String text, String language) {
        DetectionResult fallback = defaultNoMatch();

        if (text == null || text.isBlank()) {
            return fallback;
        }

        if (!ensureEngineReady()) {
            fallback.setError(compilationError);
            fallback.setEngineOutput("{\"matched\":false,\"error\":\"engine_unavailable\"}");
            return fallback;
        }

        Path patternFile = null;
        try {
            patternFile = exportPatternsToTempFile(language);
        } catch (IOException e) {
            log.warn("Could not export rumor patterns for C engine. Using built-in engine patterns.", e);
        }

        try {
            ProcessBuilder processBuilder = buildEngineProcess(text, language, patternFile);
            ProcessResult execution = executeProcess(processBuilder, ENGINE_TIMEOUT_SECONDS, false);

            if (execution.timedOut()) {
                log.error("C engine timed out for text='{}'", text);
                fallback.setError("C engine execution timed out.");
                fallback.setEngineOutput("{\"matched\":false,\"error\":\"engine_timeout\"}");
                return fallback;
            }

            if (execution.exitCode() != 0) {
                log.error("C engine failed with exit code {}. stderr={}", execution.exitCode(), execution.stderr());
                fallback.setError("C engine failed with exit code " + execution.exitCode() + ". " + execution.stderr());
                fallback.setEngineOutput("{\"matched\":false,\"error\":\"engine_execution_failed\"}");
                return fallback;
            }

            String output = execution.stdout().trim();
            DetectionResult parsed = objectMapper.readValue(output, DetectionResult.class);
            parsed.setEngineOutput(output);
            if (parsed.getRiskLevel() == null) {
                parsed.setRiskLevel(RiskLevel.LOW);
            }
            if (parsed.getRiskScore() == null) {
                parsed.setRiskScore(0);
            }
            return parsed;
        } catch (IOException e) {
            log.error("Unable to execute C engine.", e);
            fallback.setError("Unable to execute C engine: " + e.getMessage());
            fallback.setEngineOutput("{\"matched\":false,\"error\":\"io_error\"}");
            return fallback;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("C engine execution interrupted.", e);
            fallback.setError("C engine execution was interrupted.");
            fallback.setEngineOutput("{\"matched\":false,\"error\":\"interrupted\"}");
            return fallback;
        } catch (Exception e) {
            log.error("Invalid JSON output from C engine.", e);
            fallback.setError("Unable to parse C engine output: " + e.getMessage());
            fallback.setEngineOutput("{\"matched\":false,\"error\":\"invalid_engine_json\"}");
            return fallback;
        } finally {
            if (patternFile != null) {
                try {
                    Files.deleteIfExists(patternFile);
                } catch (IOException deleteEx) {
                    log.debug("Failed to delete temp pattern file {}", patternFile, deleteEx);
                }
            }
        }
    }

    private ProcessBuilder buildEngineProcess(String text, String language, Path patternFile) {
        List<String> command = new ArrayList<>();
        command.add(engineBinary.toString());
        command.add("--claim");
        command.add(text);

        if (patternFile != null) {
            command.add("--patterns");
            command.add(patternFile.toAbsolutePath().toString());
        }

        if (language != null && !language.isBlank()) {
            command.add("--language");
            command.add(language.trim().toLowerCase(Locale.ROOT));
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(engineDirectory.toFile());
        return processBuilder;
    }

    private Path exportPatternsToTempFile(String language) throws IOException {
        List<RumorPattern> patterns = rumorPatternRepository.findByActiveTrueAndLanguage(
                language == null ? "" : language.trim().toLowerCase(Locale.ROOT)
        );
        if (patterns.isEmpty()) {
            patterns = rumorPatternRepository.findByActiveTrue();
        }
        patterns = patterns.stream()
                .sorted(
                        Comparator.comparing(RumorPattern::getSeverity, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(RumorPattern::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();

        if (patterns.isEmpty()) {
            return null;
        }

        Path tempFile = Files.createTempFile("civicalert_patterns_", ".txt");
        List<String> lines = patterns.stream()
                .map(pattern -> {
                    String phrase = sanitizePatternField(
                            pattern.getNormalizedPhrase() == null || pattern.getNormalizedPhrase().isBlank()
                                    ? pattern.getPhrase()
                                    : pattern.getNormalizedPhrase()
                    );
                    String category = sanitizePatternField(pattern.getCategory());
                    int severity = pattern.getSeverity() == null ? 1 : pattern.getSeverity();
                    String patternLanguage = sanitizePatternField(pattern.getLanguage());
                    return phrase + "|" + category + "|" + severity + "|" + patternLanguage;
                })
                .toList();
        Files.write(tempFile, lines, StandardCharsets.UTF_8);
        return tempFile;
    }

    private String sanitizePatternField(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", " ").replace("\n", " ").replace("\r", " ").trim();
    }

    private synchronized boolean ensureEngineReady() {
        if (Files.exists(engineBinary) && Files.isExecutable(engineBinary)) {
            return true;
        }

        if (compilationAttempted && compilationError != null) {
            return false;
        }

        compilationAttempted = true;
        ProcessBuilder compileProcessBuilder = new ProcessBuilder("make");
        compileProcessBuilder.directory(engineDirectory.toFile());
        compileProcessBuilder.redirectErrorStream(true);

        try {
            ProcessResult execution = executeProcess(compileProcessBuilder, BUILD_TIMEOUT_SECONDS, true);

            if (execution.timedOut()) {
                compilationError = "C engine build timed out.";
                log.error(compilationError);
                return false;
            }

            if (execution.exitCode() != 0 || !Files.exists(engineBinary)) {
                compilationError = "Unable to compile C engine. make exit=" + execution.exitCode() + ". " + execution.stdout();
                log.error(compilationError);
                return false;
            }

            engineBinary.toFile().setExecutable(true);
            compilationError = null;
            return true;
        } catch (IOException e) {
            compilationError = "Unable to start C engine build: " + e.getMessage();
            log.error(compilationError, e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            compilationError = "C engine build was interrupted.";
            log.error(compilationError, e);
            return false;
        }
    }

    private ProcessResult executeProcess(ProcessBuilder processBuilder, long timeoutSeconds, boolean mergedErrorStream)
            throws IOException, InterruptedException {
        Process process = processBuilder.start();

        CompletableFuture<String> stdoutFuture = readStream(process.getInputStream());
        CompletableFuture<String> stderrFuture = mergedErrorStream
                ? CompletableFuture.completedFuture("")
                : readStream(process.getErrorStream());

        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!completed) {
            process.destroy();
            if (!process.waitFor(1, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        }

        int exitCode = completed ? process.exitValue() : -1;
        String stdout = awaitStream(stdoutFuture);
        String stderr = mergedErrorStream ? "" : awaitStream(stderrFuture);

        return new ProcessResult(completed, exitCode, stdout, stderr);
    }

    private CompletableFuture<String> readStream(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream stream = inputStream) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "";
            }
        });
    }

    private String awaitStream(CompletableFuture<String> streamFuture) {
        try {
            return streamFuture.get(STREAM_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException | TimeoutException e) {
            return "";
        }
    }

    private DetectionResult defaultNoMatch() {
        DetectionResult result = new DetectionResult();
        result.setMatched(false);
        result.setRiskScore(0);
        result.setRiskLevel(RiskLevel.LOW);
        result.setEngineOutput("{\"matched\":false}");
        result.setCategory("general");
        result.setSeverity(0);
        result.setMatchedPhrase(null);
        result.setError(null);
        return result;
    }

    private record ProcessResult(boolean completed, int exitCode, String stdout, String stderr) {
        boolean timedOut() {
            return !completed;
        }
    }
}
