package com.elog.integration;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class Report5L2EvidenceExtension implements AfterTestExecutionCallback {

    private static final Path EVIDENCE = Path.of("test-execution/evidence/l2-junit-results.ndjson");

    @Override
    public void afterTestExecution(ExtensionContext context) throws IOException {
        String method = context.getRequiredTestMethod().getName();
        String testId = toTestId(method);
        if (testId == null) return;
        Throwable failure = context.getExecutionException().orElse(null);
        String status = failure == null ? "Pass" : "Fail";
        String message = failure == null ? "" : failure.toString().replace("\\", "\\\\").replace("\"", "\\\"");
        String json = "{\"testId\":\"" + testId + "\",\"status\":\"" + status
                + "\",\"class\":\"" + context.getRequiredTestClass().getName()
                + "\",\"method\":\"" + method + "\",\"message\":\"" + message + "\"}\n";
        Files.createDirectories(EVIDENCE.getParent());
        Files.writeString(EVIDENCE, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    static String toTestId(String methodName) {
        if (!methodName.startsWith("l2") || methodName.length() < 7) return null;
        String compact = methodName.substring(2, 7).toUpperCase();
        return "L2-" + compact.substring(0, 3) + "-" + compact.substring(3);
    }
}
