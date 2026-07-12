package org.cbioportal.legacy.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class IndexTemplateContractTest {

  @Test
  void initializesAnalyticsQueueBeforeLoadingPinnedFrontend() throws Exception {
    String template =
        new ClassPathResource("templates/index.html").getContentAsString(StandardCharsets.UTF_8);

    assertThat(template)
        .contains("window.dataLayer = window.dataLayer || []")
        .contains("window.gtag = window.gtag || function()")
        .containsSubsequence("window.gtag = window.gtag || function()", "loadAppStyles");
  }
}
