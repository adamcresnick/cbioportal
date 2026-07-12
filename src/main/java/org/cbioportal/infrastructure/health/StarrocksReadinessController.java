package org.cbioportal.infrastructure.health;

import org.cbioportal.infrastructure.health.StarrocksReadinessService.StarrocksReadiness;
import org.cbioportal.legacy.web.config.annotation.PublicApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@PublicApi
@RestController
@ConditionalOnProperty(name = "columnstore.backend", havingValue = "starrocks")
public class StarrocksReadinessController {

  private final StarrocksReadinessService readinessService;

  public StarrocksReadinessController(StarrocksReadinessService readinessService) {
    this.readinessService = readinessService;
  }

  @GetMapping("/api/health/starrocks")
  @PreAuthorize("permitAll()")
  public ResponseEntity<StarrocksReadiness> readiness() {
    StarrocksReadiness readiness = readinessService.check();
    return ResponseEntity.status(readiness.ready() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
        .body(readiness);
  }
}
