package org.cbioportal.legacy.persistence.mybatis.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SqlUtilsTest {

  @Test
  void pairsEntityAndProfileIdsWithoutEncodingThemIntoOneString() {
    List<SqlUtils.EntityProfilePair> pairs =
        SqlUtils.pairEntityAndProfileIds(
            List.of("sample:one", "sample"), List.of("profile", "one:profile"));

    assertThat(pairs)
        .extracting(
            SqlUtils.EntityProfilePair::getEntityId, SqlUtils.EntityProfilePair::getProfileId)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("sample:one", "profile"),
            org.assertj.core.groups.Tuple.tuple("sample", "one:profile"));
  }

  @Test
  void rejectsUnpairedEntityAndProfileIds() {
    assertThatThrownBy(
            () -> SqlUtils.pairEntityAndProfileIds(List.of("sample"), List.of("p1", "p2")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("same size");
  }
}
