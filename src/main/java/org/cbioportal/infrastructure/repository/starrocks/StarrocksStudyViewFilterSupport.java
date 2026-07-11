package org.cbioportal.infrastructure.repository.starrocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.legacy.web.parameter.CategorizedGenericAssayDataCountFilter;
import org.cbioportal.legacy.web.parameter.filter.AndedPatientTreatmentFilters;
import org.cbioportal.legacy.web.parameter.filter.AndedSampleTreatmentFilters;

public final class StarrocksStudyViewFilterSupport {

  private StarrocksStudyViewFilterSupport() {}

  public static void requireBaseFilterFamilies(StudyViewFilterContext context) {
    List<String> unsupported = new ArrayList<>();
    addIfPresent(unsupported, "clinicalDataFilters", context.clinicalDataFilters());
    addIfPresent(unsupported, "geneFilters", context.geneFilters());
    addIfPresent(unsupported, "structuralVariantFilters", context.structuralVariantFilters());
    addIfPresent(unsupported, "genomicDataFilters", context.genomicDataFilters());
    addIfPresent(unsupported, "genericAssayDataFilters", context.genericAssayDataFilters());
    addIfPresent(
        unsupported, "genericAssaySelectionFilters", context.genericAssaySelectionFilters());
    addIfPresent(unsupported, "customDataFilters", context.customDataFilters());
    addIfPresent(unsupported, "clinicalEventFilters", context.clinicalEventFilters());
    addIfPresent(unsupported, "mutationDataFilters", context.mutationDataFilters());
    addIfPresent(unsupported, "customSampleIdentifiers", context.customSampleIdentifiers());
    addIfPresent(unsupported, "sampleTreatmentFilters", context.sampleTreatmentFilters());
    addIfPresent(unsupported, "sampleTreatmentGroupFilters", context.sampleTreatmentGroupFilters());
    addIfPresent(
        unsupported, "sampleTreatmentTargetFilters", context.sampleTreatmentTargetFilters());
    addIfPresent(unsupported, "patientTreatmentFilters", context.patientTreatmentFilters());
    addIfPresent(
        unsupported, "patientTreatmentGroupFilters", context.patientTreatmentGroupFilters());
    addIfPresent(
        unsupported, "patientTreatmentTargetFilters", context.patientTreatmentTargetFilters());
    if (context.alterationFilter() != null) {
      unsupported.add("alterationFilter");
    }
    addIfPresent(
        unsupported,
        "categorizedGenericAssayDataCountFilter",
        context.categorizedGenericAssayDataCountFilter());
    if (!unsupported.isEmpty()) {
      throw new StarrocksUnsupportedStudyViewFilterException(unsupported);
    }
  }

  private static void addIfPresent(List<String> unsupported, String name, Collection<?> values) {
    if (values != null && !values.isEmpty()) {
      unsupported.add(name);
    }
  }

  private static void addIfPresent(
      List<String> unsupported, String name, AndedSampleTreatmentFilters filters) {
    if (filters != null && filters.getFilters() != null && !filters.getFilters().isEmpty()) {
      unsupported.add(name);
    }
  }

  private static void addIfPresent(
      List<String> unsupported, String name, AndedPatientTreatmentFilters filters) {
    if (filters != null && filters.getFilters() != null && !filters.getFilters().isEmpty()) {
      unsupported.add(name);
    }
  }

  private static void addIfPresent(
      List<String> unsupported,
      String name,
      CategorizedGenericAssayDataCountFilter categorizedFilters) {
    if (categorizedFilters == null) {
      return;
    }
    if (hasValues(categorizedFilters.getSampleNumericalGenericAssayDataFilters())
        || hasValues(categorizedFilters.getSampleCategoricalGenericAssayDataFilters())
        || hasValues(categorizedFilters.getPatientNumericalGenericAssayDataFilters())
        || hasValues(categorizedFilters.getPatientCategoricalGenericAssayDataFilters())) {
      unsupported.add(name);
    }
  }

  private static boolean hasValues(Collection<?> values) {
    return values != null && !values.isEmpty();
  }
}
