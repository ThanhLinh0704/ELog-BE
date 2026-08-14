package com.elog.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Report5L2EvidenceExtensionTest {

    @Test
    void convertsReportFiveMethodNameToCatalogId() {
        assertThat(Report5L2EvidenceExtension.toTestId("l2Prd01CreatesProduct"))
                .isEqualTo("L2-PRD-01");
        assertThat(Report5L2EvidenceExtension.toTestId("ordinaryTest")).isNull();
    }
}
