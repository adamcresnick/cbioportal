package org.cbioportal.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class PaginationCalculatorTest {

  @Test
  void offsetUsesPublicApiZeroBasedPageNumbers() {
    assertThat(PaginationCalculator.offset(10, 0)).isZero();
    assertThat(PaginationCalculator.offset(10, 1)).isEqualTo(10);
    assertThat(PaginationCalculator.offset(10, 10)).isEqualTo(100);
    assertThat(PaginationCalculator.offset(null, 1)).isNull();
    assertThat(PaginationCalculator.offset(10, null)).isZero();
    assertThatIllegalArgumentException().isThrownBy(() -> PaginationCalculator.offset(10, -1));
  }

  @Test
  void lastIndexIsExclusiveAndClampedToListLength() {
    assertThat(PaginationCalculator.lastIndex(0, 3, 26)).isEqualTo(3);
    assertThat(PaginationCalculator.lastIndex(3, 3, 26)).isEqualTo(6);
    assertThat(PaginationCalculator.lastIndex(25, 3, 26)).isEqualTo(26);
    assertThat(PaginationCalculator.lastIndex(null, 3, 26)).isNull();
  }
}
