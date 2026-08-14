package com.elog.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class Report5L2CatalogIntegrityTest {

    private static final Path CATALOG = Path.of("test-execution/catalog/l2.json");

    @Test
    void catalogContainsExactlySeventyFiveUniqueExecutableCasesAcrossSeventeenServices() throws Exception {
        String json = Files.readString(CATALOG).replaceFirst("^\\uFEFF", "");
        JsonNode cases = new ObjectMapper().readTree(json).path("cases");
        Set<String> ids = new HashSet<>();
        Set<String> sheets = new HashSet<>();

        cases.forEach(testCase -> {
            ids.add(testCase.path("id").asText());
            sheets.add(testCase.path("sheet").asText());
            assertThat(testCase.path("when").asText()).isNotBlank();
            assertThat(testCase.path("then").asText()).isNotBlank();
            assertThat(testCase.path("boundaryEvidence").path("schemaRefs").size()).isPositive();
        });

        assertThat(cases.size()).isEqualTo(75);
        assertThat(ids).hasSize(75);
        assertThat(sheets).hasSize(17);
    }
}
