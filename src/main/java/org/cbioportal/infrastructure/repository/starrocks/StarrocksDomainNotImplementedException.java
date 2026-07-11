package org.cbioportal.infrastructure.repository.starrocks;

import java.lang.reflect.Method;

/** Raised when a StarRocks domain adapter has not yet been ported. */
public class StarrocksDomainNotImplementedException extends UnsupportedOperationException {

  public StarrocksDomainNotImplementedException(Class<?> repositoryType, Method method) {
    super(
        "StarRocks repository operation is not implemented: "
            + repositoryType.getName()
            + "#"
            + method.getName());
  }
}
