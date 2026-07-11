package org.cbioportal.infrastructure.repository.starrocks.coexpression;

public class StarrocksCoExpressionAggregateRow {

  private Integer entrezGeneId;
  private String spearmansCorrelation;
  private Integer numSamples;
  private Integer distinctGeneValues;

  public Integer getEntrezGeneId() {
    return entrezGeneId;
  }

  public void setEntrezGeneId(Integer entrezGeneId) {
    this.entrezGeneId = entrezGeneId;
  }

  public String getSpearmansCorrelation() {
    return spearmansCorrelation;
  }

  public void setSpearmansCorrelation(String spearmansCorrelation) {
    this.spearmansCorrelation = spearmansCorrelation;
  }

  public Integer getNumSamples() {
    return numSamples;
  }

  public void setNumSamples(Integer numSamples) {
    this.numSamples = numSamples;
  }

  public Integer getDistinctGeneValues() {
    return distinctGeneValues;
  }

  public void setDistinctGeneValues(Integer distinctGeneValues) {
    this.distinctGeneValues = distinctGeneValues;
  }
}
