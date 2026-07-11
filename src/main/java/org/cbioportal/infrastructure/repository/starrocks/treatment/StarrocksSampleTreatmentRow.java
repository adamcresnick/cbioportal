package org.cbioportal.infrastructure.repository.starrocks.treatment;

public record StarrocksSampleTreatmentRow(
    String treatment,
    String temporalRelation,
    String sampleId,
    String patientId,
    Integer timeTaken,
    String studyId) {}
