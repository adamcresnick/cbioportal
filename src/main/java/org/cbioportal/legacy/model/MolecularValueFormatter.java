package org.cbioportal.legacy.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/** Applies the API's molecular-value precision rules without relying on a database dialect. */
final class MolecularValueFormatter {

  private static final double SCIENTIFIC_NOTATION_THRESHOLD = 0.0001;
  private static final Pattern COMMA = Pattern.compile(",", Pattern.LITERAL);

  private MolecularValueFormatter() {}

  static String formatValues(String values) {
    if (values == null) {
      return null;
    }
    String[] tokens = COMMA.split(values, -1);
    for (int index = 0; index < tokens.length; index++) {
      tokens[index] = formatToken(tokens[index]);
    }
    return String.join(",", tokens);
  }

  private static String formatToken(String token) {
    final double numericValue;
    try {
      numericValue = Double.parseDouble(token);
    } catch (NumberFormatException exception) {
      return token;
    }
    if (!Double.isFinite(numericValue)) {
      return token;
    }
    if (numericValue == 0) {
      return "0";
    }
    if (Math.abs(numericValue) < SCIENTIFIC_NOTATION_THRESHOLD) {
      int exponent = (int) Math.floor(Math.log10(Math.abs(numericValue)));
      double base = numericValue / Math.pow(10, exponent);
      return rounded(base, 3) + "e" + exponent;
    }
    return rounded(numericValue, 4);
  }

  private static String rounded(double value, int scale) {
    return BigDecimal.valueOf(value)
        .setScale(scale, RoundingMode.HALF_EVEN)
        .stripTrailingZeros()
        .toPlainString();
  }
}
