package org.cbioportal.infrastructure.repository.starrocks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.cbioportal.legacy.persistence.helper.AlterationFilterHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StarrocksMapperDialectLintTest {

  @TempDir Path tempDir;

  @Test
  void currentStarrocksMappersPassLint() throws Exception {
    LintResult result = runLint(Path.of("src/main/resources/mappers/starrocks"));

    assertThat(result.exitCode).isEqualTo(0);
    assertThat(result.output).isBlank();
  }

  @Test
  void lintFailsOnClickhouseOnlyConstruct() throws Exception {
    Path mapper = tempDir.resolve("BadMapper.xml");
    Files.writeString(
        mapper,
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <mapper namespace="BadMapper">
            <select id="bad" resultType="string">
                SELECT multiIf(value = 1, 'yes', 'no')
            </select>
        </mapper>
        """);

    LintResult result = runLint(tempDir);

    assertThat(result.exitCode).isNotEqualTo(0);
    assertThat(result.output).contains("multiIf");
  }

  @Test
  void nestedSharedFragmentsUseFullyQualifiedReferences() throws Exception {
    Path mapper =
        Path.of(
            "src/main/resources/mappers/starrocks/studyview/StarrocksStudyViewFilterMapper.xml");
    String xml = Files.readString(mapper);

    assertThat(xml)
        .doesNotContain("<include refid=\"isAttributeValueNA\"")
        .contains(
            "<include refid=\"org.cbioportal.infrastructure.repository.starrocks.studyview.StarrocksStudyViewFilterMapper.isAttributeValueNA\"");
  }

  @Test
  void sharedClinicalEventMapperUsesMysqlProtocolSafeBindings() throws Exception {
    Path mapper =
        Path.of(
            "src/main/resources/org/cbioportal/legacy/persistence/mybatis/ClinicalEventMapper.xml");
    String xml = Files.readString(mapper);

    assertThat(xml)
        .doesNotContain("ArrayTypeHandler")
        .doesNotContain("${sortBy}")
        .doesNotContain("${direction}")
        .doesNotContain("combineStudyAndPatientIds");
  }

  @Test
  void sharedMutationMapperUsesSafePortableBindings() throws Exception {
    Path mapper =
        Path.of("src/main/resources/org/cbioportal/legacy/persistence/mybatis/MutationMapper.xml");
    String xml = Files.readString(mapper);

    assertThat(xml)
        .doesNotContain("ArrayTypeHandler")
        .doesNotContain("${sortBy}")
        .doesNotContain("${direction}")
        .doesNotContain("'${geneFilterQuery")
        .doesNotContain("CONCAT(sampleId");
  }

  @Test
  void sharedStructuralVariantMapperUsesSafePortableBindings() throws Exception {
    String xml =
        Files.readString(
            Path.of(
                "src/main/resources/org/cbioportal/legacy/persistence/mybatis/StructuralVariantMapper.xml"));

    assertThat(xml)
        .doesNotContain("ArrayTypeHandler")
        .doesNotContain("${geneFilterQuery")
        .doesNotContain("listToArray");
  }

  @Test
  void alterationMapperOnlyReferencesExistingFilterHelperMethods() throws Exception {
    String xml =
        Files.readString(
            Path.of(
                "src/main/resources/mappers/starrocks/alteration/StarrocksAlterationMapper.xml"));
    Pattern pattern = Pattern.compile("alterationFilterHelper[.]([A-Za-z0-9_]+)[(]");

    for (var result : pattern.matcher(xml).results().toList()) {
      assertThat(AlterationFilterHelper.class.getMethod(result.group(1)))
          .as(result.group(1))
          .isNotNull();
    }
    assertThat(xml)
        .doesNotContain("sample_derived sample")
        .doesNotContain("sample_to_gene_panel_derived panel");
  }

  @Test
  void genomicMappersAvoidReservedAliases() throws Exception {
    String xml =
        Files.readString(
            Path.of(
                "src/main/resources/mappers/starrocks/genomic_data/StarrocksGenomicDataMapper.xml"));

    assertThat(xml)
        .doesNotContain("sample_derived sample")
        .doesNotContain("sample_to_gene_panel_derived panel");
  }

  private LintResult runLint(Path mapperDir) throws IOException, InterruptedException {
    Process process =
        new ProcessBuilder("bash", "scripts/lint_starrocks_mappers.sh", mapperDir.toString())
            .directory(Path.of(System.getProperty("user.dir")).toFile())
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();
    return new LintResult(exitCode, output);
  }

  private static class LintResult {
    private final int exitCode;
    private final String output;

    private LintResult(int exitCode, String output) {
      this.exitCode = exitCode;
      this.output = output;
    }
  }
}
