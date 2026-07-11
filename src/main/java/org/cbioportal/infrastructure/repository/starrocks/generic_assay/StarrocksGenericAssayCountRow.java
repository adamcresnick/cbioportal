package org.cbioportal.infrastructure.repository.starrocks.generic_assay;

public class StarrocksGenericAssayCountRow {

  private String stableId;
  private String value;
  private Integer count;

  public String getStableId() {
    return stableId;
  }

  public void setStableId(String stableId) {
    this.stableId = stableId;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }
}
