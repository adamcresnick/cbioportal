package org.cbioportal.infrastructure.repository.starrocks;

import java.util.List;

public class StarrocksUnsupportedStudyViewFilterException extends UnsupportedOperationException {

  public StarrocksUnsupportedStudyViewFilterException(List<String> filterFamilies) {
    super(
        "StarRocks Study View filter families are not implemented yet: "
            + String.join(", ", filterFamilies));
  }
}
