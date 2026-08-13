# Report 5 - Test execution status

Generated: 2026-08-13 18:13:00 +07:00

Pass is assigned only to a directly executed assertion. Stubbed Cypress suites, generated `assertTrue(true)` classes, source inspection, and fake HTTP responses are explicitly excluded.

| Level | Pass | Fail | Not Run | Total |
|---|---:|---:|---:|---:|
| L1 | 31 | 0 | 114 | 145 |
| L2 | 1 | 0 | 74 | 75 |
| L3 | 73 | 0 | 57 | 130 |
| L4 | 5 | 0 | 45 | 50 |
| UAT | 0 | 0 | 25 | 25 |

## Detailed ledger

| Test ID | Level | Status | Evidence / reason |
|---|---|---|---|
| L1-SM-01 | L1 | Pass | Maven JUnit PASS: TripStateMachineTest#l1Sm01_validatedToDispatched_setsLockAndVehicleAvailable |
| L1-SM-02 | L1 | Pass | Maven JUnit PASS: TripStateMachineTest#l1Sm02_dispatchedToInProgress_setsDepartureAndVehicleInUse |
| L1-SM-03 | L1 | Pass | Maven JUnit PASS: TripStateMachineTest#l1Sm03_inProgressToCompleted_setsCompletedAt |
| L1-SM-04 | L1 | Pass | Maven JUnit PASS: TripStateMachineTest#l1Sm04_validatedToDispatched_acceptsNullVehicle |
| L1-SM-05 | L1 | Pass | Maven JUnit PASS: TripStateMachineTest#l1Sm05_completedRejectsEveryTargetWithoutMutation |
| L1-SM-06 | L1 | Pass | Maven JUnit PASS: TripStateMachineTest#l1Sm06_dispatchedToValidated_reportsLocked |
| L1-SM-07 | L1 | Pass | Maven JUnit PASS: TripStateMachineTest#l1Sm07_unsupportedNonTerminalTransitionsAreRejected |
| L1-AU-01 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-AU-02 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-AU-03 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-AU-04 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-AU-05 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-AU-06 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-AU-07 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-AU-13 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-AU-14 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-IM-01 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-IM-02 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-IM-03 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-IM-04 | L1 | Pass | Maven JUnit PASS: ImportServiceImplTest#processRow_duplicateSkuInSameOrder_accumulatesQuantity |
| L1-IM-05 | L1 | Pass | Maven JUnit PASS: ImportServiceImplTest#importExcel_invalidExtension_throwsException |
| L1-IM-06 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-IM-07 | L1 | Pass | Maven JUnit PASS: ImportServiceImplTest#importExcel_nullFile_throwsException + importExcel_emptyFile_throwsException |
| L1-IM-08 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-IM-09 | L1 | Pass | Maven JUnit PASS: ImportServiceImplTest#parseRow_storeCodeNotExist_rejectsRow |
| L1-IM-16 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-IM-17 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-IM-18 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-01 | L1 | Pass | Maven JUnit PASS: CapacityValidationServiceImplTest#validate_success_eligibleVehicle |
| L1-CV-02 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-03 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-04 | L1 | Pass | Maven JUnit PASS: CapacityValidationServiceImplTest#validate_fails_whenVolumeExceedsSafetyBuffer |
| L1-CV-06 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-07 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-08 | L1 | Pass | Maven JUnit PASS: CapacityValidationServiceImplTest#validate_fails_whenWeightExceedsStoreLimit |
| L1-CV-09 | L1 | Pass | Maven JUnit PASS: CapacityValidationServiceImplTest#validate_fails_whenEtaOutsideStoreAllowedHours |
| L1-CV-10 | L1 | Pass | Maven JUnit PASS: CapacityValidationServiceImplTest#validate_fails_whenEtaOutsideOrderTimeWindow |
| L1-CV-11 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-12 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-13 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-14 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-15 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-16 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-17 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-18 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-21 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CV-22 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RC-01 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RC-02 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RC-03 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RC-04 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RC-05 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RC-07 | L1 | Pass | Maven JUnit PASS: CapacityValidationServiceImplTest#validate_fails_whenNeitherSingleNorTwoVehicleFits |
| L1-RC-08 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RC-09 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RC-10 | L1 | Pass | Maven JUnit PASS: CapacityValidationServiceImplTest#validate_success_viaTwoVehicleFallback_whenNoSingleVehicleFitsButPairDoes |
| L1-RC-11 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RC-12 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RC-13 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TD-01 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TD-02 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TD-03 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TD-04 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TD-05 | L1 | Pass | Maven JUnit PASS: TripDraftServiceImplTest#consolidate_emptyOrders_returnsEmptyResponse |
| L1-TD-06 | L1 | Pass | Maven JUnit PASS: TripDraftServiceImplTest#consolidate_unmappedOrders_skipped |
| L1-TD-07 | L1 | Pass | Maven JUnit PASS: TripDraftServiceImplTest#consolidate_existingDraftInDraftStatus_updatesDraft |
| L1-TD-08 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TD-09 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TD-10 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TD-11 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TS-01 | L1 | Pass | Maven JUnit PASS: TripServiceImplTest#assignVehicleAndDriver_success_compatibleLicense |
| L1-TS-02 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TS-03 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TS-04 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TS-05 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TS-06 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TS-07 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TS-08 | L1 | Pass | Maven JUnit PASS: TripServiceImplTest#assignVehicleAndDriver_fails_incompatibleLicense |
| L1-TS-09 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TS-10 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TS-11 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TS-12 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-DR-04 | L1 | Pass | Maven JUnit PASS: DriverTripServiceImplTest#startTrip_setsVehicleStatusToInUse |
| L1-DR-05 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-DR-06 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-DR-07 | L1 | Pass | Maven JUnit PASS: DriverTripServiceImplTest#startTrip_futureDeliveryDate_throwsException |
| L1-DR-08 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-DR-09 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-DR-10 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-DR-11 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-DR-12 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-DR-13 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-DR-14 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-DR-15 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-DR-16 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-DR-17 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-US-01 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-US-02 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-US-03 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-US-04 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-US-05 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-US-06 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-US-07 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-US-08 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-US-09 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RL-01 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RL-02 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RL-03 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RL-04 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RL-05 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RL-06 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RL-07 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RL-08 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-VH-11 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-VH-12 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RT-03 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RT-04 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RT-05 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RT-06 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RT-07 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RT-08 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-RT-09 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-MF-01 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-MF-02 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-MF-03 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-MF-04 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-MF-05 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-MF-06 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-MF-07 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TO-04 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TO-05 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TO-06 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TO-07 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TO-08 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-TO-09 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-AU-15 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-AU-16 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-AU-17 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-CS-10 | L1 | Pass | Maven JUnit PASS: ConstraintValidationServiceImplTest#testValidateStoreVehicle_Pass |
| L1-CS-11 | L1 | Pass | Maven JUnit PASS: ConstraintValidationServiceImplTest#testValidateStoreVehicle_WeightExceeded |
| L1-CS-12 | L1 | Not Run | No direct one-to-one assertion matching this catalog expectation was executed. |
| L1-HV-01 | L1 | Pass | Maven JUnit PASS: HaversineEtaCalculatorTest#l1Hv01_sameCoordinatesReturnZero |
| L1-HV-02 | L1 | Pass | Maven JUnit PASS: HaversineEtaCalculatorTest#l1Hv02_hanoiToHoChiMinhIsApproximatelyKnownDistance |
| L1-HV-03 | L1 | Pass | Maven JUnit PASS: HaversineEtaCalculatorTest#l1Hv03_antipodalPolesReturnHalfEarthCircumference |
| L1-HV-04 | L1 | Pass | Maven JUnit PASS: HaversineEtaCalculatorTest#l1Hv04_shortUrbanPairReturnsPositiveLocalDistance |
| L2-MON-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-MON-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-MON-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-MON-04 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-MON-05 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-MON-06 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-MON-07 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-UDR-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-UDR-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-UDR-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-UDR-04 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-TED-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-TED-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-TED-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-TED-04 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-EXC-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-EXC-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-EXC-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-EXC-04 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-EXC-05 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-EXC-06 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-DSP-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-DSP-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-DSP-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-DSP-04 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-DSP-05 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-TDC-01 | L2 | Pass | SpringBootTest + MySQL + Flyway PASS: TripDraftServiceImplIntegrationTest#testConsolidateIntegrationSuccess |
| L2-TDC-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-TDC-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-TDC-04 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-TDC-05 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-TOC-04 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-TOC-05 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-TOC-06 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-TOC-07 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-STA-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-STA-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-STA-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-STA-04 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-ATK-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-ATK-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-ATK-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-ATK-04 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-ATK-05 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-CVD-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-CVD-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-CVD-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-CVD-04 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-RCD-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-RCD-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-RCD-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-DRV-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-DRV-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-DRV-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-DRV-07 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-DRV-08 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-PRD-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-PRD-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-PRD-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-MNF-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-MNF-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-RSM-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-RSM-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-RSM-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-RSM-04 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-RSM-05 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-IMP-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-IMP-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-IMP-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-IMP-04 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-IMP-05 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-VPM-01 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-VPM-02 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-VPM-03 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L2-VPM-04 | L2 | Not Run | Legacy integration class was fake (assertTrue(true)) or no isolated DB fixture matched this catalog expectation; excluded from Pass. |
| L3-MASTERDATA-001 | L3 | Pass | HTTP 200 for GET /api/v1/addresses/districts/001/wards; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-002 | L3 | Pass | HTTP 200 for GET /api/v1/addresses/provinces; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-003 | L3 | Pass | HTTP 200 for GET /api/v1/addresses/provinces/01/districts; HTTP 200 and basic response envelope matched |
| L3-AUTH-004 | L3 | Pass | HTTP 200 for POST /api/v1/auth/login; HTTP 200 and basic response envelope matched |
| L3-AUTH-005 | L3 | Pass | HTTP 200 for POST /api/v1/auth/logout; HTTP 200 and basic response envelope matched |
| L3-AUTH-006 | L3 | Pass | HTTP 200 for POST /api/v1/auth/refresh; HTTP 200 and basic response envelope matched |
| L3-MONITORING-007 | L3 | Pass | HTTP 200 for GET /api/v1/dashboard/active-trips; HTTP 200 and basic response envelope matched |
| L3-EXECUTION-008 | L3 | Not Run | HTTP 404 for POST /api/v1/driver/trips/1/complete; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-009 | L3 | Not Run | HTTP 404 for PUT /api/v1/driver/trips/1/orders/1/result; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-010 | L3 | Not Run | HTTP 404 for POST /api/v1/driver/trips/1/return-to-warehouse; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-011 | L3 | Not Run | HTTP 404 for POST /api/v1/driver/trips/1/start; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-012 | L3 | Not Run | HTTP 404 for POST /api/v1/driver/trips/1/stops/1/arrive; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-013 | L3 | Not Run | HTTP 404 for GET /api/v1/driver/trips/active; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-014 | L3 | Pass | HTTP 200 for GET /api/v1/driver/trips/pending-return; HTTP 200 and basic response envelope matched |
| L3-IDENTITY-015 | L3 | Pass | HTTP 200 for GET /api/v1/drivers; HTTP 200 and basic response envelope matched |
| L3-IDENTITY-016 | L3 | Pass | HTTP 200 for GET /api/v1/drivers/6; HTTP 200 and basic response envelope matched |
| L3-IDENTITY-017 | L3 | Pass | HTTP 200 for PATCH /api/v1/drivers/6/status; HTTP 200 and basic response envelope matched |
| L3-IDENTITY-018 | L3 | Pass | HTTP 200 for GET /api/v1/drivers/6/status-history; HTTP 200 and basic response envelope matched |
| L3-IDENTITY-019 | L3 | Pass | HTTP 200 for GET /api/v1/drivers/available?date=2026-08-13; HTTP 200 and basic response envelope matched |
| L3-MONITORING-020 | L3 | Pass | HTTP 200 for GET /api/v1/exceptions; HTTP 200 and basic response envelope matched |
| L3-MONITORING-021 | L3 | Not Run | HTTP 404 for GET /api/v1/exceptions/1; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-MONITORING-022 | L3 | Not Run | HTTP 404 for PATCH /api/v1/exceptions/1/resolve; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-MONITORING-023 | L3 | Pass | HTTP 200 for GET /api/v1/exceptions/violations; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-024 | L3 | Pass | HTTP 200 for GET /api/v1/fleet/capacity-check?date=2026-08-13; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-025 | L3 | Pass | HTTP 200 for GET /api/v1/health; HTTP 200 and basic response envelope matched |
| L3-IMPORT-026 | L3 | Not Run | HTTP 0 for GET /api/v1/imports; Multipart workbook fixture is not available in this runner |
| L3-IMPORT-027 | L3 | Not Run | HTTP 0 for POST /api/v1/imports; Multipart workbook fixture is not available in this runner |
| L3-IMPORT-028 | L3 | Not Run | HTTP 404 for GET /api/v1/imports/1; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-IMPORT-029 | L3 | Not Run | HTTP 404 for GET /api/v1/imports/1/errors; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-IMPORT-030 | L3 | Not Run | HTTP 404 for GET /api/v1/imports/1/errors/export; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-IMPORT-031 | L3 | Not Run | HTTP 404 for GET /api/v1/imports/1/orders; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-MONITORING-032 | L3 | Pass | HTTP 200 for GET /api/v1/kpi/by-driver; HTTP 200 and basic response envelope matched |
| L3-MONITORING-033 | L3 | Pass | HTTP 200 for GET /api/v1/kpi/by-route; HTTP 200 and basic response envelope matched |
| L3-MONITORING-034 | L3 | Pass | HTTP 200 for GET /api/v1/kpi/by-vehicle; HTTP 200 and basic response envelope matched |
| L3-MONITORING-035 | L3 | Pass | HTTP 200 for GET /api/v1/kpi/daily-trend; HTTP 200 and basic response envelope matched |
| L3-MONITORING-036 | L3 | Pass | HTTP 200 for GET /api/v1/kpi/summary; HTTP 200 and basic response envelope matched |
| L3-IDENTITY-037 | L3 | Pass | HTTP 200 for GET /api/v1/permissions; HTTP 200 and basic response envelope matched |
| L3-PLANNING-038 | L3 | Pass | HTTP 200 for GET /api/v1/planning-events; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-039 | L3 | Pass | HTTP 200 for GET /api/v1/products; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-040 | L3 | Not Run | HTTP 400 for POST /api/v1/products; Expected HTTP 201, received 400; prerequisite business fixture was not available. |
| L3-MASTERDATA-041 | L3 | Pass | HTTP 200 for GET /api/v1/products/1; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-042 | L3 | Pass | HTTP 200 for PUT /api/v1/products/1; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-043 | L3 | Pass | HTTP 200 for PATCH /api/v1/products/1/status; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-044 | L3 | Pass | HTTP 200 for GET /api/v1/products/by-sku/TOSTLR677WI; HTTP 200 and basic response envelope matched |
| L3-IDENTITY-045 | L3 | Pass | HTTP 200 for GET /api/v1/roles; HTTP 200 and basic response envelope matched |
| L3-IDENTITY-046 | L3 | Pass | HTTP 200 for GET /api/v1/roles/1; HTTP 200 and basic response envelope matched |
| L3-IDENTITY-047 | L3 | Pass | HTTP 200 for PUT /api/v1/roles/1/permissions; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-048 | L3 | Pass | HTTP 200 for GET /api/v1/routes; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-049 | L3 | Pass | HTTP 201 for POST /api/v1/routes; HTTP 201 and basic response envelope matched |
| L3-MASTERDATA-050 | L3 | Pass | HTTP 200 for GET /api/v1/routes/1; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-051 | L3 | Pass | HTTP 200 for PUT /api/v1/routes/1; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-052 | L3 | Pass | HTTP 200 for GET /api/v1/routes/1/directions; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-053 | L3 | Pass | HTTP 200 for PATCH /api/v1/routes/1/status; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-054 | L3 | Pass | HTTP 201 for POST /api/v1/routes/1/stops; HTTP 201 and basic response envelope matched |
| L3-MASTERDATA-055 | L3 | Pass | HTTP 200 for DELETE /api/v1/routes/1/stops/1; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-056 | L3 | Not Run | HTTP 400 for PUT /api/v1/routes/1/stops/reorder; Expected HTTP 200, received 400; prerequisite business fixture was not available. |
| L3-MASTERDATA-057 | L3 | Pass | HTTP 200 for GET /api/v1/stores; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-058 | L3 | Pass | HTTP 201 for POST /api/v1/stores; HTTP 201 and basic response envelope matched |
| L3-MASTERDATA-059 | L3 | Pass | HTTP 200 for GET /api/v1/stores/1; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-060 | L3 | Pass | HTTP 200 for PUT /api/v1/stores/1; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-061 | L3 | Pass | HTTP 200 for PATCH /api/v1/stores/1/status; HTTP 200 and basic response envelope matched |
| L3-PLANNING-062 | L3 | Pass | HTTP 200 for GET /api/v1/trip-drafts?deliveryDate=2026-08-13; HTTP 200 and basic response envelope matched |
| L3-PLANNING-063 | L3 | Not Run | HTTP 404 for GET /api/v1/trip-drafts/1; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-064 | L3 | Not Run | HTTP 400 for POST /api/v1/trip-drafts/1/adjust-departure-time; Expected HTTP 200, received 400; prerequisite business fixture was not available. |
| L3-PLANNING-065 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-drafts/1/assign; Expected HTTP 201, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-066 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-drafts/1/assign-split; Expected HTTP 201, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-067 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-drafts/1/confirm; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-068 | L3 | Not Run | HTTP 404 for GET /api/v1/trip-drafts/1/eligible-vehicles; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-069 | L3 | Not Run | HTTP 404 for GET /api/v1/trip-drafts/1/eligible-vehicles-for-stops?stopIds=1; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-070 | L3 | Not Run | HTTP 404 for GET /api/v1/trip-drafts/1/excluded-orders; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-071 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-drafts/1/generate-manifest; Expected HTTP 201, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-072 | L3 | Pass | HTTP 200 for GET /api/v1/trip-drafts/1/history; HTTP 200 and basic response envelope matched |
| L3-PLANNING-073 | L3 | Not Run | HTTP 404 for GET /api/v1/trip-drafts/1/manifest; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-074 | L3 | Not Run | HTTP 404 for GET /api/v1/trip-drafts/1/manifest/by-stop; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-075 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-drafts/1/optimal-departure; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-076 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-drafts/1/orders/1/exclude; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-077 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-drafts/1/orders/1/re-include; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-078 | L3 | Not Run | HTTP 400 for POST /api/v1/trip-drafts/1/orders/1/settle-delay; Expected HTTP 200, received 400; prerequisite business fixture was not available. |
| L3-PLANNING-079 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-drafts/1/recalculate-eta; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-080 | L3 | Not Run | HTTP 404 for GET /api/v1/trip-drafts/1/recommendations; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-081 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-drafts/1/revert; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-082 | L3 | Not Run | HTTP 404 for GET /api/v1/trip-drafts/1/stops; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-083 | L3 | Not Run | HTTP 404 for PATCH /api/v1/trip-drafts/1/stops/1; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-084 | L3 | Not Run | HTTP 404 for GET /api/v1/trip-drafts/1/stops/1/order-items; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-085 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-drafts/1/validate-capacity; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-086 | L3 | Not Run | HTTP 404 for GET /api/v1/trip-drafts/1/validation-result; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-PLANNING-087 | L3 | Pass | HTTP 200 for POST /api/v1/trip-drafts/consolidate; HTTP 200 and basic response envelope matched |
| L3-EXECUTION-088 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-executions/1/admin-override; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-MONITORING-089 | L3 | Pass | HTTP 200 for GET /api/v1/trip-outcome-events; HTTP 200 and basic response envelope matched |
| L3-MONITORING-090 | L3 | Pass | HTTP 200 for GET /api/v1/trip-outcomes; HTTP 200 and basic response envelope matched |
| L3-MONITORING-091 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-outcomes/1/amend?amendmentReason=QA%20contract%20verification; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-MONITORING-092 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-outcomes/1/validate; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-093 | L3 | Pass | HTTP 200 for GET /api/v1/trips?tripDraftId=1; HTTP 200 and basic response envelope matched |
| L3-EXECUTION-094 | L3 | Not Run | HTTP 404 for PATCH /api/v1/trips/1/assignment; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-095 | L3 | Not Run | HTTP 404 for POST /api/v1/trips/1/dispatch; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-096 | L3 | Not Run | HTTP 404 for GET /api/v1/trips/1/handover-slip; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-097 | L3 | Not Run | HTTP 404 for GET /api/v1/trips/1/progress; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-098 | L3 | Not Run | HTTP 404 for POST /api/v1/trips/1/start; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-099 | L3 | Not Run | HTTP 404 for GET /api/v1/trips/1; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-100 | L3 | Pass | HTTP 200 for GET /api/v1/trips/1/outcome-history; HTTP 200 and basic response envelope matched |
| L3-EXECUTION-101 | L3 | Pass | HTTP 200 for GET /api/v1/trips/my-trips?date=2026-08-13; HTTP 200 and basic response envelope matched |
| L3-EXECUTION-102 | L3 | Pass | HTTP 200 for GET /api/v1/trips/my-trips/calendar?month=2026-08; HTTP 200 and basic response envelope matched |
| L3-EXECUTION-103 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-stops/1/arrive; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-104 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-stops/1/complete; Expected HTTP 200, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-105 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-stops/1/reject; Expected HTTP 201, received 404; prerequisite business fixture was not available. |
| L3-IDENTITY-106 | L3 | Pass | HTTP 200 for GET /api/v1/users; HTTP 200 and basic response envelope matched |
| L3-IDENTITY-107 | L3 | Pass | HTTP 201 for POST /api/v1/users; HTTP 201 and basic response envelope matched |
| L3-IDENTITY-108 | L3 | Pass | HTTP 200 for GET /api/v1/users/1; HTTP 200 and basic response envelope matched |
| L3-IDENTITY-109 | L3 | Pass | HTTP 200 for PUT /api/v1/users/1; HTTP 200 and basic response envelope matched |
| L3-IDENTITY-110 | L3 | Pass | HTTP 200 for PATCH /api/v1/users/1/roles; HTTP 200 and basic response envelope matched |
| L3-IDENTITY-111 | L3 | Pass | HTTP 200 for PATCH /api/v1/users/1/status; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-112 | L3 | Pass | HTTP 200 for GET /api/v1/vehicles; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-113 | L3 | Pass | HTTP 201 for POST /api/v1/vehicles; HTTP 201 and basic response envelope matched |
| L3-MASTERDATA-114 | L3 | Pass | HTTP 200 for GET /api/v1/vehicles/1; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-115 | L3 | Pass | HTTP 200 for PUT /api/v1/vehicles/1; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-116 | L3 | Pass | HTTP 200 for PATCH /api/v1/vehicles/1/status; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-117 | L3 | Pass | HTTP 200 for GET /api/v1/vehicles/available; HTTP 200 and basic response envelope matched |
| L3-MASTERDATA-118 | L3 | Pass | HTTP 200 for GET /api/v1/vehicles/fleet-capacity; HTTP 200 and basic response envelope matched |
| L3-AUTH-119 | L3 | Pass | HTTP 401 for POST /api/v1/auth/login; HTTP 401 and basic response envelope matched |
| L3-AUTH-120 | L3 | Not Run | HTTP 401 for POST /api/v1/auth/login; Expected HTTP 403, received 401; no approved disabled-account credential fixture existed. |
| L3-AUTH-121 | L3 | Pass | HTTP 401 for POST /api/v1/auth/refresh; HTTP 401 and basic response envelope matched |
| L3-AUTH-122 | L3 | Pass | HTTP 401 for POST /api/v1/auth/refresh; HTTP 401 and basic response envelope matched |
| L3-MASTERDATA-123 | L3 | Not Run | HTTP 400 for POST /api/v1/vehicles; Expected HTTP 409, received 400; prerequisite business fixture was not available. |
| L3-MASTERDATA-124 | L3 | Pass | HTTP 409 for POST /api/v1/products; HTTP 409 and basic response envelope matched |
| L3-MASTERDATA-125 | L3 | Pass | HTTP 409 for POST /api/v1/stores; HTTP 409 and basic response envelope matched |
| L3-MASTERDATA-126 | L3 | Pass | HTTP 409 for POST /api/v1/routes; HTTP 409 and basic response envelope matched |
| L3-IMPORT-127 | L3 | Not Run | HTTP 0 for POST /api/v1/imports; Multipart workbook fixture is not available in this runner |
| L3-PLANNING-128 | L3 | Not Run | HTTP 404 for POST /api/v1/trip-drafts/1/confirm; Expected HTTP 409, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-129 | L3 | Not Run | HTTP 404 for POST /api/v1/trips/1/dispatch; Expected HTTP 409, received 404; prerequisite business fixture was not available. |
| L3-EXECUTION-130 | L3 | Not Run | HTTP 404 for POST /api/v1/trips/1/start; Expected HTTP 409, received 404; prerequisite business fixture was not available. |
| L4-WEB-AUTH-01 | L4 | Pass | Cypress FS-02-01: real login -> dashboard |
| L4-WEB-AUTH-02 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-AUTH-03 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-IMPORT-01 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-IMPORT-02 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-IMPORT-03 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-PLAN-01 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-PLAN-03 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-PLAN-04 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-PLAN-05 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-PLAN-06 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-PLAN-07 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-PLAN-08 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-PLAN-09 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-CAP-01 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-CAP-02 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-ASSIGN-01 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-ASSIGN-02 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-ASSIGN-03 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-MANIFEST-01 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-DISPATCH-01 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-DISPATCH-02 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-DISPATCH-03 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-MON-01 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-MON-02 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-MON-03 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-EXC-01 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-EXC-02 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-OUT-01 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-OUT-02 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-OUT-03 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-KPI-01 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-HIST-01 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-HIST-02 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-ADMIN-01 | L4 | Pass | Cypress FS-03-01: real authenticated users list |
| L4-WEB-ADMIN-02 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-ADMIN-03 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-ADMIN-04 | L4 | Pass | Cypress FS-04-01: real stores API and rendered rows |
| L4-WEB-ADMIN-05 | L4 | Pass | Cypress FS-06-01: real vehicles API and rendered rows |
| L4-WEB-ADMIN-06 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-WEB-ADMIN-07 | L4 | Pass | Cypress FS-05-01: real routes API and map view |
| L4-WEB-ADMIN-08 | L4 | Not Run | No dedicated real-backend Cypress journey matched all documented steps; stubbed UI specs are not accepted as L4 Pass. |
| L4-MOB-AUTH-01 | L4 | Not Run | Flutter smoke passed, but no emulator/device + GPS full-stack journey was executed. |
| L4-MOB-TRIP-01 | L4 | Not Run | Flutter smoke passed, but no emulator/device + GPS full-stack journey was executed. |
| L4-MOB-TRIP-02 | L4 | Not Run | Flutter smoke passed, but no emulator/device + GPS full-stack journey was executed. |
| L4-MOB-TRIP-03 | L4 | Not Run | Flutter smoke passed, but no emulator/device + GPS full-stack journey was executed. |
| L4-MOB-TRIP-04 | L4 | Not Run | Flutter smoke passed, but no emulator/device + GPS full-stack journey was executed. |
| L4-MOB-TRIP-05 | L4 | Not Run | Flutter smoke passed, but no emulator/device + GPS full-stack journey was executed. |
| L4-MOB-EXC-01 | L4 | Not Run | Flutter smoke passed, but no emulator/device + GPS full-stack journey was executed. |
| L4-MOB-PROFILE-01 | L4 | Not Run | Flutter smoke passed, but no emulator/device + GPS full-stack journey was executed. |
| ELOG-INTAKE-02 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-IMPORT-02, L4-WEB-IMPORT-03; business sign-off remains required. |
| ELOG-PLAN-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-PLAN-01, L4-WEB-PLAN-03; business sign-off remains required. |
| ELOG-PLAN-02 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-PLAN-04; business sign-off remains required. |
| ELOG-PLAN-03 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-PLAN-05, L4-WEB-PLAN-06; business sign-off remains required. |
| ELOG-CAPACITY-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-CAP-01; business sign-off remains required. |
| ELOG-CAPACITY-02 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-CAP-02, L4-WEB-ASSIGN-02; business sign-off remains required. |
| ELOG-RECOMMEND-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-PLAN-07; business sign-off remains required. |
| ELOG-RECOMMEND-02 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-CAP-02; business sign-off remains required. |
| ELOG-CONFIRM-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-PLAN-08; business sign-off remains required. |
| ELOG-CONFIRM-02 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-PLAN-09; business sign-off remains required. |
| ELOG-ASSIGN-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-ASSIGN-01, L4-WEB-ASSIGN-03; business sign-off remains required. |
| ELOG-MANIFEST-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-MANIFEST-01; business sign-off remains required. |
| ELOG-DISPATCH-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-DISPATCH-01, L4-WEB-DISPATCH-02, L4-MOB-TRIP-01; business sign-off remains required. |
| ELOG-MONITOR-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-MON-01, L4-WEB-MON-02; business sign-off remains required. |
| ELOG-EXCEPTION-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-MON-03, L4-WEB-EXC-01, L4-WEB-EXC-02; business sign-off remains required. |
| ELOG-OUTCOME-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-OUT-01, L4-WEB-OUT-02, L4-MOB-TRIP-04; business sign-off remains required. |
| ELOG-OUTCOME-02 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-OUT-03, L4-WEB-HIST-02; business sign-off remains required. |
| ELOG-KPI-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-KPI-01, L4-WEB-OUT-02; business sign-off remains required. |
| ELOG-AUDIT-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-HIST-01, L4-WEB-HIST-02; business sign-off remains required. |
| ELOG-MASTER-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-ADMIN-04, L4-WEB-ADMIN-05, L4-WEB-ADMIN-06, L4-WEB-ADMIN-07; business sign-off remains required. |
| ELOG-ACCESS-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-ADMIN-01, L4-WEB-ADMIN-02, L4-WEB-ADMIN-03; business sign-off remains required. |
| ELOG-FLEET-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-ADMIN-08, L4-WEB-ASSIGN-01; business sign-off remains required. |
| ELOG-DRIVER-02 | UAT | Not Run | Technical prerequisite incomplete: L4-MOB-TRIP-02, L4-MOB-TRIP-03; business sign-off remains required. |
| ELOG-DRIVER-03 | UAT | Not Run | Technical prerequisite incomplete: L4-MOB-TRIP-04, L4-MOB-TRIP-05, L4-WEB-OUT-01; business sign-off remains required. |
| ELOG-SESSION-01 | UAT | Not Run | Technical prerequisite incomplete: L4-WEB-AUTH-02, L4-WEB-AUTH-03, L4-MOB-PROFILE-01; business sign-off remains required. |
