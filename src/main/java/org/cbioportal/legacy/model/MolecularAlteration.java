package org.cbioportal.legacy.model;

import java.io.Serializable;

public abstract class MolecularAlteration implements Serializable {

  private String values;
  private String[] splitValues = null;

  /**
   * Set the values for all samples.
   *
   * @param values: string with list of values, comma (,) separated
   */
  public void setValues(String values) {
    this.values = MolecularValueFormatter.formatValues(values);
    this.splitValues = null;
  }

  /**
   * Returns the values attribute split on (,).
   *
   * <p>Remembers last .split to avoid repeating this costly operation.
   *
   * @return list of values for all samples
   */
  public String[] getSplitValues() {
    if (splitValues == null) {
      // A negative split limit preserves empty values after trailing commas.
      splitValues = values.split(",", -1);
    }
    return splitValues;
  }

  public abstract String getStableId();
}
