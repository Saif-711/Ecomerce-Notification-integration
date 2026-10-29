package com.notificationsystem.notificationservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a local .env file into Spring's Environment early enough for
 * ${RESEND_API_KEY} placeholders in application.yml.
 *
 * spring-dotenv only looks at the JVM working directory. Running the app
 * from the repo root (IntelliJ / Maven parent) misses notification-service/.env,
 * while the same keys in application.yml always work because that file is on
 * the classpath.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);
    private static final String PROPERTY_SOURCE_NAME = "notificationServiceDotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }

        Path envFile = findEnvFile();
        if (envFile == null) {
            log.warn("No .env file found. Resend keys in YAML placeholders will be empty unless set in the OS environment.");
            return;
        }

        Map<String, Object> values = readEnvFile(envFile);
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
        log.info("Loaded {} entries from {}", values.size(), envFile.toAbsolutePath());
    }

    private Path findEnvFile() {
        List<Path> candidates = new ArrayList<>();
        Path cwd = Path.of("").toAbsolutePath().normalize();
        candidates.add(cwd.resolve(".env"));
        candidates.add(cwd.resolve("notification-service").resolve(".env"));
        candidates.add(cwd.resolve("Notification-System").resolve("notification-service").resolve(".env"));
        candidates.add(cwd.resolve("Notification-System").resolve(".env"));

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            Path userPath = Path.of(userDir).toAbsolutePath().normalize();
            candidates.add(userPath.resolve(".env"));
            candidates.add(userPath.resolve("notification-service").resolve(".env"));
            candidates.add(userPath.resolve("Notification-System").resolve("notification-service").resolve(".env"));
        }

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private Map<String, Object> readEnvFile(Path envFile) {
        Map<String, Object> values = new LinkedHashMap<>();
        FileSystemResource resource = new FileSystemResource(envFile.toFile());
        try {
            List<String> lines = Files.readAllLines(resource.getFile().toPath(), StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = stripBom(rawLine).trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = unquote(line.substring(eq + 1).trim());
                values.put(key, value);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read .env file: " + envFile.toAbsolutePath(), e);
        }
        return values;
    }

    private static String stripBom(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
