package org.cbioportal.legacy.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MolecularValueFormatterTest {

  @Test
  void formatsNumericValuesAndPreservesCategoricalAndEmptyTokens() {
    GeneMolecularAlteration alteration = new GeneMolecularAlteration();

    alteration.setValues(
        "1.234567,-1.23455,0,-0,0.0000123456,-0.0000123456,4e1,NA,true,,NaN,Infinity");

    assertThat(alteration.getSplitValues())
        .containsExactly(
            "1.2346",
            "-1.2346",
            "0",
            "0",
            "1.235e-5",
            "-1.235e-5",
            "40",
            "NA",
            "true",
            "",
            "NaN",
            "Infinity");
  }

  @Test
  void resettingValuesInvalidatesTheSplitCache() {
    GeneMolecularAlteration alteration = new GeneMolecularAlteration();
    alteration.setValues("1.11111");
    assertThat(alteration.getSplitValues()).containsExactly("1.1111");

    alteration.setValues("2.22222,");

    assertThat(alteration.getSplitValues()).containsExactly("2.2222", "");
  }
}
