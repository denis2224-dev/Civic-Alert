package com.civicalert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.civicalert.dto.DetectionResult;
import com.civicalert.enums.RiskLevel;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Service;

@Service
public class CEngineService {

    private final ObjectMapper objectMapper;
    private final Path engineDirectory = Paths.get("../c-engine").toAbsolutePath().normalize();
    private final Path engineBinary = engineDirectory.resolve("civic_alert_engine");
    private volatile boolean compilationAttempted;
    private volatile String compilationError;

    public CEngineService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

        ProcessBuilder processBuilder = new ProcessBuilder(engineBinary.toString(), text);
        processBuilder.directory(engineDirectory.toFile());

        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            String errorOutput = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                fallback.setError("C engine failed with exit code " + exitCode + ". " + errorOutput);
                fallback.setEngineOutput("{\"matched\":false,\"error\":\"engine_execution_failed\"}");
                return fallback;
            }

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
            fallback.setError("Unable to execute C engine: " + e.getMessage());
            fallback.setEngineOutput("{\"matched\":false,\"error\":\"io_error\"}");
            return fallback;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fallback.setError("C engine execution was interrupted.");
            fallback.setEngineOutput("{\"matched\":false,\"error\":\"interrupted\"}");
            return fallback;
        } catch (Exception e) {
            fallback.setError("Unable to parse C engine output: " + e.getMessage());
            fallback.setEngineOutput("{\"matched\":false,\"error\":\"invalid_engine_json\"}");
            return fallback;
        }
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
            Process compileProcess = compileProcessBuilder.start();
            String output = new String(compileProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = compileProcess.waitFor();

            if (exitCode != 0 || !Files.exists(engineBinary)) {
                compilationError = "Unable to compile C engine. make exit=" + exitCode + ". " + output;
                return false;
            }

            engineBinary.toFile().setExecutable(true);
            compilationError = null;
            return true;
        } catch (IOException e) {
            compilationError = "Unable to start C engine build: " + e.getMessage();
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            compilationError = "C engine build was interrupted.";
            return false;
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
}
